package com.jetbrains.rider.plugins.renderdoc.execution

import com.jetbrains.rider.plugins.renderdoc.common.execution.LifetimedProcessHandler
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugSessionInfo
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.Deferred

class ShaderDebugProcessHandler(processLifetimeDef: LifetimeDefinition, val shaderDebugSession: Deferred<RenderDocDebugSessionInfo?>) : LifetimedProcessHandler(processLifetimeDef)