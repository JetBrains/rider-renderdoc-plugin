package com.jetbrains.rider.plugins.renderdoc.debugger.frame

import com.jetbrains.renderdoc.rdClient.model.RdcDebugVariableType
import com.jetbrains.renderdoc.rdClient.model.RdcVarType

data class RenderDocVariableInfo(val name: String, val type: RdcVarType, val varType: RdcDebugVariableType?, val signatureIndex: Int, val isLeaf: Boolean)