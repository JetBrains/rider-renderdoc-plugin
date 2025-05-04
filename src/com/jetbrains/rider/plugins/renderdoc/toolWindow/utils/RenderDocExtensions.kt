package com.jetbrains.rider.plugins.renderdoc.toolWindow.utils

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.renderdoc.rdClient.model.RdcAction
import com.jetbrains.renderdoc.rdClient.model.RdcActionFlags
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.ShaderUsages
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.TextureInfo
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.TextureType
import java.util.*
import kotlin.io.path.Path

internal fun RdcAction.isDrawCall(): Boolean = flags.run {
  contains(RdcActionFlags.Drawcall) || contains(RdcActionFlags.MeshDispatch)
}

internal fun Array<RdcAction>.collectShaderFilesUsages(): ShaderUsages {
  val entryPoints = mutableMapOf<String, MutableSet<UInt>>()
  val otherShaders = mutableMapOf<String, MutableSet<UInt>>()
  this.forEach { action ->
    val usagesInfo = action.collectShaderFilesUsages()
    usagesInfo.entryPoints.forEach { (path, eventIds) ->
      entryPoints.getOrPut(path) { mutableSetOf() }.addAll(eventIds)
    }
    usagesInfo.otherShaders.forEach { (path, eventIds) ->
      otherShaders.getOrPut(path) { mutableSetOf() }.addAll(eventIds)
    }
  }
  return ShaderUsages(entryPoints, otherShaders)
}

internal fun RdcAction.collectShaderFilesUsages(): ShaderUsages {
  if (children.isNotEmpty())
    return children.collectShaderFilesUsages()
  val entryPoints = usedSourceFilePaths?.entrypointPaths?.associate { it to setOf(eventId) } ?: emptyMap()
  val includes = usedSourceFilePaths?.otherIncludedFilePaths?.filter { path -> !Path(path).isAbsolute }?.associateWith { setOf(eventId) }
                 ?: emptyMap<@NlsSafe String, Set<UInt>>()

  return ShaderUsages(entryPoints, includes)
}

internal fun EnumMap<TextureType, List<TextureInfo>>.tryGetTexture(type: TextureType, index: Int) : TextureInfo? {
  if (index < 0)
    return null
  return get(type)?.getOrNull(index)
}

internal fun EnumMap<TextureType, List<TextureInfo>>.getFirstOfTypeOrDefault(type: TextureType) : TextureInfo? {
  val textures = get(type)
  if (textures != null && textures.isNotEmpty())
    return textures.first()

  for ((_, textures) in entries) {
    if (textures.isNotEmpty()) {
      return textures.first()
    }
  }
  return null
}

internal fun EnumMap<TextureType, List<TextureInfo>>.getTexturePlainIndex(type: TextureType, index: Int) : Int {
  if (index < 0)
    return -1
  var idx = 0
  for ((k, v) in entries) {
    if (k == type) {
      return idx + index
    }
    idx += v.size
  }
  return -1
}

internal fun EnumMap<TextureType, List<TextureInfo>>.getTextureNumber() : Int {
  return values.sumOf { it.size }
}