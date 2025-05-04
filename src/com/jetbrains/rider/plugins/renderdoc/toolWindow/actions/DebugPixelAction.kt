package com.jetbrains.rider.plugins.renderdoc.toolWindow.actions

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocPixelInput
import com.jetbrains.rider.plugins.renderdoc.execution.DebugPixelConfigurationType
import com.jetbrains.rider.plugins.renderdoc.execution.DebugPixelRunConfiguration
import com.jetbrains.rider.plugins.renderdoc.toolWindow.RenderDocDataKeys
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.CaptureInfo

class DebugPixelAction : RenderDocDebugAction() {
  override fun getDebugInput(e: AnActionEvent): RenderDocDebugInput {
    return e.getData(RenderDocDataKeys.RENDERDOC_PIXEL_DEBUG_INPUT) ?: RenderDocPixelInput.DEFAULT
  }

  override fun debug(project: Project, captureInfo: CaptureInfo, drawCallId: UInt?, input: RenderDocDebugInput) {
    val runSettings = RunManager.getInstance(project).createConfiguration("Debug Pixel", DebugPixelConfigurationType::class.java)
    val configuration = runSettings.configuration as DebugPixelRunConfiguration
    configuration.file = captureInfo.file
    configuration.drawCallId = drawCallId
    configuration.input = input as RenderDocPixelInput
    ProgramRunnerUtil.executeConfiguration(runSettings, DefaultDebugExecutor.getDebugExecutorInstance())
  }
}
