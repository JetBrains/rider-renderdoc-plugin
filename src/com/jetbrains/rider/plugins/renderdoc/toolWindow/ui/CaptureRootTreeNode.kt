package com.jetbrains.rider.plugins.renderdoc.toolWindow.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.CaptureInfo
import com.jetbrains.renderdoc.rdClient.model.RdcAction

internal class CaptureRootTreeNode(project: Project, captureInfo: CaptureInfo) : CaptureTreeNode<CaptureInfo>(project, captureInfo, captureInfo.file.name) {
  override fun getActions(): Array<RdcAction> = value.capture.rootActions

  override fun update(presentation: PresentationData) {
    presentation.presentableText = name
    presentation.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
  }
}