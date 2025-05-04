package com.jetbrains.rider.plugins.renderdoc.debugger.frame

import com.jetbrains.renderdoc.rdClient.model.*

class RenderDocVariableResolver(rdcSession: RdcDrawCallDebugSession, val api: RdcGraphicsApi)
{
  private val allResources = rdcSession.allResources.associateTo(hashMapOf()) { Pair(it.id, it.name) }
  private val rdcDebugTrace = rdcSession.debugTrace
  internal val readOnlyResources = rdcSession.readOnlyResources
  internal val readWriteResources = rdcSession.readWriteResources
  internal val samplers = rdcSession.samplers
  internal val shaderDetails = rdcSession.shaderDetails
  private val variableCache = hashMapOf<RdcDebugVariableType, HashMap<String, RdcShaderVariable>>()
  var stageVariables: Array<RdcSourceVariableMapping> = arrayOf()

  init {
    variableCache[RdcDebugVariableType.Input] = rdcDebugTrace.inputs.associateByTo(hashMapOf()) { it.name }
    variableCache[RdcDebugVariableType.Constant] = rdcDebugTrace.constantBlocks.associateByTo(hashMapOf()) { it.name }
    variableCache[RdcDebugVariableType.Sampler] = rdcDebugTrace.samplers.associateByTo(hashMapOf()) { it.name }
    variableCache[RdcDebugVariableType.ReadOnlyResource] = rdcDebugTrace.readOnlyResources.associateByTo(hashMapOf()) { it.name }
    variableCache[RdcDebugVariableType.ReadWriteResource] = rdcDebugTrace.readWriteResources.associateByTo(hashMapOf()) { it.name }
    variableCache[RdcDebugVariableType.Variable] = hashMapOf()
  }

  fun loadStageVariablesInfo(info: RdcStageInfo) {
    stageVariables = info.currentVariables
    variableCache[RdcDebugVariableType.Variable] = info.availableVariable.associateByTo(hashMapOf()) { it.name }
  }

  fun resolveVariableReference(ref: RdcDebugVariableReference) = getDebugVariable(ref)

  fun getDebugVariable(ref: RdcDebugVariableReference): RdcShaderVariable? {
    val type = ref.type
    if (type == RdcDebugVariableType.Input || type == RdcDebugVariableType.Constant || type == RdcDebugVariableType.Variable)
      return getShaderDebugVariable(ref.name, variableCache[ref.type] ?: emptyMap())
    return variableCache[ref.type]?.get(ref.name)
  }

  fun resolveResource(id: ULong?) : String {
    return allResources.getOrDefault(id, "ResourceId::${id ?: "0"}")
  }

  private fun getShaderDebugVariable(name_: String, vars: Map<String, RdcShaderVariable>): RdcShaderVariable? {
    var name = name_
    var elem = ""

    if (name.startsWith('[')) {
      elem = name.substring(0, name.indexOf(']') + 1)
      if (elem.isEmpty()) return null

      var idx = elem.length
      if (idx != name.length && name[idx] == '.')
        idx++

      name = name.drop(idx)
    }
    else {
      var idx = name.indexOfFirst { it == '[' || it == '.' }
      if (idx == -1) {
        val x = name
        name = elem
        elem = x
      }
      else {
        elem = name.substring(0, idx)

        if (name[idx] == '.')
          idx++

        name = name.substring(idx)
      }
    }
    val variable = vars[elem] ?: return null

    if (name.isEmpty())
      return variable

    return getShaderDebugVariable(name, variable.members.associateBy { it.name })
  }
}
