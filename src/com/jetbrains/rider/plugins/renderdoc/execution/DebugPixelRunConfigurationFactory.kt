package com.jetbrains.rider.plugins.renderdoc.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class DebugPixelRunConfigurationFactory(configurationType: DebugPixelConfigurationType) : ConfigurationFactory(configurationType) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration = DebugPixelRunConfiguration(project, this, null)

  override fun getId(): String = type.id
}
