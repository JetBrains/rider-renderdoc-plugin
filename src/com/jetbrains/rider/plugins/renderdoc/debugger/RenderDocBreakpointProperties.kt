package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XBreakpointProperties

class RenderDocBreakpointProperties(val file: VirtualFile) : XBreakpointProperties<Unit>() {

  companion object {
    val supportedFileExtensions = listOf("shader", "cg", "cginc", "hlsl", "hlslinc", "compute", "urtshader", "glsl", "glslinc")
  }

  override fun getState() = Unit

  override fun loadState(state: Unit) = Unit
}
