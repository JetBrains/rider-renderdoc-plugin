package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.rd.defineNestedLifetime
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocPixelInput
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocService
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocVertexInput
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.threading.coroutines.launch
import com.jetbrains.renderdoc.rdClient.model.RdcCapture

internal class RenderDocPreviewerTabbedPane(service: RenderDocService) : JBTabbedPane(), Disposable {
  private val lifetime = defineNestedLifetime()
  private val lifetimes = SequentialLifetimes(lifetime)

  private val texturePreviewer = RenderDocTexturePreviewPanel(service)
  private val meshPreviewer = RenderDocMeshPreviewPanel(service)

  private val activeTab
    get() = selectedComponent as RenderDocAbstractPreviewer

  internal var eventId: Long? = null

  init {
    addTab(RenderDocBundle.message("renderdoc.tool.previewer.texture.title"), texturePreviewer)
    addTab(RenderDocBundle.message("renderdoc.tool.previewer.mesh.title"), meshPreviewer)
  }

  fun defaultPreview() {
    val lifetime = lifetimes.next()
    lifetime.launch {
      eventId = null.also {
        activeTab.defaultPreview()
        isVisible = true
      }
    }
  }

  /**
   * Synchronizes the previewer with the specified event ID and input.
   * 
   * @param capture The RenderDoc capture
   * @param eventId The event ID to sync with, or null to reset to default state
   * @param input The debug input to use, or null to use the default input for the target tab
   */
  internal fun syncWithEventId(capture: RdcCapture, eventId: Long?, input: RenderDocDebugInput?) {
    val lifetime = lifetimes.next()
    lifetime.launch {
      if (eventId == null) {
        this@RenderDocPreviewerTabbedPane.eventId = null.also { activeTab.defaultPreview() }
        return@launch
      }

      val targetTab = when (input) {
        is RenderDocPixelInput -> texturePreviewer
        is RenderDocVertexInput -> meshPreviewer
        else -> activeTab.also { if (activeTab === texturePreviewer) texturePreviewer.hideTextureSelection() }
      }

      this@RenderDocPreviewerTabbedPane.eventId = eventId.also {
        targetTab.syncPreviewWithEvent(capture, eventId, input ?: targetTab.getDefaultInput())
        if (targetTab !== activeTab)
          selectedComponent = targetTab
      }
    }
  }

  fun getCurrentInput() = activeTab.getCurrentInput()
  fun getPixelInput() = texturePreviewer.getCurrentInput()
  fun getVertexInput() = meshPreviewer.getCurrentInput()

  override fun dispose() {
    Disposer.dispose(texturePreviewer)
    Disposer.dispose(meshPreviewer)
  }
}