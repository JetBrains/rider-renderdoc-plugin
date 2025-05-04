package com.jetbrains.rider.plugins.renderdoc.debugger.frame

import com.intellij.xdebugger.frame.*
import com.jetbrains.renderdoc.rdClient.model.RdcSourceVariableMapping

open class RenderDocNamedValue(
  resolver: RenderDocVariableResolver,
  rdcVariable: RdcSourceVariableMapping,
  name: String
) : XNamedValue(name) {

  private val value: XValue = RenderDocValue.create(resolver, rdcVariable)

  override fun computePresentation(node: XValueNode, place: XValuePlace) {
    value.computePresentation(node, place)
  }

  override fun computeChildren(node: XCompositeNode) = value.computeChildren(node)
}