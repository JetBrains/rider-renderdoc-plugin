package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.intellij.openapi.actionSystem.DataKey
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocPixelInput
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocVertexInput
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.CaptureInfo

object RenderDocDataKeys {
  val RENDERDOC_CAPTURE_INFO: DataKey<CaptureInfo> = DataKey.create("RENDERDOC_CAPTURE")
  val RENDERDOC_TOOL: DataKey<RenderDocTool> = DataKey.create("RENDERDOC_TOOL")
  val DRAW_CALL_ID: DataKey<UInt> = DataKey.create("SHADER_DRAW_CALL_ID")
  val RENDERDOC_PIXEL_DEBUG_INPUT: DataKey<RenderDocPixelInput> = DataKey.create("RENDERDOC_PIXEL_DEBUG_INPUT")
  val RENDERDOC_VERTEX_DEBUG_INPUT: DataKey<RenderDocVertexInput> = DataKey.create("RENDERDOC_VERTEX_DEBUG_INPUT")
}
