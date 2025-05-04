package com.jetbrains.rider.plugins.renderdoc.toolWindow.model

import java.awt.image.BufferedImage

enum class TextureType { INPUT, COLOR, DEPTH }

internal data class TextureInfo(val name: String, val type: TextureType, val image: BufferedImage)

internal data class EventTexturesInfo(
  val eventId: Long?,
  val inputs: List<TextureInfo>,
  val colorTextures: List<TextureInfo>,
  val depthTexture: TextureInfo?)