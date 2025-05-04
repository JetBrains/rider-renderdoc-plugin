package com.jetbrains.rider.plugins.renderdoc.toolWindow.model

import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.renderdoc.rdClient.model.RdcCapture
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.collectShaderFilesUsages

data class ShaderUsages(val entryPoints: Map<@NlsSafe String, Set<UInt>>, val otherShaders: Map<@NlsSafe String, Set<UInt>>) {
  companion object {
    internal val EMPTY = ShaderUsages(emptyMap(), emptyMap())
  }

  internal fun getFilePaths() = sequence {
    yieldAll(entryPoints.keys)
    yieldAll(otherShaders.keys)
  }
}

data class CaptureInfo(
  val lifetime: Lifetime,
  val capture: RdcCapture,
  val file: VirtualFile,
  val shadersFileUsages: ShaderUsages = capture.rootActions.collectShaderFilesUsages()
)