package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import org.jetbrains.annotations.NonNls

class RenderDocLineBreakpointType : XLineBreakpointType<RenderDocBreakpointProperties>(ID, RenderDocBundle.message("renderdoc.breakpoint.type.title")) {
  companion object {
    const val ID: @NonNls String = "RenderDocLineBreakpoint"
  }

  override fun createBreakpointProperties(file: VirtualFile, line: Int): RenderDocBreakpointProperties = RenderDocBreakpointProperties(file)

  override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
    return Registry.get("renderdoc.enabled").asBoolean() && (file.getUserData(ShaderDebugUserDataKeys.SHADER_SOURCE_FILE_INDEX) != null || RenderDocBreakpointProperties.supportedFileExtensions.contains(file.extension))
  }
}
