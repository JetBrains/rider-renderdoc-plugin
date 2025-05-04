package com.jetbrains.rider.plugins.renderdoc.debugger.frame

import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList
import com.jetbrains.renderdoc.rdClient.model.RdcDebugVariableType
import com.jetbrains.renderdoc.rdClient.model.RdcSourceVariableMapping
import com.jetbrains.renderdoc.rdClient.model.RdcVarType
import com.jetbrains.rider.plugins.renderdoc.debugger.RenderDocDrawCallDebugInfo
import com.jetbrains.rider.plugins.renderdoc.debugger.RenderDocSourcePosition

class RenderDocStackFrame(private val drawCallInfo: RenderDocDrawCallDebugInfo, private val position: RenderDocSourcePosition) : XStackFrame() {

  override fun getSourcePosition(): XSourcePosition = position

  override fun computeChildren(node: XCompositeNode) {
    if (drawCallInfo.debugTrace == null || drawCallInfo.variableResolverService == null) return
    node.addChildren(computeChildrenFromVariables(drawCallInfo.debugTrace.sourceVars), false)
    node.addChildren(computeChildrenFromVariables(drawCallInfo.variableResolverService.stageVariables, true), true)
  }

  private fun computeChildrenFromVariables(variables: Array<RdcSourceVariableMapping>, isStageVars: Boolean = false) : XValueChildrenList {
    if (drawCallInfo.variableResolverService == null) return XValueChildrenList.EMPTY
    val groupedVars = variables.groupBy { isStageVars || it.variables.firstOrNull()?.type == RdcDebugVariableType.Variable }
    val varList = XValueChildrenList(groupedVars.size)

    for ((info, vars) in groupedVars.getOrDefault(true, emptyList()).groupBy {
      val parts = it.name.split('.')
      RenderDocVariableInfo(parts.first(), it.type,  it.variables.firstOrNull()?.type, it.signatureIndex, parts.size == 1)
    }) {
      if (info.type == RdcVarType.Unknown) continue
      if (vars.size == 1) {
        varList.add(RenderDocNamedValue(drawCallInfo.variableResolverService, vars.first(), info.name))
      } else {
        varList.addBottomGroup(RenderDocVariablesGroup(drawCallInfo.variableResolverService, vars, 0, info.name))
      }
    }

    val constants = groupedVars[false] ?: return varList
    val constGroup = varList.topGroups.firstOrNull { it.name == "Constants & Resources" } as RenderDocVariablesGroup?
    if (constGroup != null)
      constGroup.addChildren(constants)
    else
      varList.addTopGroup(RenderDocVariablesGroup(drawCallInfo.variableResolverService, constants, -1, "Constants & Resources"))

    return varList
  }
}