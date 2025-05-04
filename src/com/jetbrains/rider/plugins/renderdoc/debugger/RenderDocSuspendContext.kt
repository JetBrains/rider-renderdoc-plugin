package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XSuspendContext

class RenderDocSuspendContext : XSuspendContext {
  private val stack: RenderDocExecutionStack

  constructor(drawCallInfo: RenderDocDrawCallDebugInfo, position: RenderDocSourcePosition) : super() {
    stack = RenderDocExecutionStack(drawCallInfo, position)
  }

  constructor(sourceMapper: RenderDocSourcesMapper, captureTreeFile: VirtualFile, eventId: UInt) : super() {
    stack = RenderDocExecutionStack(sourceMapper, captureTreeFile, eventId)
  }

  override fun getActiveExecutionStack(): XExecutionStack {
    return stack
  }
}