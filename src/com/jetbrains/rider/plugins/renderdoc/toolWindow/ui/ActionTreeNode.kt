package com.jetbrains.rider.plugins.renderdoc.toolWindow.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.renderdoc.rdClient.model.RdcAction
import com.jetbrains.rider.projectView.views.addAdditionalTextSeparator

internal class ActionTreeNode(project: Project, parent: AbstractTreeNode<out Any>, action: RdcAction) : CaptureTreeNode<RdcAction>(project, action, action.name) {
  init {
    myName = action.name
    setParent(parent)
  }

  override fun getActions(): Array<RdcAction> = value.children

  override fun createPresentation(): PresentationData {
    val presentation = super.createPresentation()
    update(presentation)
    return presentation
  }

  override fun update(presentation: PresentationData) {
    val lastId = getLastDescendantEventId()
    val idRange = "[${value.eventId}" + (if (lastId != null) "-$lastId" else "") + "]"
    presentation.addText(idRange, SimpleTextAttributes.GRAYED_ATTRIBUTES)
    presentation.addAdditionalTextSeparator()
    presentation.addText(myName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
  }

  fun getLastDescendantEventId() : UInt? {
    if (value.children.isEmpty())
      return null
    var node = value.children.last()
    while (node.children.isNotEmpty()) {
      node = node.children.last()
    }
    return node.eventId
  }
}
