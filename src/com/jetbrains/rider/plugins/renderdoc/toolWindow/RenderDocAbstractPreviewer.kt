package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.intellij.openapi.Disposable
import com.jetbrains.renderdoc.rdClient.model.RdcCapture
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import java.awt.BorderLayout
import javax.swing.JPanel

abstract class RenderDocAbstractPreviewer : JPanel(BorderLayout()), Disposable {
  abstract fun defaultPreview()
  abstract fun syncPreviewWithEvent(capture: RdcCapture, eventId: Long, input: RenderDocDebugInput = getDefaultInput())
  abstract fun getCurrentInput() : RenderDocDebugInput
  abstract fun getDefaultInput() : RenderDocDebugInput
  override fun dispose() : Unit = Unit
}
