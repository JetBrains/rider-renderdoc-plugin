package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.intellij.openapi.rd.defineNestedLifetime
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocService
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.threading.coroutines.launch
import com.jetbrains.renderdoc.rdClient.model.RdcCapture
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocPixelInput
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.TextureInfo
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.TextureType
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.EventTexturesInfo
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.TexturePreviewUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

class RenderDocTexturePreviewPanel(private val service: RenderDocService) : RenderDocAbstractPreviewer() {
  private val lifetime = defineNestedLifetime()
  private val texturePreviewLifetime = SequentialLifetimes(lifetime)

  private val titlePanel = JPanel(BorderLayout())
  private val expandButton = JButton(RenderDocBundle.message("renderdoc.tool.previewer.texture.expand"))

  private val inputsRadioButton = JRadioButton(RenderDocBundle.message("renderdoc.tool.previewer.type.inputs"))
  private val outputsRadioButton = JRadioButton(RenderDocBundle.message("renderdoc.tool.previewer.type.outputs"))
  private val typeButtonsGroup = ButtonGroup()
  private val inputMode: Boolean
    get() = inputsRadioButton.isSelected

  private var eventTexturesInfo : EventTexturesInfo = EventTexturesInfo(null, emptyList(), emptyList(), null)
  private val textureSelectionPanel : RenderDocAbstractTextureSelectionPanel = RenderDocTextureSelectionPanel()

  init {
    val radioButtonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    typeButtonsGroup.apply {
      add(inputsRadioButton)
      add(outputsRadioButton)
    }
    outputsRadioButton.isSelected = true

    radioButtonPanel.add(inputsRadioButton)
    radioButtonPanel.add(outputsRadioButton)

    inputsRadioButton.addActionListener {
      if (inputsRadioButton.isSelected)
        updateTexturesOnSelection()
    }

    outputsRadioButton.addActionListener {
      if (outputsRadioButton.isSelected)
        updateTexturesOnSelection()
    }

    titlePanel.add(radioButtonPanel, BorderLayout.WEST)
    titlePanel.add(expandButton, BorderLayout.EAST)
    expandButton.addActionListener(ExpandCollapseAction())
    add(titlePanel, BorderLayout.NORTH)

    add(textureSelectionPanel,  BorderLayout.CENTER)
  }

  override fun getDefaultInput() : RenderDocDebugInput = RenderDocPixelInput.DEFAULT

  internal fun hideTextureSelection() = textureSelectionPanel.getImageComponent()

  override fun getCurrentInput(): RenderDocPixelInput = textureSelectionPanel.getCurrentPixelInput()

  override fun syncPreviewWithEvent(capture: RdcCapture, eventId: Long, input: RenderDocDebugInput) {
    val lifetime = texturePreviewLifetime.next()
    lifetime.launch {
      // If the event ID hasn't changed, just update the texture preview
      if (eventId == eventTexturesInfo.eventId) {
        val pixelInput = input as RenderDocPixelInput
        checkPreviewMode(pixelInput.textureType)
        updateTexturePreview(pixelInput)
        return@launch
      }
      val inOutputs = service.getTexturePreview(lifetime, capture, eventId)
      if (inOutputs == null) {
        resetToDefaultState(eventId)
        return@launch
      }

      val inputs = inOutputs.inputs.mapNotNull { output ->
        TexturePreviewUtil.convertRGBBufferToImage(output.buffer, output.width, output.height)?.let { TextureInfo(output.name, TextureType.INPUT, it) }
      }

      val colorOutputs = inOutputs.colorOutputs.mapNotNull { output ->
        TexturePreviewUtil.convertRGBBufferToImage(output.buffer, output.width, output.height)?.let { TextureInfo(output.name, TextureType.COLOR, it) }
      }

      val depthOutput = inOutputs.depthOutput?.let { output ->
        TexturePreviewUtil.convertRGBBufferToImage(output.buffer, output.width, output.height)?.let { TextureInfo(output.name, TextureType.DEPTH, it) }
      }

      eventTexturesInfo = EventTexturesInfo(eventId, inputs, colorOutputs, depthOutput)

      val pixelInput = input as RenderDocPixelInput
      checkPreviewMode(pixelInput.textureType)
      updateTexturePreview(pixelInput)
    }
  }

  override fun defaultPreview() {
    val lt = texturePreviewLifetime.next()
    lt.launch {
      resetToDefaultState(null)
    }
  }

  private fun updateTexturePreview(input: RenderDocPixelInput) {
    val isNotEmpty = textureSelectionPanel.updateTextures(eventTexturesInfo, input, inputMode)
    expandButton.isEnabled = isNotEmpty
  }

  private fun updateTexturesOnSelection() {
    if (eventTexturesInfo.eventId == null) return

    val currentPixelInput = getCurrentInput().let {
      RenderDocPixelInput(if (inputMode) TextureType.INPUT else TextureType.COLOR, it.x, it.y)
    }
    textureSelectionPanel.updateTextures(eventTexturesInfo, currentPixelInput, inputMode)
  }

  private fun resetToDefaultState(eventId : Long?) {
    eventTexturesInfo = EventTexturesInfo(eventId, emptyList(), emptyList(), null)
    textureSelectionPanel.setToDefaultState(inputMode)
    expandButton.isEnabled = false
  }

  private fun checkPreviewMode(type: TextureType) {
    when (type) {
      TextureType.INPUT -> inputsRadioButton.isSelected = true
      else -> outputsRadioButton.isSelected = true
    }
  }

  override fun dispose(): Unit = Unit

  inner class ExpandCollapseAction : ActionListener {
    override fun actionPerformed(e: ActionEvent?) {
      when (val selectionPanel = this@RenderDocTexturePreviewPanel.textureSelectionPanel) {
        is RenderDocTextureSelectionPanel -> TexturePreviewUtil.showImageViewerPopup(this@RenderDocTexturePreviewPanel.service.project, selectionPanel)
      }
    }
  }
}