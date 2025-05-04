package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.intellij.ui.components.JBLabel
import com.intellij.util.containers.enumMapOf
import com.intellij.util.ui.StatusText
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.toolWindow.actions.SelectPixelAdapter
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocPixelInput
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.EventTexturesInfo
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.TextureInfo
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.TextureType
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.*
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.getTextureNumber
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.getTexturePlainIndex
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Image
import java.awt.image.BufferedImage
import java.util.EnumMap
import javax.swing.*
import kotlin.math.max

internal class RenderDocTextureSelectionPanel : RenderDocAbstractTextureSelectionPanel() {
  private var textures = enumMapOf<TextureType, List<TextureInfo>>() as EnumMap
  private var textureSelection : TextureSelection = TextureSelection(TextureType.COLOR, -1)

  private val texturePanel = BufferedImagePanel()
  private val textureNameLabel = JBLabel()
  private val bottomPanel = JPanel(BorderLayout())
  private val pixelInfo = RenderDocPixelInfo()
  private val prevButton = JButton("<")
  private val counterLabel = JLabel("0/0")
  private val nextButton = JButton(">")

  init {
    initTexturePreview()
    initBottomPanel()
  }

  private fun initTexturePreview() {
    val previewContainer = JPanel(BorderLayout())

    texturePanel.apply {
      alignmentX = CENTER_ALIGNMENT
      minimumSize = Dimension(0, 0)
      val mouseAdapter = SelectPixelAdapter(this@RenderDocTextureSelectionPanel)
      addMouseListener(mouseAdapter)
      addMouseMotionListener(mouseAdapter)
    }
    previewContainer.add(texturePanel, BorderLayout.CENTER)

    val textureInfoPanel = JPanel(BorderLayout()).apply {
      border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
      add(textureNameLabel, BorderLayout.CENTER)
    }
    previewContainer.add(textureInfoPanel, BorderLayout.SOUTH)

    add(previewContainer, BorderLayout.CENTER)
  }

  private fun initBottomPanel() {
    pixelInfo.addPixelInfoChangeListener { x, y ->
      val (type, index) = textureSelection
      if (textures.tryGetTexture(type, index) != null) {
        updateWithInput(RenderDocPixelInput(type, x, y))
      }
    }
    val pixelWrapper = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply { add(pixelInfo) }
    bottomPanel.add(pixelWrapper, BorderLayout.CENTER)

    val carouselPanel = JPanel(BorderLayout())

    prevButton.isEnabled = false
    counterLabel.horizontalAlignment = SwingConstants.CENTER
    nextButton.isEnabled = false

    carouselPanel.add(prevButton, BorderLayout.WEST)
    carouselPanel.add(counterLabel, BorderLayout.CENTER)
    carouselPanel.add(nextButton, BorderLayout.EAST)

    prevButton.addActionListener {
      textureSelection = textureSelection.moveOrSelf(-1)
      updateWithInput(getCurrentPixelInput())
    }

    nextButton.addActionListener {
      textureSelection = textureSelection.moveOrSelf(1)
      updateWithInput(getCurrentPixelInput())
    }

    bottomPanel.add(carouselPanel, BorderLayout.EAST)

    add(bottomPanel, BorderLayout.SOUTH)
  }

  private fun getCurrentTexture() : TextureInfo? = textureSelection.let { textures.tryGetTexture(it.type, it.index) }

  fun getTextureImage() : BufferedImage = getCurrentTexture()?.image ?: error("No texture selected")

  override fun getImageComponent(): BufferedImagePanel = texturePanel

  override fun getCurrentPixelInput(): RenderDocPixelInput =
    pixelInfo.getInfo().let {
      val type = textureSelection.type
      RenderDocPixelInput(type, it.first, it.second)
    }

  /**
   * Updates the texture preview based on the provided input.
   *
   * This method tries to set the image index based on the texture type in the input,
   * update the pixel info, refresh the texture display, and update button states.
   */
  override fun updateTextures(texturesInfo: EventTexturesInfo, pixelInput: RenderDocPixelInput, inputMode: Boolean) : Boolean {
    textures = enumMapOf<TextureType, List<TextureInfo>>().apply {
      if (inputMode) {
        put(TextureType.INPUT, texturesInfo.inputs)
      } else {
        put(TextureType.COLOR, texturesInfo.colorTextures)
        put(TextureType.DEPTH, texturesInfo.depthTexture?.let { listOf(it) } ?: emptyList())
      }
    } as EnumMap
    val selection = textureSelection
    if (selection.type != pixelInput.textureType || textures.tryGetTexture(pixelInput.textureType, selection.index) == null) {
      val texture = textures.getFirstOfTypeOrDefault(pixelInput.textureType)
      textureSelection = texture?.let { TextureSelection(it.type, 0) } ?: getInvalidSelection(inputMode)
    }
    return updateWithInput(pixelInput)
  }

  override fun updatePixelInfo(x: Int, y: Int) {
    val texture = getCurrentTexture() ?: return
    pixelInfo.updateRGBInfo(texture.image, RenderDocPixelInput(texture.type, x.toUInt(), y.toUInt()))
    texturePanel.isVisible = true
    texturePanel.repaint()
  }

  override fun setToDefaultState(inputMode: Boolean) {
    textureSelection = getInvalidSelection(inputMode)
    textures = enumMapOf<TextureType, List<TextureInfo>>() as EnumMap
    texturePanel.isVisible = true
    texturePanel.repaint()
    pixelInfo.isVisible = false
    textureNameLabel.text = ""
    updateButtonsAvailability()
  }

  private fun updateWithInput(input: RenderDocPixelInput): Boolean {
    val texture = getCurrentTexture()
    if (texture == null) {
      setToDefaultState(input.textureType == TextureType.INPUT)
      return false
    }
    textureNameLabel.text = RenderDocBundle.message("renderdoc.tool.previewer.texture.info", texture.name, texture.image.width, texture.image.height)
    textureNameLabel.toolTipText = textureNameLabel.text
    pixelInfo.updateRGBInfo(texture.image, input)
    texturePanel.isVisible = true
    texturePanel.repaint()
    updateButtonsAvailability()
    return true
  }

  private fun updateButtonsAvailability() {
    val index = textures.getTexturePlainIndex(textureSelection.type, textureSelection.index)
    val textureImagesNumber = textures.getTextureNumber()
    prevButton.isEnabled = textureImagesNumber > 0 && index > 0
    nextButton.isEnabled = textureImagesNumber > 0 && index != -1 && index < textureImagesNumber - 1
    counterLabel.text = "${index + 1}/$textureImagesNumber"
  }

  private data class TextureSelection(val type: TextureType, val index: Int)
  private fun TextureSelection.isValid() : Boolean = index == -1
  private fun TextureSelection.moveOrSelf(direction: Int) : TextureSelection {
    val texturesMap = textures

    if (index + direction in 0..(texturesMap[type]?.lastIndex ?: -1)) {
      return TextureSelection(type, index + direction)
    }

    val types = textures.keys.toList()
    var idx = types.indexOf(type) + direction
    while (idx in 0..types.lastIndex) {
      val textureList = texturesMap[types[idx]]
      if (!textureList.isNullOrEmpty())
        return TextureSelection(types[idx], if (direction > 0) 0 else textureList.lastIndex)
      idx += direction
    }
    return TextureSelection(type, index)
  }

  private fun getInvalidSelection(inputMode: Boolean) : TextureSelection {
    val type = if (inputMode) TextureType.INPUT else TextureType.COLOR
    return TextureSelection(type, -1)
  }

  inner class BufferedImagePanel : JPanel() {
    private val emptyText = object : StatusText(this) {
      init {
        appendText(RenderDocBundle.message("renderdoc.tool.previewer.default"))
      }
      override fun isStatusVisible(): Boolean { return isEmpty(); }
    }
    var dimensions: Dimension = Dimension(0, 0)

    fun isEmpty(): Boolean = textureSelection.isValid()

    override fun paint(g: Graphics?) {
      super.paint(g)

      val texture = getCurrentTexture()
      if (texture == null) {
        emptyText.paint(this, g)
        return
      }

      val image = texture.image
      val k = max(image.width.toDouble() / this.width, image.height.toDouble() / height)
      val newWidth = (image.width / k).toInt()
      val newHeight = (image.height / k).toInt()
      val scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_FAST)
      val imageWithSelection = TexturePreviewUtil.getImageWithSelectedPixel(scaledImage, getCurrentPixelInput(), Dimension(image.width, image.height))
      val left = (this.width - imageWithSelection.width) / 2
      val top = (this.height - imageWithSelection.height) / 2
      g?.drawImage(imageWithSelection, left, top, this).also {
        dimensions.width = imageWithSelection.width
        dimensions.height = imageWithSelection.height
      }
    }
  }
}