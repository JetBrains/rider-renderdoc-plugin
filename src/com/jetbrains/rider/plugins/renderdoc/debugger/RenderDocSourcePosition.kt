package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.impl.XDebuggerUtilImpl
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter

class RenderDocSourcePosition : ExecutionPointHighlighter.HighlighterProvider, XSourcePosition {
  private val file: VirtualFile
  private val lineStart: Int
  private val offsetStart: Int
  private val offsetEnd: Int

  constructor(file: VirtualFile, lineStart: Int, offsetStart: Int, offsetEnd: Int): super() {
    this.file = file
    this.lineStart = lineStart
    this.offsetStart = offsetStart
    this.offsetEnd = offsetEnd
  }

  constructor(file: VirtualFile, frame: ContentFrame): super() {
    this.file = file
    this.lineStart = frame.lineStart
    this.offsetStart = frame.offsetStart
    this.offsetEnd = frame.offsetEnd
  }

  override fun getLine(): Int = lineStart

  override fun getOffset(): Int = offsetStart

  override fun getFile(): VirtualFile = file

  override fun createNavigatable(project: Project): Navigatable {
    return XDebuggerUtilImpl.createNavigatable(project, this)
  }

  override fun getHighlightRange(): TextRange = TextRange(offsetStart, offsetEnd)

  data class ContentFrame(val lineStart: Int, val offsetStart: Int, val offsetEnd: Int)
}