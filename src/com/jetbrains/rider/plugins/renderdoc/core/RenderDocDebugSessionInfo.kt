package com.jetbrains.rider.plugins.renderdoc.core

import com.jetbrains.renderdoc.rdClient.model.RdcCapture
import com.jetbrains.renderdoc.rdClient.model.RdcDebugSession
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.TextureType

interface RenderDocDebugInput
data class RenderDocPixelInput(val textureType: TextureType, val x: UInt, val y: UInt) : RenderDocDebugInput {
  companion object {
    val DEFAULT: RenderDocPixelInput = RenderDocPixelInput(TextureType.COLOR, 0u, 0u)
  }
}
data class RenderDocVertexInput(val index: UInt) : RenderDocDebugInput {
  companion object {
    val DEFAULT: RenderDocVertexInput = RenderDocVertexInput(0u)
  }
}

data class RenderDocDebugSessionInfo(val capture: RdcCapture, val session: RdcDebugSession, val input: RenderDocDebugInput)