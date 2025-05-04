package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugSessionInfo
import com.jetbrains.rider.plugins.renderdoc.execution.ShaderDebugProcessHandler
import com.jetbrains.rider.plugins.renderdoc.vfs.RenderDocCaptureFileSystem
import com.jetbrains.rd.framework.protocolOrThrow
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.fire
import com.jetbrains.rd.util.threading.coroutines.launch
import com.jetbrains.renderdoc.rdClient.model.RdcDebugSession
import com.jetbrains.renderdoc.rdClient.model.RdcSessionState
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import java.util.*

class RenderDocDebugProcess(session: XDebugSession, private val project: Project,
                            private val processHandler: ShaderDebugProcessHandler,
                            private val rdcSessionInfo: RenderDocDebugSessionInfo) : XDebugProcess(session) {
  internal val rdcSession: RdcDebugSession = rdcSessionInfo.session
  internal val uniqueId : UUID = UUID.randomUUID()

  private val sourceMapper: RenderDocSourcesMapper = RenderDocSourcesMapper(project)
  private val scheduler: IScheduler = rdcSession.protocolOrThrow.scheduler
  private val lifetimeDefinition = LifetimeDefinition()

  private val fileSystem = VirtualFileManager.getInstance().getFileSystem(RenderDocCaptureFileSystem.PROTOCOL) as RenderDocCaptureFileSystem
  private val fileSystemRootDomain = UUID.randomUUID().toString()

  private var drawCallInfo: RenderDocDrawCallDebugInfo? = null
  private var captureTreeFile : RenderDocCaptureFileSystem.MyFile? = null

  private val breakpointHandlers = arrayOf<XBreakpointHandler<*>>(RenderDocLineBreakpointHandler(scheduler, this, project.basePath ?: ""))

  fun isDrawCallSourceFile(file: VirtualFile) : Boolean {
    if (file !is RenderDocCaptureFileSystem.MyFile) return false
    return drawCallInfo?.sourceFiles?.containsValue(file) ?: false
  }

  override fun sessionInitialized() {
    scheduler.invokeOrQueue {
      rdcSession.sessionState.advise(lifetimeDefinition) { debugState ->
        if (debugState != null) {
          val info = syncAndGetDrawCallInfo(debugState)
          val stack = debugState.currentStack
          if (stack.stepIndex != -1) {
            lifetimeDefinition.launch {
              info.sourceFiles[stack.sourceFileIndex]?.let {
                val position = sourceMapper.mapDecompiledToSourcePosition(it, stack)
                session.positionReached(RenderDocSuspendContext(info, position))
              }
            }
          }
          else {
            session.positionReached(RenderDocSuspendContext(sourceMapper, getOrCreateCaptureTreeFile(), stack.drawCallId))
          }
        }
        else {
          processHandler.destroyProcess()
          return@advise
        }
      }
    }
  }

  override fun getEditorsProvider(): XDebuggerEditorsProvider {
    return RenderDocDebuggerEditorsProvider()
  }

  override fun startStepInto(context: XSuspendContext?) {
    scheduler.queue {
      rdcSession.stepInto.fire()
    }
  }

  override fun startStepOver(context: XSuspendContext?) {
    scheduler.queue {
      rdcSession.stepOver.fire()
    }
  }

  override fun stop() {
    lifetimeDefinition.terminate()
    drawCallInfo?.stop()
    captureTreeFile?.let { fileSystem.deleteFile(this, it) }
  }

  override fun resume(context: XSuspendContext?) {
    scheduler.queue {
      rdcSession.resume.fire()
    }
  }

  override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> = breakpointHandlers

  override fun doGetProcessHandler(): ProcessHandler = processHandler
  internal fun getCurrentEventId() : UInt? = drawCallInfo?.eventId
  internal fun getCurrentDebugInput() : RenderDocDebugInput = rdcSessionInfo.input

  private fun syncAndGetDrawCallInfo(debugState: RdcSessionState) : RenderDocDrawCallDebugInfo {
    val eventId = debugState.currentStack.drawCallId
    var info = drawCallInfo ?: RenderDocDrawCallDebugInfo(fileSystem, sourceMapper, debugState.drawCallSession, fileSystemRootDomain, rdcSessionInfo, eventId)
    drawCallInfo?.let { drawCall ->
      if (drawCall.eventId != eventId ) {
        drawCall.stop()
        info = RenderDocDrawCallDebugInfo(fileSystem, sourceMapper, debugState.drawCallSession, fileSystemRootDomain, rdcSessionInfo, eventId)
      }
    }

    debugState.stageInfo?.let { info.variableResolverService?.loadStageVariablesInfo(it) }
    RenderDocSessionSwitchListener.updateToolWindowPreview(uniqueId, project, eventId, rdcSessionInfo.input)
    drawCallInfo = info
    return info
  }

  private fun getOrCreateCaptureTreeFile() : VirtualFile {
    val file = captureTreeFile ?: fileSystem.createDrawCallsTreeFile(fileSystemRootDomain, sourceMapper, rdcSessionInfo.capture)
    if (captureTreeFile == null)
      captureTreeFile = file
    return file
  }
}