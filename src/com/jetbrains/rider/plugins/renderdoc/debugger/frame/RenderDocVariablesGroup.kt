package com.jetbrains.rider.plugins.renderdoc.debugger.frame

import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueGroup
import com.jetbrains.renderdoc.rdClient.model.RdcSourceVariableMapping
import com.jetbrains.renderdoc.rdClient.model.RdcVarType

class RenderDocVariablesGroup(
  private val resolver: RenderDocVariableResolver,
  vars: List<RdcSourceVariableMapping>,
  private val depth: Int = 0,
  name: String
) : XValueGroup(name) {

  private val rdcVariables = vars.toMutableList()

  override fun computeChildren(node: XCompositeNode) {
    val varList = XValueChildrenList(rdcVariables.size)
    for ((info, vars) in rdcVariables.groupBy {
      val parts = it.name.split('.')
      RenderDocVariableInfo(parts.getOrElse(depth + 1) { "" }, it.type, it.variables.firstOrNull()?.type, it.signatureIndex, parts.size == depth + 2)
    }) {
      if (info.name.isEmpty() || info.type == RdcVarType.Unknown) continue
      if (vars.size == 1 && info.isLeaf) {
        varList.add(RenderDocNamedValue(resolver, vars.first(), info.name))
      }
      else {
        varList.addTopGroup(RenderDocVariablesGroup(resolver, vars, depth + 1, info.name))
      }
    }
    node.addChildren(varList, true)
  }

  fun addChildren(children: List<RdcSourceVariableMapping>) {
    rdcVariables.addAll(children)
  }
}