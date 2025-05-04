package com.jetbrains.rider.plugins.renderdoc.toolWindow.model

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.util.ui.tree.AbstractTreeModel
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.RenderDocTreeFilter.Companion.FilterType
import com.jetbrains.rider.plugins.renderdoc.toolWindow.ui.ActionTreeNode
import com.jetbrains.rider.plugins.renderdoc.toolWindow.ui.CaptureRootTreeNode
import com.jetbrains.rider.plugins.renderdoc.toolWindow.ui.CaptureTreeNode
import javax.swing.tree.TreePath

internal class RdcCaptureTreeModel : AbstractTreeModel {
  val filter : RenderDocTreeFilter
  private val root : CaptureRootTreeNode

  constructor(project: Project, captureInfo: CaptureInfo) : super() {
    this.root = CaptureRootTreeNode(project, captureInfo)
    this.filter = RenderDocTreeFilter(FilterType.AllShaders, captureInfo.shadersFileUsages)
  }

  constructor(project: Project, captureInfo: CaptureInfo, filter: RenderDocTreeFilter) : super() {
    this.root = CaptureRootTreeNode(project, captureInfo)
    this.filter = filter
  }
  private fun getChildNodes(parent: Any?) = (parent as CaptureTreeNode<*>).childNodes.filter { filter.apply(it) }

  override fun getRoot(): Any = root

  override fun getChild(parent: Any?, index: Int): Any = getChildNodes(parent)[index]

  override fun getChildCount(parent: Any?): Int = getChildNodes(parent).size

  override fun isLeaf(node: Any?): Boolean = getChildNodes(node).isEmpty()

  override fun getIndexOfChild(parent: Any?, child: Any?): Int = getChildNodes(parent).indexOf(child as ActionTreeNode)

  internal fun getActionPathByEventId(id: UInt) : TreePath? {
    fun findNextNode(nodes: ArrayList<ActionTreeNode>): ActionTreeNode? {
      return nodes.firstOrNull {
        val action = it.value
        val lastDescendantId = it.getLastDescendantEventId() ?: action.eventId
        action.children.isNotEmpty() && id in action.eventId..lastDescendantId || action.eventId == id
      }
    }
    var node : AbstractTreeNode<out Any> = findNextNode(root.childNodes) ?: return null
    var height = 2
    while ((node as ActionTreeNode).value.eventId != id) {
      node = findNextNode(node.childNodes) ?: return null
      ++height
    }

    val nodes = arrayOfNulls<Any>(height)
    for (i in height - 1 downTo 0) {
      nodes[i] = node
      if (i != 0)
        node = node.parent
    }
    return TreePath(nodes)
  }
}