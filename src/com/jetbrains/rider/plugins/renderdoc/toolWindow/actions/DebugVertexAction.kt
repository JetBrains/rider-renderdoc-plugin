package com.jetbrains.rider.plugins.renderdoc.toolWindow.actions

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocVertexInput
import com.jetbrains.rider.plugins.renderdoc.execution.DebugVertexConfigurationType
import com.jetbrains.rider.plugins.renderdoc.execution.DebugVertexRunConfiguration
import com.jetbrains.rider.plugins.renderdoc.toolWindow.RenderDocDataKeys
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.CaptureInfo

class DebugVertexAction : RenderDocDebugAction() {
  override fun debug(project: Project, captureInfo: CaptureInfo, drawCallId: UInt?, input: RenderDocDebugInput) {
    val runSettings = RunManager.getInstance(project).createConfiguration("Debug Vertex", DebugVertexConfigurationType::class.java)
    val configuration = runSettings.configuration as DebugVertexRunConfiguration
    configuration.file = captureInfo.file
    configuration.drawCallId = drawCallId
    configuration.input = input as RenderDocVertexInput
    ProgramRunnerUtil.executeConfiguration(runSettings, DefaultDebugExecutor.getDebugExecutorInstance())
  }

  override fun getDebugInput(e: AnActionEvent): RenderDocDebugInput {
    return e.getData(RenderDocDataKeys.RENDERDOC_VERTEX_DEBUG_INPUT) ?: RenderDocVertexInput.DEFAULT
  }
}
