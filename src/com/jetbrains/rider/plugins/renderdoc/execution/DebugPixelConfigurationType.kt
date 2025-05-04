package com.jetbrains.rider.plugins.renderdoc.execution

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.VirtualConfigurationType
import com.intellij.icons.AllIcons
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle

class DebugPixelConfigurationType : ConfigurationTypeBase(id = "DebugPixelConfiguration",
                                                          displayName = RenderDocBundle.message("shader.debug.pixel.configuration.name"),
                                                          description = null,
                                                          icon = AllIcons.Actions.StartDebugger),
                                    VirtualConfigurationType {
  init {
    addFactory(DebugPixelRunConfigurationFactory(this))
  }
}
