package com.jetbrains.rider.plugins.renderdoc.toolWindow.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.ImageUtil
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocPixelInput
import com.jetbrains.rider.plugins.renderdoc.toolWindow.RenderDocTextureSelectionPanel
import com.jetbrains.rider.plugins.renderdoc.toolWindow.actions.SelectPixelAdapter
import org.intellij.images.editor.impl.ImageEditorManagerImpl
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.Point
import java.awt.Transparency
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.round

internal class TexturePreviewUtil {

  companion object {
    private const val HIGHLIGHTED_PIXEL_WIDTH_ON_PREVIEWER = 5

    private fun createImageEditor(textureSelectionPanel: RenderDocTextureSelectionPanel): JComponent {
      val textureImage = textureSelectionPanel.getTextureImage()

      val w = textureImage.width
      val h = textureImage.height

      val bufferedImage = copyBufferedImage(textureImage)
      val icon = ImageIcon(getImageWithSelectedPixel(bufferedImage, textureSelectionPanel.getCurrentPixelInput(), Dimension(w, h)))

      val panel = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice.defaultConfiguration.createCompatibleImage(w, h, Transparency.TRANSLUCENT)
      val g = panel.createGraphics()
      icon.paintIcon(null, g, 0, 0)
      g.dispose()

      val comp = ImageEditorManagerImpl.createImageEditorUI(panel)
      val imageComponent = ImageEditorManagerImpl.getImageComponent(comp)

      val mouseAdapter = SelectPixelAdapter(textureSelectionPanel) {
        imageComponent.document.value = getImageWithSelectedPixel(copyBufferedImage(textureImage), textureSelectionPanel.getCurrentPixelInput(),
                                                                  Dimension(w, h))
      }
      imageComponent.addMouseListener(mouseAdapter)
      imageComponent.addMouseMotionListener(mouseAdapter)
      return comp
    }

    fun showImageViewerPopup(project: Project, textureSelectionPanel: RenderDocTextureSelectionPanel) {
      val comp = createImageEditor(textureSelectionPanel)
      val popup = DebuggerUIUtil.createValuePopup(project, comp) { Disposer.dispose(comp as Disposable) }
      val frame = WindowManager.getInstance().getFrame(project) ?: return
      val frameSize = frame.size
      val size = Dimension(frameSize.width / 2, frameSize.height / 2)
      popup.setSize(size)
      popup.show(RelativePoint(frame, Point(size.width / 2, size.height / 2)))
    }

    fun getImageWithSelectedPixel(image: Image, input: RenderDocPixelInput, originDimension: Dimension): BufferedImage {
      val bufferedImage = ImageUtil.toBufferedImage(image, true)
      val scaleX = bufferedImage.width.toDouble() / originDimension.width
      val scaleY = bufferedImage.height.toDouble() / originDimension.height
      val x = (input.x.toInt() * scaleX).toInt()
      val y = (input.y.toInt() * scaleY).toInt()
      return createBorderAroundPixel(bufferedImage, x, y, round(max(scaleX, scaleY)).toInt())
    }

    fun convertRGBBufferToImage(buffer: ByteArray, width: Int, height: Int): BufferedImage? {
      if (buffer.size != width * height * 3) {
        return null
      }

      val image = ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_RGB)
      var index = 0

      for (y in 0 until height) {
        for (x in 0 until width) {
          val r = buffer[index].toInt() and 0xFF
          val g = buffer[index + 1].toInt() and 0xFF
          val b = buffer[index + 2].toInt() and 0xFF
          val rgb = (r shl 16) or (g shl 8) or b
          image.setRGB(x, y, rgb)
          index += 3
        }
      }

      return image.getSubimage(0, 0, width, height)
    }

    fun getZoomedSubimage(originalImage: BufferedImage, x: Int, y: Int, preferredRadius: Int, zoomFactor: Int) : BufferedImage {
      val radius = preferredRadius + zoomFactor - preferredRadius % zoomFactor
      val originRadius = radius / zoomFactor

      val topLeftX = x - originRadius
      val topLeftY = y - originRadius

      val newWidth = 2 * radius + zoomFactor
      val newHeight = 2 * radius + zoomFactor

      val zoomedImage = ImageUtil.createImage(newWidth, newHeight, originalImage.type)

      val borderColor = getColorToSeparatePixel(originalImage, x, y)

      fun isCentralPixelBorder(i: Int, j: Int) : Boolean {
        return i % zoomFactor == 0 || j % zoomFactor == 0 || (i + 1) % zoomFactor == 0 || (j + 1) % zoomFactor == 0
      }

      for (i in 0 until newWidth) {
        for (j in 0 until newHeight) {
          val originalI = i / zoomFactor + topLeftX
          val originalJ = j / zoomFactor + topLeftY
          val rgb = if (originalI == x && originalJ == y && isCentralPixelBorder(i, j)) {
            borderColor
          } else if (originalI in 0..<originalImage.width && originalJ in 0..<originalImage.height) {
            originalImage.getRGB(originalI, originalJ)
          } else {
            JBColor.DARK_GRAY.rgb
          }
          zoomedImage.setRGB(i, j, rgb)
        }
      }

      return zoomedImage.getSubimage(0, 0, newWidth, newHeight)
    }

    private fun createBorderAroundPixel(image: BufferedImage, left: Int, top: Int, scale: Int) : BufferedImage {
      val centerX = left + scale / 2
      val centerY = top + scale / 2
      if (left in 0..<image.width && top in 0..<image.height) {
        val borderWidth = HIGHLIGHTED_PIXEL_WIDTH_ON_PREVIEWER / 2
        val half = max(scale / 2, borderWidth)
        val color = getColorToSeparatePixel(image, centerX, centerY, borderWidth)
        val innerRange = -half + borderWidth..half - borderWidth
        for (dx in (-half..half)) {
          for (dy in (-half..half)) {
            if (dx in innerRange && dy in innerRange) continue
            val i = centerX + dx
            val j = centerY + dy
            if (i in 0..<image.width && j in 0..<image.height) {
              image.setRGB(i, j, color)
            }
          }
        }
      }
      return image
    }

    private fun getColorToSeparatePixel(originalImage: BufferedImage, x: Int, y: Int, borderWidth: Int = 1) : Int {
      var sumR = 0
      var sumG = 0
      var sumB = 0
      var count = 0
      for (dx in listOf(-borderWidth, 0, borderWidth)) {
        for (dy in listOf(-borderWidth, 0, borderWidth)) {
          val originalX = x + dx
          val originalY = y + dy
          if (originalX in 0..<originalImage.width && originalY in 0..<originalImage.height) {
            val rgb = originalImage.getRGB(originalX, originalY)
            sumR += (rgb shr 16) and 0xFF
            sumG += (rgb shr 8) and 0xFF
            sumB += rgb and 0xFF
            count++
          }
        }
      }
      if (count == 0) ++count

      var red = 255 - sumR / count
      var green = 255 - sumG / count
      var blue = 255 - sumB / count

      // Calculate contrast for better visibility
      val brightness = red * 0.299 + green * 0.587 + blue * 0.114
      if (brightness > 127) {
        red = minOf(255, red + 50)
        green = minOf(255, green + 50)
        blue = minOf(255, blue + 50)
      } else {
        red = maxOf(0, red - 50)
        green = maxOf(0, green - 50)
        blue = maxOf(0, blue - 50)
      }

      val alpha = if (originalImage.type == BufferedImage.TYPE_INT_ARGB) 0xFF else 0x00
      return alpha shl 24 or (red shl 16) or (green shl 8) or blue
    }

    private fun copyBufferedImage(image: BufferedImage): BufferedImage {
      val copiedImage = ImageUtil.createImage(image.width, image.height, image.type)

      val g2d = copiedImage.createGraphics()
      g2d.drawImage(image, 0, 0, null)
      g2d.dispose()
      return copiedImage
    }
  }
}