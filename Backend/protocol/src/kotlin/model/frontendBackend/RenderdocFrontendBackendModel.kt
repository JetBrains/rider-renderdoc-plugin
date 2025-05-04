package model.frontendBackend

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rider.model.nova.ide.SolutionModel
import com.jetbrains.rider.model.nova.ide.SolutionModel.RdDocumentId
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator

@Suppress("unused")
object RenderdocFrontendBackendModel : Ext(SolutionModel.Solution) {
  val rdExpandedMacro = structdef("rdExpandedMacro") {
    field("startOffset", int)
    field("endOffset", int)
    field("expansion", string)
  }

  init {
    setting(Kotlin11Generator.Namespace, "com.jetbrains.rider.model.renderdoc.frontendBackend")
    setting(CSharp50Generator.Namespace, "JetBrains.Rider.Plugins.Renderdoc.Model.FrontendBackend")

    call("openFileAndGetMacroCallExpansions", string, immutableList(rdExpandedMacro)).async
  }
}
