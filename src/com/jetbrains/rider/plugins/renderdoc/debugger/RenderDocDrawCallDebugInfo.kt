package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugSessionInfo
import com.jetbrains.rider.plugins.renderdoc.debugger.frame.RenderDocVariableResolver
import com.jetbrains.rider.plugins.renderdoc.vfs.RenderDocCaptureFileSystem
import com.jetbrains.renderdoc.rdClient.model.RdcDebugTrace
import com.jetbrains.renderdoc.rdClient.model.RdcDrawCallDebugSession
import java.util.*

class RenderDocDrawCallDebugInfo(
  private val fileSystem: RenderDocCaptureFileSystem,
  private val sourceMapper: RenderDocSourcesMapper,
  rdcDrawCallSession: RdcDrawCallDebugSession?,
  private val fileSystemRootDomain: String,
  rdcSessionInfo: RenderDocDebugSessionInfo,
  internal var eventId: UInt
) {

  private val api = rdcSessionInfo.capture.api
  internal val debugTrace : RdcDebugTrace? = rdcDrawCallSession?.debugTrace
  internal val sourceFiles: HashMap<Int, VirtualFile> = hashMapOf()
  internal val variableResolverService = rdcDrawCallSession?.let { RenderDocVariableResolver(it, api) }

  init {
    if (rdcDrawCallSession != null) {
      rdcDrawCallSession.disassembly?.let { sourceFiles[-1] = fileSystem.addSourceFile(fileSystemRootDomain, sourceMapper, it, -1) }
      rdcDrawCallSession.sourceFiles.forEachIndexed { index, sourceFile ->
        sourceFiles[index] = fileSystem.addSourceFile(fileSystemRootDomain, sourceMapper, sourceFile, index)
      }
    }
  }

  fun stop() {
    sourceFiles.values.forEach { f -> fileSystem.deleteFile(this, f) }
  }
}