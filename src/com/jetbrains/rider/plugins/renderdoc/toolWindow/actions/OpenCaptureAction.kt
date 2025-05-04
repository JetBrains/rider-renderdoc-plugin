package com.jetbrains.rider.plugins.renderdoc.toolWindow.actions

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.JBInsets
import com.jetbrains.rider.plugins.renderdoc.toolWindow.RenderDocDataKeys
import javax.swing.JComponent

class OpenCaptureAction : DumbAwareAction(), CustomComponentAction {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
    return ActionButtonWithText(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE).apply {
      putClientProperty("ActionToolbar.smallVariant", true)
      putClientProperty("customButtonInsets", JBInsets(1).asUIResource())
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.getData(RenderDocDataKeys.RENDERDOC_TOOL) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val tool = e.getData(RenderDocDataKeys.RENDERDOC_TOOL) ?: return
    tool.openCapture()
  }
}