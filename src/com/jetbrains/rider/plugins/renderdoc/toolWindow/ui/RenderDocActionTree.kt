package com.jetbrains.rider.plugins.renderdoc.toolWindow.ui

import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.ui.treeStructure.Tree
import com.jetbrains.rider.plugins.renderdoc.toolWindow.RenderDocTool
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.RdcCaptureTreeModel
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.RenderDocPopupUtil
import icons.RiderIcons
import java.util.*
import javax.swing.JTree
import javax.swing.ListSelectionModel
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.TreePath
import kotlin.collections.HashMap

class RenderDocActionTree : Tree() {
  private val lastSelectionPaths: HashMap<UUID, TreePath> = hashMapOf()

  init {
    selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    cellRenderer = NodeRenderer()

    RenderDocPopupUtil.installPopupOnDrawCallNodes(
      this, ActionManager.getInstance().getAction("RenderDocActions.Toolbar") as ActionGroup, "RenderDocTool")

    addTreeSelectionListener {
      onTreeNodeSelected(it)
    }
  }

  internal fun highlightActiveEvent(sessionId: UUID, eventId: UInt) {
    val model = model as RdcCaptureTreeModel
    val path = model.getActionPathByEventId(eventId)
    invokeAndWaitIfNeeded {
      selectionPath?.let {
        if (lastSelectionPaths[sessionId] == null) {
          lastSelectionPaths[sessionId] = it
        }
        clearSelection()
      }
      cellRenderer = object : NodeRenderer() {
        override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
          super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus)
          val node = value as? ActionTreeNode ?: return
          if (leaf && node.value.eventId == eventId) {
            icon = RiderIcons.Debugger.FollowupStatement
          }
        }
      }
      updateUI()
      if (path != null)
        scrollPathToVisible(path)
    }
  }

  internal fun setToLastSelection(sessionId: UUID) {
    invokeAndWaitIfNeeded {
      lastSelectionPaths[sessionId]?.let {
        selectionPath = it
        lastSelectionPaths.remove(sessionId)
      }
      cellRenderer = NodeRenderer()
      updateUI()
      scrollPathToVisible(selectionPath)
    }
  }

  private fun onTreeNodeSelected(event: TreeSelectionEvent) {
    val node = event.newLeadSelectionPath?.lastPathComponent
    val toolWindow = getToolWindow() ?: return
    toolWindow.syncSelectedEventId(
      when (node) {
        is ActionTreeNode -> node.value.eventId.toLong()
        is CaptureRootTreeNode -> -1
        else -> return
      })
  }

  private fun getToolWindow(): RenderDocTool? {
    var parent = parent
    while (parent != null && parent !is RenderDocTool) {
      parent = parent.parent
    }
    return parent as? RenderDocTool?
  }
}