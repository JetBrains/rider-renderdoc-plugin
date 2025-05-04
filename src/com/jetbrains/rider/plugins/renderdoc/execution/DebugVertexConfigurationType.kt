package com.jetbrains.rider.plugins.renderdoc.execution

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.VirtualConfigurationType
import com.intellij.icons.AllIcons
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle

class DebugVertexConfigurationType : ConfigurationTypeBase(id = "DebugVertexConfiguration",
                                                           displayName = RenderDocBundle.message("shader.debug.vertex.configuration.name"),
                                                           description = null,
                                                           icon = AllIcons.Actions.StartDebugger),
                                     VirtualConfigurationType {
  init {
    addFactory(DebugVertexRunConfigurationFactory(this))
  }
}
