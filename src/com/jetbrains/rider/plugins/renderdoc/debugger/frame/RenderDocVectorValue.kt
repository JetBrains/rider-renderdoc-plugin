package com.jetbrains.rider.plugins.renderdoc.debugger.frame

import com.intellij.xdebugger.frame.*
import com.jetbrains.renderdoc.rdClient.model.RdcShaderVariable
import com.jetbrains.renderdoc.rdClient.model.RdcSourceVariableMapping
import com.jetbrains.renderdoc.rdClient.model.RdcVarType
import kotlin.math.max
import kotlin.math.min

open class RenderDocVectorValue : XValue {

  private val resolver: RenderDocVariableResolver
  private val rdcVariable: RdcVariableProperties
  private val matrixRow : Int

  constructor(resolver: RenderDocVariableResolver, rdcVariable: RdcVariableProperties, matrixRow : Int = -1) : super() {
    this.resolver = resolver
    this.rdcVariable = rdcVariable
    this.matrixRow = matrixRow
  }

  constructor(resolver: RenderDocVariableResolver, rdcVariable: RdcSourceVariableMapping, matrixRow : Int = -1) : super() {
    this.resolver = resolver
    this.rdcVariable = SourceVariableMapping(rdcVariable)
    this.matrixRow = matrixRow
  }

  constructor(resolver: RenderDocVariableResolver, rdcVariable: RdcShaderVariable, matrixRow : Int = -1) : super() {
    this.resolver = resolver
    this.rdcVariable = ShaderVariable(rdcVariable)
    this.matrixRow = matrixRow
  }

  override fun computePresentation(node: XValueNode, place: XValuePlace) {
    val type = rdcVariable.type.name + if (rdcVariable.columns > 1u) rdcVariable.columns else ""
    if (rdcVariable.rows > 1u && matrixRow == -1) {
      val matrixType = type + "x" + rdcVariable.rows
      node.setPresentation(null, matrixType, rdcVariable.name, true)
    }
    else {
      val c = rdcVariable.columns.toInt()
      val start = c * max(matrixRow, 0)
      val sliceRange = start..<min(start + c, rdcVariable.childrenNumber)
      node.setPresentation(null, type, rdcVariable.evaluateChildren(resolver, sliceRange, matrixRow), false)
    }
  }

  override fun computeChildren(node: XCompositeNode) {
    if (matrixRow != -1) return
    val varList = XValueChildrenList(rdcVariable.childrenNumber)
    for (i in 0..<rdcVariable.rows.toInt()) {
      varList.add("[${i}]", RenderDocVectorValue(resolver, rdcVariable, i))
    }
    node.addChildren(varList, true)
  }


  interface RdcVariableProperties {
    val name: String
    val type: RdcVarType
    val columns: UInt
    val rows: UInt
    val childrenNumber: Int

    fun evaluateChildren(resolver: RenderDocVariableResolver, sliceRange: IntRange, idx: Int): String
  }

  private data class SourceVariableMapping(
    val value: RdcSourceVariableMapping,
    override val name: String = value.name,
    override val type: RdcVarType = value.type,
    override val columns: UInt = value.columns,
    override val rows: UInt = value.rows,
  ) : RdcVariableProperties {
    override val childrenNumber
      get() = value.variables.size

    override fun evaluateChildren(resolver: RenderDocVariableResolver, sliceRange: IntRange, idx: Int): String {
      return value.variables.asList().subList(sliceRange.first, sliceRange.last + 1).joinToString { it.evaluate(resolver, type).first() }
    }
  }

  private data class ShaderVariable(
    val value: RdcShaderVariable,
    override val name: String = value.name,
    override val type: RdcVarType = value.type,
    override val columns: UInt = value.columns,
    override val rows: UInt = value.rows,
  ) : RdcVariableProperties {
    override val childrenNumber
      get() = max(value.members.size, 16)

    override fun evaluateChildren(resolver: RenderDocVariableResolver, sliceRange: IntRange, idx: Int): String {
      if (value.members.isEmpty()) {
        return sliceRange.joinToString { value.evaluate(resolver, type, it).first() }
      }
      return value.members.asList().subList(sliceRange.first, sliceRange.last + 1).joinToString { it.evaluate(resolver, type, 0).first() }
    }
  }

}