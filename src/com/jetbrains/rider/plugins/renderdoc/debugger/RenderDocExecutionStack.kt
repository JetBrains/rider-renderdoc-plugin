package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.debugger.frame.RenderDocDrawCallTreeFrame
import com.jetbrains.rider.plugins.renderdoc.debugger.frame.RenderDocStackFrame

class RenderDocExecutionStack : XExecutionStack {
  private val frame: XStackFrame

  constructor(drawCallInfo: RenderDocDrawCallDebugInfo, position: RenderDocSourcePosition) : super(RenderDocBundle.message("renderdoc.debugger.executionStack.title")) {
    this.frame = RenderDocStackFrame(drawCallInfo, position)
  }

  constructor(sourceMapper: RenderDocSourcesMapper, captureTreeFile: VirtualFile, eventId: UInt) : super(RenderDocBundle.message("renderdoc.debugger.executionStack.title")) {
    this.frame = RenderDocDrawCallTreeFrame(sourceMapper, captureTreeFile, eventId)
  }
  override fun getTopFrame(): XStackFrame = frame

  override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer?) {
  }
}