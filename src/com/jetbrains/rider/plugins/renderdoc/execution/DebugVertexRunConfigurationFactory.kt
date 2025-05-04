package com.jetbrains.rider.plugins.renderdoc.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class DebugVertexRunConfigurationFactory(configurationType: DebugVertexConfigurationType) : ConfigurationFactory(configurationType) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration = DebugVertexRunConfiguration(project, this, null)

  override fun getId(): String = type.id
}
