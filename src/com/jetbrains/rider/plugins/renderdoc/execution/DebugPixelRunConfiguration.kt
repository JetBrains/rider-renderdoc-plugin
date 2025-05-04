package com.jetbrains.rider.plugins.renderdoc.execution

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocService
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.coroutines.async
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocPixelInput
import javax.swing.JComponent
import javax.swing.JLabel

class DebugPixelRunConfiguration(
  project: Project,
  factory: ConfigurationFactory?,
  name: String?
) : RunConfigurationBase<DebugPixelRunConfiguration>(project, factory, name),
    RunConfigurationWithSuppressedDefaultRunAction, WithoutOwnBeforeRunSteps, ShaderDebugProfile {
  var file: VirtualFile? = null
  var drawCallId: UInt? = null
  var input: RenderDocPixelInput = RenderDocPixelInput.DEFAULT

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = State(environment, this)

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = MySettingsEditor()

  private class MySettingsEditor : SettingsEditor<DebugPixelRunConfiguration>() {
    override fun resetEditorFrom(s: DebugPixelRunConfiguration) {
    }

    override fun applyEditorTo(s: DebugPixelRunConfiguration) {
    }

    override fun createEditor(): JComponent {
      return JLabel(RenderDocBundle.message("shader.debug.pixel.configuration"))
    }
  }

  private class State(val environment: ExecutionEnvironment, val configuration: DebugPixelRunConfiguration) : RunProfileState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
      val file = configuration.file ?: return null
      val drawCallId = configuration.drawCallId

      val processLifetimeDefinition = LifetimeDefinition()
      val shaderDebugSession = processLifetimeDefinition.async {
        if (drawCallId == null) {
          RenderDocService.getInstance(environment.project).tryDebugPixel(processLifetimeDefinition, file, configuration.input)
        } else {
          RenderDocService.getInstance(environment.project).debugPixel(processLifetimeDefinition, file, drawCallId, configuration.input)
        }
      }

      val processHandler = ShaderDebugProcessHandler(processLifetimeDefinition, shaderDebugSession)
      return DefaultExecutionResult(processHandler)
    }
  }
}
