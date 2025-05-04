package com.jetbrains.rider.plugins.renderdoc.execution

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.EDT
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.rider.plugins.renderdoc.common.execution.CoroutineProgramRunner
import com.jetbrains.rider.plugins.renderdoc.debugger.RenderDocDebugProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NonNls

class ShaderDebugRunner(scope: CoroutineScope) : CoroutineProgramRunner<RunnerSettings>(scope) {
  companion object {
    const val RUNNER_ID: @NonNls String = "ShaderDebugRunner"
  }

  override suspend fun execute(environment: ExecutionEnvironment, state: RunProfileState): RunContentDescriptor? {
    val executionResult = state.execute(environment.executor, this) ?: return null
    val processHandler = executionResult.processHandler
    if (processHandler !is ShaderDebugProcessHandler)
    {
      processHandler.destroyProcess()
      return null
    }

    val shaderDebugSession = processHandler.shaderDebugSession.await() ?: return null
    processHandler.startNotify()

    return withContext(Dispatchers.EDT) {
      val starter = object : XDebugProcessStarter() {
        override fun start(session: XDebugSession): XDebugProcess = RenderDocDebugProcess(session, environment.project, processHandler, shaderDebugSession)
      }
      XDebuggerManager.getInstance(environment.project).startSession(environment, starter)
    }.runContentDescriptor
  }

  override fun getRunnerId(): String = RUNNER_ID

  override fun canRun(executorId: String, profile: RunProfile): Boolean = DefaultDebugExecutor.EXECUTOR_ID == executorId && profile is ShaderDebugProfile
}