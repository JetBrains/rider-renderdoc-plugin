package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.ContentFactory
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocService

class RenderDocToolWindowFactory : ToolWindowFactory {
  override fun init(toolWindow: ToolWindow) {
    toolWindow.stripeTitle = RenderDocBundle.message("renderdoc.tool.title")
    toolWindow.component.putClientProperty(ToolWindowContentUi.DONT_HIDE_TOOLBAR_IN_HEADER, true)
  }

  override suspend fun isApplicableAsync(project: Project): Boolean = RenderDocService.getInstance(project).isAvailable()

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val container = RenderDocTool(project, RenderDocService.getInstance(project))
    Disposer.register(toolWindow.disposable, container)
    val actionManager = ActionManager.getInstance()
    toolWindow.setTitleActions(listOf(actionManager.getAction("OpenRenderDocCapture")))
    val dialogContent = ContentFactory.getInstance().createContent(container, null, false).apply { isCloseable = false }
    toolWindow.contentManager.addContent(dialogContent)
  }
}