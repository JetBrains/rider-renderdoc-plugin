package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISignal
import com.jetbrains.renderdoc.rdClient.model.RdcLineBreakpoint
import com.jetbrains.renderdoc.rdClient.model.RdcSourceBreakpoint

class RenderDocLineBreakpointHandler(private val scheduler: IScheduler, private val debugProcess: RenderDocDebugProcess, private val projectPath: String)
  : XBreakpointHandler<XLineBreakpoint<RenderDocBreakpointProperties>>(RenderDocLineBreakpointType::class.java) {
  private val debugSession = debugProcess.rdcSession

  override fun registerBreakpoint(breakpoint: XLineBreakpoint<RenderDocBreakpointProperties>) {
    processBreakpoint(breakpoint, debugSession.addLineBreakpoint, debugSession.addSourceBreakpoint)
  }

  override fun unregisterBreakpoint(breakpoint: XLineBreakpoint<RenderDocBreakpointProperties>, temporary: Boolean) {
    processBreakpoint(breakpoint, debugSession.removeLineBreakpoint, debugSession.removeSourceBreakpoint)
  }

  private fun processBreakpoint(breakpoint: XLineBreakpoint<RenderDocBreakpointProperties>, lineSignal: ISignal<RdcLineBreakpoint>, sourceSignal: ISignal<RdcSourceBreakpoint>) {
    scheduler.queue {
      breakpoint.sourcePosition?.let {
        val line = breakpoint.line.toUInt() + 1u
        val file = it.file
        if (debugProcess.isDrawCallSourceFile(file)) {
          val index = file.getUserData(ShaderDebugUserDataKeys.SHADER_SOURCE_FILE_INDEX) ?: -1
          lineSignal.fire(RdcLineBreakpoint(index, line))
        }
        else sourceSignal.fire(RdcSourceBreakpoint(transformFilePath(file.path), line))
      }
    }
  }

  private fun transformFilePath(path: String): String {
    var filePath = path.removePrefix(projectPath)
    if (filePath != path) filePath = filePath.drop(1)
    return filePath
  }
}