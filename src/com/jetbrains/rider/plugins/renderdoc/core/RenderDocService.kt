package com.jetbrains.rider.plugins.renderdoc.core

import com.intellij.ide.plugins.getPluginDistDirByClass
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.system.CpuArch
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.rider.plugins.renderdoc.common.rd.LifetimedCache
import com.jetbrains.rider.plugins.renderdoc.debugger.RenderDocBreakpointProperties
import com.jetbrains.rider.plugins.renderdoc.debugger.RenderDocLineBreakpointType
import com.jetbrains.rd.framework.createBackgroundScheduler
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.coroutines.asCoroutineDispatcher
import com.jetbrains.rd.util.threading.coroutines.async
import com.jetbrains.renderdoc.rdClient.RenderDocClient
import com.jetbrains.renderdoc.rdClient.RenderDocHostException
import com.jetbrains.renderdoc.rdClient.model.*
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.RenderDocNotificationUtil
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.isUnityPluginEnabled
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.isUnityProject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.random.Random

@Service(Service.Level.PROJECT)
class RenderDocService(val project: Project) : LifetimedService() {
  companion object {
    fun getInstance(project: Project): RenderDocService = project.service<RenderDocService>()
  }

  private val openedCaptureFiles = LifetimedCache<VirtualFile, RdcCaptureFile>()
  private val openedCaptures = LifetimedCache<VirtualFile, RdcCapture>()
  private val client: Deferred<RenderDocClient?>
  private val scheduler = createBackgroundScheduler(serviceLifetime, "RenderDocPanel")
  private val dispatcher = scheduler.asCoroutineDispatcher

  init {
    if (Registry.get("renderdoc.enabled").asBoolean() && isUnityPluginEnabled() && project.isUnityProject()) {
      val hostBinDir = resolveHostBinDirectory()
      client = if (hostBinDir != null) {
        serviceLifetime.async {
          try {
            RenderDocClient.createWithHost(serviceLifetime, scheduler, Random.nextLong(), hostBinDir.pathString)
          } catch (e : RenderDocHostException) {
            val exitCode = e.exitCode
            RenderDocNotificationUtil.notifyProcessStartFailure(project, exitCode)
            throw e
          }
        }
      }
      else {
        thisLogger().warn("RenderDocHost isn't available for current runtime")
        CompletableDeferred(null as RenderDocClient?)
      }
    } else {
      client = CompletableDeferred(null as RenderDocClient?)
    }
  }

  suspend fun clientOrThrow(): RenderDocClient = client.await() ?: throw IllegalStateException("RenderDoc client not available")

  fun resolveHostBinDirectory(): Path? {
    Registry.stringValue("renderdoc.host.bin").takeIf { it.isNotEmpty() }?.let { return Path(it) }
    val arch = when (CpuArch.CURRENT) {
      CpuArch.ARM64 -> "aarch64"
      CpuArch.X86_64 -> "x86_64"
      else -> return null
    }
    val os = when {
      SystemInfoRt.isWindows -> "windows"
      SystemInfoRt.isLinux -> "linux"
      SystemInfoRt.isMac -> "macos"
      else -> return null
    }
    val libsDirectory = getPluginDistDirByClass(this::class.java)?.resolve("runtime") ?: error("Unable to determine plugin lib path.")
    val runtimeIdentifier = "$os-$arch"

    fun resolveRuntimeDir(runtimeIdentifier: String) =
      libsDirectory.resolve(runtimeIdentifier).takeIf { it.isDirectory() }

    return resolveRuntimeDir(runtimeIdentifier)
  }

  suspend fun isAvailable(): Boolean = client.await() != null

  suspend fun openCaptureFile(lifetime: Lifetime, file: VirtualFile): RdcCaptureFile =
    withContext(dispatcher) {
      openedCaptureFiles.getOrCalculate(lifetime, file) { captureFileLifetime ->
        clientOrThrow().model.openCaptureFile.startSuspending(captureFileLifetime, file.path)
      }
    }

  suspend fun openCapture(lifetime: Lifetime, file: VirtualFile): RdcCapture =
    withContext(dispatcher) {
      openedCaptures.getOrCalculate(lifetime, file) { captureLifetime ->
        openCaptureFile(captureLifetime, file).openCapture.startSuspending(captureLifetime, Unit)
      }
    }


  suspend fun debugVertex(lifetime: Lifetime, file: VirtualFile, eventId: UInt, input: RenderDocVertexInput): RenderDocDebugSessionInfo =
    withContext(dispatcher) {
      val cap = openCapture(lifetime, file)
      RenderDocDebugSessionInfo(cap, cap.debugVertex.startSuspending(lifetime, RdcDebugVertexInput(eventId, input.index, emptyList())), input)
    }

  suspend fun debugPixel(lifetime: Lifetime, file: VirtualFile, eventId: UInt, input: RenderDocPixelInput): RenderDocDebugSessionInfo =
    withContext(dispatcher) {
      val cap = openCapture(lifetime, file)
      val session = cap.debugPixel.startSuspending(lifetime, RdcDebugPixelInput(eventId, input.x, input.y, emptyList()))
      RenderDocDebugSessionInfo(cap, session, input)
    }

  private fun getFilteredBreakpoint(): List<RdcSourceBreakpoint> {
    return XDebuggerManager.getInstance(project).breakpointManager.getBreakpoints(RenderDocLineBreakpointType::class.java).mapNotNull {
      val pos = it.sourcePosition ?: return@mapNotNull null
      val sourceFile = pos.file
      if (!RenderDocBreakpointProperties.supportedFileExtensions.contains(sourceFile.extension)) return@mapNotNull null
      var filePath = sourceFile.path.removePrefix(project.basePath.orEmpty())
      if (filePath != sourceFile.path) filePath = filePath.drop(1)
      RdcSourceBreakpoint(filePath, pos.line.toUInt() + 1u)
    }
  }

  suspend fun tryDebugVertex(lifetime: Lifetime, file: VirtualFile, input: RenderDocVertexInput): RenderDocDebugSessionInfo = withContext(dispatcher) {
    val cap = openCapture(lifetime, file)
    val breakpoints = getFilteredBreakpoint()
    val session = cap.tryDebugVertex.startSuspending(lifetime, RdcDebugVertexInput(0u, input.index, breakpoints))
    RenderDocDebugSessionInfo(cap, session, input)
  }

  suspend fun tryDebugPixel(lifetime: Lifetime, file: VirtualFile, input: RenderDocPixelInput): RenderDocDebugSessionInfo = withContext(dispatcher) {
    val cap = openCapture(lifetime, file)
    val breakpoints = getFilteredBreakpoint()
    val session = cap.tryDebugPixel.startSuspending(lifetime, RdcDebugPixelInput(0u, input.x, input.y, breakpoints))
    return@withContext RenderDocDebugSessionInfo(cap, session, input)
  }

  suspend fun getTexturePreview(lifetime: Lifetime, capture: RdcCapture, eventId: Long): RdcPixelStageInOutputs? =
    withContext(dispatcher) {
      capture.getPixelStageInOutputs.startSuspending(lifetime, eventId)
    }

  suspend fun getVertexStageInOutputs(lifetime: Lifetime, capture: RdcCapture, eventId: Long) : RdcVertexStageInOutputs? =
    withContext(dispatcher) {
      capture.getVertexStageInOutputs.startSuspending(lifetime, eventId)
    }
}