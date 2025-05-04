package com.jetbrains.rider.plugins.renderdoc.debugger.frame

import com.jetbrains.renderdoc.rdClient.model.*
import kotlin.math.min

@OptIn(ExperimentalUnsignedTypes::class)
internal fun RdcShaderVariable.evaluate(resolver: RenderDocVariableResolver, parType: RdcVarType, idx: Int): List<String> {
  var type = parType
  if (type == RdcVarType.Unknown) type = this.type
  val value = when (type) {
    RdcVarType.Float -> value.f32v[idx]
    RdcVarType.Double -> value.f64v[idx]
    RdcVarType.Half -> value.f16v[idx]
    RdcVarType.SInt -> value.s32v[idx]
    RdcVarType.UInt -> value.u32v[idx]
    RdcVarType.SShort -> value.s16v[idx]
    RdcVarType.UShort -> value.u16v[idx]
    RdcVarType.SLong -> value.s64v[idx]
    RdcVarType.ULong -> value.u64v[idx]
    RdcVarType.SByte -> value.s8v[idx]
    RdcVarType.UByte -> value.u8v[idx]
    RdcVarType.Bool -> value.u32v[idx] != 0u
    RdcVarType.Unknown -> value.f32v[idx] // TODO: handle int and float view mode
    RdcVarType.Enum -> "{ ... }"
    RdcVarType.Struct -> "{ ... }"
    RdcVarType.ConstantBlock -> "Constant Block" // TODO: handle this
    RdcVarType.GPUPointer -> getPointerValue()
    else -> null
  }?.toString()
  if (value != null) return listOf(value)
  return when (type) {
    RdcVarType.ReadOnlyResource -> evaluateResource(resolver, type)
    RdcVarType.ReadWriteResource -> evaluateResource(resolver, type)
    RdcVarType.Sampler ->  evaluateSampler(resolver)
    else -> listOf("") // unused
  }
}

internal enum class DescriptorCategory {
  Unknown,
  ConstantBlock,
  Sampler,
  ReadOnlyResource,
  ReadWriteResource;

  companion object {
    fun fromUInt(value: UInt) = when (value) {
      0u -> Unknown
      1u -> ConstantBlock
      2u -> Sampler
      3u -> ReadOnlyResource
      4u -> ReadWriteResource
      else -> throw IndexOutOfBoundsException("Can't convert $value to DescriptorCategory")
    }
  }

  fun isForType(type: RdcDescriptorType): Boolean {
    if (type == RdcDescriptorType.ConstantBuffer)
      return this == ConstantBlock
    if (type == RdcDescriptorType.Sampler)
      return this == Sampler
    if (type == RdcDescriptorType.ImageSampler || type == RdcDescriptorType.Image || type == RdcDescriptorType.TypedBuffer ||
        type == RdcDescriptorType.Buffer || type == RdcDescriptorType.AccelerationStructure)
      return this == ReadOnlyResource
    if (type == RdcDescriptorType.ReadWriteBuffer || type == RdcDescriptorType.ReadWriteImage ||
        type == RdcDescriptorType.ReadWriteTypedBuffer)
      return this == ReadWriteResource
    return type == RdcDescriptorType.Unknown
  }
}

internal data class ShaderBindIndex(val category: DescriptorCategory, val index: UInt, val elem: UInt)

@OptIn(ExperimentalUnsignedTypes::class)
internal fun RdcShaderVariable.evaluateResource(resolver: RenderDocVariableResolver, type: RdcVarType): List<String> {
  val details = resolver.shaderDetails
  val bind = ShaderBindIndex(DescriptorCategory.fromUInt(value.u32v[0]), value.u32v[1], 0u)

  val list = if (type == RdcVarType.ReadOnlyResource) resolver.readOnlyResources else resolver.readWriteResources

  val descriptors = list.filter { bind.category.isForType(it.access.type) && it.access.index.toUInt() == bind.index }
  if (descriptors.isEmpty()) return listOf("")

  val res = (if (type == RdcVarType.ReadOnlyResource) details.readOnlyResources else details.readWriteResources)[bind.index.toInt()]

  if (res.bindArraySize == 1u)
    return listOf(resolver.resolveResource(descriptors.first().resourceId))

  if (res.bindArraySize == 0u.inv())
    return listOf("[unbounded]")

  return buildList {
    add("[${res.bindArraySize}]")
    for (i in 0..<min(res.bindArraySize.toInt(), descriptors.size)) {
      add("${name}[${i}]")
    }
  }
}

@OptIn(ExperimentalUnsignedTypes::class)
internal fun RdcShaderVariable.evaluateSampler(resolver: RenderDocVariableResolver): List<String> {
  val bind = ShaderBindIndex(DescriptorCategory.fromUInt(value.u32v[0]), value.u32v[1], value.u32v[2])
  if (bind.category != DescriptorCategory.Sampler)
    return listOf("")

  val desc = resolver.samplers.firstOrNull { bind.category.isForType(it.access.type) &&
                                             it.access.index.toUInt() == bind.index &&
                                             it.access.arrayElement == bind.elem }
  if (desc == null)
    return listOf("")

  val samp = resolver.shaderDetails.samplers[bind.index.toInt()]

  if (samp.bindArraySize == 1u)
    return listOf(evaluateSampler(resolver, samp, 0u.inv(), desc.resourceId))
  if (samp.bindArraySize == 0u.inv())
    return listOf("")
  return buildList {
    add("[${samp.bindArraySize}]")
    for (i in 0u..<samp.bindArraySize) {
      add(evaluateSampler(resolver, samp, i, desc.resourceId))
    }
  }
}

internal fun evaluateSampler(resolver: RenderDocVariableResolver, sampler: RdcShaderSampler, elem: UInt, id: ULong?): String {
  if (id == null) {
    return buildString {
      if (sampler.fixedBindSetOrSpace > 0u) {
        if (resolver.api == RdcGraphicsApi.D3D11 || resolver.api == RdcGraphicsApi.D3D12) {
          append("space${sampler.fixedBindSetOrSpace}, ")
        }
        else {
          append("Set ${sampler.fixedBindSetOrSpace}, ")
        }
      }
      if (elem == 0u.inv() || sampler.bindArraySize == 1u)
        append(sampler.fixedBindNumber)
      else
        append("${sampler.fixedBindSetOrSpace} [${elem}]")
    }
  }
  return resolver.resolveResource(id)
}

internal fun RdcDebugVariableReference.evaluate(resolver: RenderDocVariableResolver, parType: RdcVarType): List<String> {
  val variable = resolver.getDebugVariable(this) ?: return listOf("Unknown value type")
  return variable.evaluate(resolver, parType, component.toInt())
}

@OptIn(ExperimentalUnsignedTypes::class)
internal fun RdcShaderVariable.getPointerValue() = "GPUAddress::${value.u64v[0]}::${value.u64v[2]}::${value.u64v[1] and 0xFFFFFFFFu}"

