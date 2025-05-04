package com.jetbrains.rider.plugins.renderdoc.debugger.frame

import com.intellij.xdebugger.frame.*
import com.jetbrains.renderdoc.rdClient.model.RdcShaderVariable
import com.jetbrains.renderdoc.rdClient.model.RdcSourceVariableMapping
import com.jetbrains.renderdoc.rdClient.model.RdcVarType

open class RenderDocValue(private val resolver: RenderDocVariableResolver, private val variable: RdcShaderVariable?, private val outerType: RdcVarType) : XValue() {
  companion object {
    fun create(resolver: RenderDocVariableResolver, rdcVariable: RdcSourceVariableMapping): XValue {
      if (rdcVariable.type != RdcVarType.ConstantBlock && rdcVariable.rows * rdcVariable.columns >= 0u) {
        return RenderDocVectorValue(resolver, rdcVariable)
      }
      return RenderDocValue(resolver, resolver.resolveVariableReference(rdcVariable.variables.first()), rdcVariable.type)
    }

    fun create(resolver: RenderDocVariableResolver, variable: RdcShaderVariable): XValue {
      if (variable.type != RdcVarType.ConstantBlock && variable.rows * variable.columns >= 0u) {
        return RenderDocVectorValue(resolver, variable)
      }
      return RenderDocValue(resolver, variable.members.first(), variable.type)
    }
  }

  override fun computePresentation(node: XValueNode, place: XValuePlace) {
    if (variable == null) return
    var type = variable.type
    if (type == RdcVarType.Unknown)
      type = outerType
    node.setPresentation(null, type.name, variable.evaluate(resolver, outerType, 0).first(), variable.members.isNotEmpty())
  }

  override fun computeChildren(node: XCompositeNode) {
    if (variable == null) return

    val members = variable.members
    val varList = XValueChildrenList(members.size)
    for (child in members) {
      varList.add(child.name, create(resolver, child))
    }
    node.addChildren(varList, true)
  }
}