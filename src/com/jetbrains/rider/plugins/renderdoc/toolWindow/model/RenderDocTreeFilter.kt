package com.jetbrains.rider.plugins.renderdoc.toolWindow.model

import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.Project
import com.intellij.ui.filterField.FilterAllAction
import com.intellij.ui.filterField.FilterApplier
import com.intellij.ui.filterField.FilterItem
import com.intellij.ui.filterField.FilterMultiSelectAction
import com.intellij.ui.filterField.fillFilterItems
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.toolWindow.ui.ActionTreeNode
import org.jetbrains.annotations.Nls
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTree

internal class RenderDocTreeFilter(
  private val type: FilterType,
  private val shaderUsages: ShaderUsages,
  private val shaderPaths: Collection<String> = shaderUsages.getFilePaths().toList()) {

  companion object {
    enum class FilterType { All, AllShaders, ShaderSelection }

    private class FilterSourceFilesAction(private val attribute: String,
                                          private val applier: FilterApplier,
                                          private val values: List<String>) : AnAction(
      RenderDocBundle.message("renderdoc.tool.tree.filter.source.files")) {
      override fun actionPerformed(e: AnActionEvent) {
        applier.applyFilter(attribute, values)
      }
    }

    fun createFilterActions(
      filterField: Component, attribute: String, @Nls title: String,
      items: List<FilterItem>, selectedItems: Collection<String>, applier: (FilterType) -> FilterApplier
    ): Collection<AnAction> {
      val group = mutableListOf<AnAction>()
      group.add(FilterAllAction(attribute, applier(FilterType.All)))
      group.add(FilterSourceFilesAction(attribute, applier(FilterType.AllShaders), items.map { it.title }))
      group.add(FilterMultiSelectAction(attribute, title, filterField, items, selectedItems, applier(FilterType.ShaderSelection)))
      group.add(Separator())
      fillFilterItems(items, attribute, selectedItems, group, applier(FilterType.ShaderSelection))
      return group
    }

    fun filterApplierFactory(project: Project, captureInfo: CaptureInfo, tree: JTree, toolbar: JComponent): (FilterType) -> FilterApplier {
      return { type -> FilterApplier { attribute: String, values: Collection<String> ->
          for (filter in toolbar.components) {
            filter.revalidate()
          }
          toolbar.repaint()

          tree.model = RdcCaptureTreeModel(project, captureInfo, RenderDocTreeFilter(type, captureInfo.shadersFileUsages, values))
          tree.repaint()
          if (values.isNotEmpty())
            DefaultTreeExpander(tree).expandAll()
        }
      }
    }
  }

  fun isEmpty() : Boolean = type == FilterType.All

  fun getFilterValues() = shaderPaths

  fun isAllUserFilesFilter() : Boolean = type == FilterType.AllShaders

  fun apply(node: ActionTreeNode): Boolean {
    if (type == FilterType.All)
      return true
    val idRange = node.value.eventId..(node.getLastDescendantEventId() ?: node.value.eventId)
    for (path in shaderPaths) {
      var eventIds = shaderUsages.entryPoints[path]
      if (eventIds == null)
        eventIds = shaderUsages.otherShaders[path] ?: continue
      if (eventIds.any { it in idRange })
        return true
    }
    return false
  }
}