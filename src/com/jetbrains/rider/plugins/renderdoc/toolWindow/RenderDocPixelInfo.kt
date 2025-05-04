package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocPixelInput
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.image.BufferedImage
import javax.swing.*

class RenderDocPixelInfo: JPanel(GridBagLayout()) {
  private val pixelX = JTextField("0")
  private val pixelY = JTextField("0")
  private val pixelColorLabel = JBLabel()

  init {
    isVisible = false

    val gridBag = GridBagConstraints()

    gridBag.insets = JBUI.insetsRight(5)
    gridBag.gridx = 0
    gridBag.gridy = 0
    gridBag.anchor = GridBagConstraints.WEST
    add(JLabel(RenderDocBundle.message("renderdoc.tool.previewer.texture.pixelInfo.title")), gridBag)

    val validator = object : InputVerifier() {
      override fun verify(input: JComponent): Boolean {
        val field = input as JTextField
        val inp = field.text.toUIntOrNull()
        return inp != null
      }
    }

    pixelX.inputVerifier = validator
    pixelX.verifyInputWhenFocusTarget = true
    pixelY.inputVerifier = validator
    pixelY.verifyInputWhenFocusTarget = true
    pixelColorLabel.iconTextGap = 5

    val components = listOf(pixelX, pixelY, JBLabel(":"), pixelColorLabel)
    components.forEachIndexed { i, comp ->
      gridBag.gridx = i + 1
      add(comp, gridBag)
    }
  }

  fun updateRGBInfo(image: BufferedImage, input: RenderDocPixelInput?) {
    val (x, y) = if (input != null) Pair(input.x.toInt(), input.y.toInt()) else Pair(0, 0)
    if (x >= image.width || y >= image.height) {
      pixelX.text = x.toString()
      pixelY.text = y.toString()
      pixelColorLabel.icon = null
      pixelColorLabel.text = RenderDocBundle.message("renderdoc.tool.previewer.texture.pixelInfo.notExist")
      isVisible = true
      return
    }
    updateInfo(image, x, y)
    isVisible = true
  }

  fun updateInfo(image: BufferedImage, x: Int, y: Int) {
    val rgb = image.getRGB(x, y)
    pixelX.text = x.toString()
    pixelY.text = y.toString()
    pixelColorLabel.text = "${(rgb shr 16) and 0xFF}, ${(rgb shr 8) and 0xFF}, ${rgb and 0xFF}"
    val pixelSize = pixelColorLabel.preferredSize.height
    pixelColorLabel.icon = ColorIcon(pixelSize, pixelSize, pixelSize - 1, pixelSize - 1, JBColor(rgb, rgb), JBColor.foreground(), 0)
    isVisible = true
  }

  fun addPixelInfoChangeListener(handler: (UInt, UInt)->Unit) {
    fun update() {
      val (x, y) = getInfo()
      handler(x, y)
    }
    pixelX.addActionListener { update() }
    pixelY.addActionListener { update() }
  }

  fun getInfo() : Pair<UInt, UInt> {
    val x = pixelX.text.toUIntOrNull() ?: 0u
    val y = pixelY.text.toUIntOrNull() ?: 0u
    return Pair(x, y)
  }
}