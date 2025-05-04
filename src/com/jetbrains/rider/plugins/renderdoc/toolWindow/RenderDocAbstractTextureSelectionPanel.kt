package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.jetbrains.rider.plugins.renderdoc.core.RenderDocPixelInput
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.EventTexturesInfo
import java.awt.BorderLayout
import javax.swing.*

internal abstract class RenderDocAbstractTextureSelectionPanel : JPanel(BorderLayout()) {
  abstract fun getImageComponent() : RenderDocTextureSelectionPanel.BufferedImagePanel
  abstract fun getCurrentPixelInput() : RenderDocPixelInput

  abstract fun updateTextures(texturesInfo: EventTexturesInfo, pixelInput: RenderDocPixelInput, inputMode: Boolean) : Boolean
  abstract fun updatePixelInfo(x: Int, y: Int)
  abstract fun setToDefaultState(inputMode: Boolean)
}