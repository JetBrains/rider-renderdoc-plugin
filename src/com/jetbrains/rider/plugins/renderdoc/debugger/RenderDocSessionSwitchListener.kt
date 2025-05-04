package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManagerListener
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import com.jetbrains.rider.plugins.renderdoc.toolWindow.RenderDocTool
import java.util.UUID

class RenderDocSessionSwitchListener : XDebuggerManagerListener {
  companion object {
    fun updateToolWindowPreview(sessionId: UUID, project: Project, eventId: UInt?, input: RenderDocDebugInput) {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("RenderDocTool")
      if (toolWindow != null) {
        val renderdocTool = toolWindow.contentManager.selectedContent?.component as? RenderDocTool
        renderdocTool?.syncWithEventIdInDebug(sessionId, eventId, input)
      }
    }
  }
  override fun currentSessionChanged(previousSession: XDebugSession?, currentSession: XDebugSession?) {
    super.currentSessionChanged(previousSession, currentSession)
    val previousProcess = previousSession?.debugProcess as? RenderDocDebugProcess
    val currentProcess = currentSession?.debugProcess as? RenderDocDebugProcess
    val project = currentSession?.project ?: previousSession?.project
    val eventId = currentProcess?.getCurrentEventId()
    if (project == null) return
    if (currentProcess != null && (previousProcess?.getCurrentEventId() != eventId || previousProcess?.getCurrentDebugInput() != currentProcess.getCurrentDebugInput()))
      updateToolWindowPreview(currentProcess.uniqueId, project, eventId, currentProcess.getCurrentDebugInput())
    else if (eventId == null && previousProcess != null)
      updateToolWindowPreview(previousProcess.uniqueId, project, null, previousProcess.getCurrentDebugInput())
  }
}