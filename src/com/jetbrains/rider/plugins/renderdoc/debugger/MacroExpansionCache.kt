package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.framework.protocolOrThrow
import com.jetbrains.rd.ide.model.Solution
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.coroutines.asCoroutineDispatcher
import com.jetbrains.rider.model.renderdoc.frontendBackend.RdExpandedMacro
import com.jetbrains.rider.model.renderdoc.frontendBackend.renderdocFrontendBackendModel
import kotlinx.coroutines.withContext

internal class MacroExpansionCache(lifetime: LifetimeDefinition, solution: Solution) {
  private val lifetime = lifetime.createNested()
  private val renderdocFrontendBackendModel = solution.renderdocFrontendBackendModel
  private val scheduler = renderdocFrontendBackendModel.protocolOrThrow.scheduler

  private val cache = mutableMapOf<String, FileMacroExpansionInfo>()

  private fun tryGetUpToDate(file: VirtualFile): HashMap<Int, RdExpandedMacro>? {
    val existingEntry = synchronized(cache) { cache[file.path] }
    return existingEntry?.expansionsByStartOffset
  }

  private fun tryAdd(path: String, expansionsByStartOffset: HashMap<Int, RdExpandedMacro>) {
    return synchronized(cache) {
      cache[path] = FileMacroExpansionInfo(expansionsByStartOffset)
    }
  }

  suspend fun getOrCalculate(file: VirtualFile): HashMap<Int, RdExpandedMacro> {
    return tryGetUpToDate(file) ?: run {
      val value = withContext(scheduler.asCoroutineDispatcher) {
          renderdocFrontendBackendModel.openFileAndGetMacroCallExpansions.startSuspending(lifetime, file.path).associateByTo(hashMapOf()) { s -> s.startOffset }
        }
      tryAdd(file.path, value)
      value
    }
  }

  data class FileMacroExpansionInfo(val expansionsByStartOffset: HashMap<Int, RdExpandedMacro>)
}
