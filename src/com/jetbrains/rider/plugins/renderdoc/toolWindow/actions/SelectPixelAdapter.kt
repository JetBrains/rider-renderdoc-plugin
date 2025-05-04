package com.jetbrains.rider.plugins.renderdoc.toolWindow.actions

import com.intellij.ui.JBColor
import com.jetbrains.rider.plugins.renderdoc.toolWindow.RenderDocTextureSelectionPanel
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.TexturePreviewUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.math.max
import kotlin.math.min

internal class SelectPixelAdapter(private val texturePanel: RenderDocTextureSelectionPanel, private val selectionPreviewerHandler: (MouseEvent)->Unit = {}) : MouseAdapter()  {
  private var popup: JWindow? = null
  private var imageLabel: JLabel = JLabel()
  private val PIXEL_WIDTH = 30

  override fun mousePressed(e : MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e) && !texturePanel.getImageComponent().isEmpty()) {
      showZoomedPopup(e.point, e.source as Component)
    } else {
      super.mousePressed(e)
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e) && popup != null) {
      showZoomedPopup(e.point, e.source as Component)
    } else {
      super.mouseDragged(e)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e) && popup != null) {
      if (e.source !== texturePanel.getImageComponent())
        selectionPreviewerHandler(e)
      popup?.dispose()
      popup = null
    } else {
      super.mouseReleased(e)
    }
  }

  private fun showZoomedPopup(point: Point, imageComponent: Component) {
    val imageSize: Dimension
    val locationOnScreen: Point = imageComponent.locationOnScreen.apply {
      x += point.x
      y += point.y
    }

    if (imageComponent == texturePanel.getImageComponent()) {
      val comp = imageComponent as RenderDocTextureSelectionPanel.BufferedImagePanel
      imageSize = comp.dimensions
      point.x -= (comp.width - imageSize.width) / 2
      point.y -= (comp.height - imageSize.height) / 2
    }
    else {
      imageSize = imageComponent.size
    }
    point.x = min(max(0, point.x), imageSize.width)
    point.y = min(max(0, point.y), imageSize.height)

    val radius = min((texturePanel.height + texturePanel.width) / 2, 150)
    val zoomedImage = updateZoomedImage(point, imageSize, radius)

    val parent = SwingUtilities.getWindowAncestor(imageComponent)
    val popup = popup ?: JWindow(parent).apply {
      type = Window.Type.POPUP
      isAlwaysOnTop = true
      contentPane.add(imageLabel)
      size = Dimension(zoomedImage.width, zoomedImage.height)
      shape = Ellipse2D.Float(0f, 0f, zoomedImage.width.toFloat(), zoomedImage.height.toFloat())
    }

    imageLabel.icon = ImageIcon(zoomedImage)
    adjustPopupLocation(locationOnScreen, popup)
    popup.isVisible = true
    this.popup = popup
  }

  private fun adjustPopupLocation(pointOnScreen: Point, popup: JWindow) {
    val screen = Toolkit.getDefaultToolkit().screenSize

    val r = popup.width / 2
    val hasXOverflow = pointOnScreen.x >= screen.width - 2 * r
    val hasYOverflow = pointOnScreen.y >= screen.height - 2 * r
    if (hasXOverflow)
      pointOnScreen.x -= 2 * r
    if (hasYOverflow)
      pointOnScreen.y -= 2 * r

    popup.location = pointOnScreen
  }

  private fun updateZoomedImage(point: Point, imageSize: Dimension, radius: Int) : BufferedImage {
    val image = texturePanel.getTextureImage()

    val originX = min((image.width * point.x) / imageSize.width, image.width - 1)
    val originY = min((image.height * point.y) / imageSize.height, image.height - 1)

    texturePanel.updatePixelInfo(originX, originY)
    val zoomedImage = TexturePreviewUtil.getZoomedSubimage(image, originX, originY, radius, PIXEL_WIDTH)

    val g2d = zoomedImage.createGraphics()
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.color = JBColor.border()
    g2d.stroke = BasicStroke(4f)
    g2d.draw(Ellipse2D.Double(0.0, 0.0, zoomedImage.width - 1.0, zoomedImage.width - 1.0))
    g2d.dispose()

    return zoomedImage
  }
}