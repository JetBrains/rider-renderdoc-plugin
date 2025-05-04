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
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocVertexInput
import javax.swing.JComponent
import javax.swing.JLabel

class DebugVertexRunConfiguration(
  project: Project,
  factory: ConfigurationFactory?,
  name: String?
) : RunConfigurationBase<DebugVertexRunConfiguration>(project, factory, name),
    RunConfigurationWithSuppressedDefaultRunAction, WithoutOwnBeforeRunSteps, ShaderDebugProfile {
  var file: VirtualFile? = null
  var drawCallId: UInt? = null
  var input: RenderDocVertexInput = RenderDocVertexInput.DEFAULT

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = State(environment, this)

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = MySettingsEditor()

  private class MySettingsEditor : SettingsEditor<DebugVertexRunConfiguration>() {
    override fun resetEditorFrom(s: DebugVertexRunConfiguration) {
    }

    override fun applyEditorTo(s: DebugVertexRunConfiguration) {
    }

    override fun createEditor(): JComponent {
      return JLabel(RenderDocBundle.message("shader.debug.vertex.configuration"))
    }
  }

  private class State(val environment: ExecutionEnvironment, val configuration: DebugVertexRunConfiguration) : RunProfileState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
      val file = configuration.file ?: return null
      val drawCallId = configuration.drawCallId
      val input = configuration.input

      val processLifetimeDefinition = LifetimeDefinition()
      val shaderDebugSession = processLifetimeDefinition.async {
        if (drawCallId == null) {
          RenderDocService.getInstance(environment.project).tryDebugVertex(processLifetimeDefinition, file, input)
        }
        else {
          RenderDocService.getInstance(environment.project).debugVertex(processLifetimeDefinition, file, drawCallId, input)
        }
      }

      val processHandler = ShaderDebugProcessHandler(processLifetimeDefinition, shaderDebugSession)
      return DefaultExecutionResult(processHandler)
    }
  }
}
