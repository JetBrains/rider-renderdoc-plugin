package com.jetbrains.rider.plugins.renderdoc.toolWindow.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBInsets
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import com.jetbrains.rider.plugins.renderdoc.toolWindow.RenderDocDataKeys
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.CaptureInfo
import com.jetbrains.rider.projectView.actions.isProjectModelReady
import javax.swing.JComponent

abstract class RenderDocDebugAction : DumbAwareAction(), CustomComponentAction {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val project = e.project
    if (project == null || !project.isInitialized || DumbService.isDumb(project) || !project.isProjectModelReady()) {
      e.presentation.isEnabled = false
      return
    }

    e.presentation.isVisible = e.getData(RenderDocDataKeys.RENDERDOC_CAPTURE_INFO) != null
    e.presentation.isEnabled = e.presentation.isVisible
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
    return ActionButton(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE).apply {
      putClientProperty("ActionToolbar.smallVariant", true)
      putClientProperty("customButtonInsets", JBInsets(1).asUIResource())
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    if (!project.isInitialized || DumbService.isDumb(project) || !project.isProjectModelReady())
      return

    val drawCallId = if (!e.isFromActionToolbar) {
      e.getData(RenderDocDataKeys.DRAW_CALL_ID) ?: return
    }
    else null

    val captureInfo = e.getData(RenderDocDataKeys.RENDERDOC_CAPTURE_INFO) ?: return
    val input = getDebugInput(e)

    debug(project, captureInfo, drawCallId, input)
  }

  protected abstract fun getDebugInput(e: AnActionEvent) : RenderDocDebugInput
  protected abstract fun debug(project: Project, captureInfo: CaptureInfo, drawCallId: UInt?, input: RenderDocDebugInput)
}
