package com.jetbrains.rider.plugins.renderdoc.debugger.frame

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XStackFrame
import com.jetbrains.rider.plugins.renderdoc.debugger.RenderDocSourcePosition
import com.jetbrains.rider.plugins.renderdoc.debugger.RenderDocSourcesMapper

class RenderDocDrawCallTreeFrame(private val sourceMapper: RenderDocSourcesMapper, private val captureTreeFile: VirtualFile, private val eventId: UInt) : XStackFrame() {

  override fun getSourcePosition(): XSourcePosition {
    return RenderDocSourcePosition(captureTreeFile, sourceMapper.getCaptureTreeFilePosition(eventId))
  }
}