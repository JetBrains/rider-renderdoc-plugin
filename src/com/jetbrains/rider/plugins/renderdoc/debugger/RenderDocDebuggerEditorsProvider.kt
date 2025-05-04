package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider

class RenderDocDebuggerEditorsProvider : XDebuggerEditorsProvider() {
  override fun getFileType(): FileType {
    return UnknownFileType.INSTANCE
  }

  override fun createDocument(project: Project,
                              expression: XExpression,
                              sourcePosition: XSourcePosition?,
                              mode: EvaluationMode): Document {
    return LanguageTextField.SimpleDocumentCreator().createDocument(expression.expression, null, project)
  }
}
