package com.jetbrains.rider.plugins.renderdoc.toolWindow.utils

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.ui.PopupHandler
import com.jetbrains.rider.plugins.renderdoc.toolWindow.ui.ActionTreeNode
import java.awt.Component
import javax.swing.JTree

class RenderDocPopupUtil {
  companion object {
    fun installPopupOnDrawCallNodes(tree: JTree, group: ActionGroup, place: String): PopupHandler {
      val handler = object : PopupHandler() {
        override fun invokePopup(comp: Component, x: Int, y: Int) {
          val path = tree.getPathForLocation(x, y) ?: return
          if ((path.lastPathComponent as? ActionTreeNode)?.value?.isDrawCall() != true)
            return

          val popupMenu = ActionManager.getInstance().createActionPopupMenu(place, group)
          popupMenu.getComponent().show(comp, x, y)
        }
      }
      tree.addMouseListener(handler)
      return handler
    }
  }
}