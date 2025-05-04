package com.jetbrains.rider.plugins.renderdoc.toolWindow.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.jetbrains.renderdoc.rdClient.model.RdcAction

internal abstract class CaptureTreeNode<T : Any>(project: Project, value: T, name: String) : AbstractTreeNode<T>(project, value) {
  init {
    myName = name
  }

  val childNodes by lazy {
    getActions().run { mapTo(ArrayList(size)) { ActionTreeNode(project, this@CaptureTreeNode, it) } }
  }

  protected abstract fun getActions(): Array<RdcAction>

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> = childNodes
}