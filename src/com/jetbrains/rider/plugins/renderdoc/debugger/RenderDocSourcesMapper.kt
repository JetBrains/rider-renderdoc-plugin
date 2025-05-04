package com.jetbrains.rider.plugins.renderdoc.debugger

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rdclient.util.idea.toVirtualFile
import com.jetbrains.renderdoc.rdClient.model.RdcAction
import com.jetbrains.renderdoc.rdClient.model.RdcCapture
import com.jetbrains.renderdoc.rdClient.model.RdcDebugStack
import com.jetbrains.rider.model.renderdoc.frontendBackend.RdExpandedMacro
import com.jetbrains.rider.projectView.solution
import kotlin.math.floor
import kotlin.math.log10

class RenderDocSourcesMapper(project: Project) {
  private val lifetime = LifetimeDefinition()
  private val definedSymbolsCache = MacroExpansionCache(lifetime, project.solution)

  private var baseSearchPath = project.basePath

  private val captureFileLineMapping = hashMapOf<UInt, RenderDocSourcePosition.ContentFrame>() // <eventId : position>

  companion object {
    private val SOURCE_REFERENCE_LINE_REGEX = Regex("""//#line (\d+)(?: "([^"]*)")?""")
  }

  suspend fun mapDecompiledToSourcePosition(decompiled: VirtualFile, frame: RdcDebugStack): RenderDocSourcePosition {
    val lines = String(decompiled.contentsToByteArray()).lines()
    val decompiledLineStart = frame.lineStart.toInt() - 1
    val decompiledLineEnd = frame.lineEnd.toInt() - 1
    if (frame.sourceFileIndex == -1) {
      val startOffset = lines.slice(0..<decompiledLineStart).sumOf { it.length + 1 }
      return RenderDocSourcePosition(decompiled, decompiledLineStart, startOffset, startOffset + lines[decompiledLineEnd].length + 1)
    }

    var sourceLineStart = -1
    var sourceLineEnd = -1

    for (i in decompiledLineEnd - 1 downTo 0) {
      val (lineIdx, filePath) = collectSnippetParts(lines[i]) ?: continue

      if (sourceLineEnd == -1) {
        sourceLineEnd = decompiledLineEnd - 1 - i + lineIdx
      }
      if (sourceLineStart == -1 && decompiledLineStart > i) {
        sourceLineStart = decompiledLineStart - 1 - i + lineIdx
      }
      if (filePath == null) continue
      val virtualFile = tryResolveFile(filePath) ?: break
      val document = readAction { virtualFile.findDocument() } ?: break

      val sourceLines = document.text.lines()
      var sourceStartOffset = sourceLines.slice(0..<sourceLineStart - 1).sumOf { it.length + 1 }
      val sourceEndOffset = sourceLines.slice(0..<sourceLineEnd - 1).sumOf { it.length + 1 }

      val (sourceColumnStart, sourceColumnEnd) = mapDecompiledToSourceColumns(
        decompiledLines = listOf(lines[decompiledLineStart], lines[decompiledLineEnd]),
        destPositions = listOf(frame.columnStart.toInt() - 1, frame.columnEnd.toInt() - 1),
        sourceLines = listOf(sourceLines[sourceLineStart - 1], sourceLines[sourceLineEnd - 1]),
        sourcePath = virtualFile.path, offsets = listOf(sourceStartOffset, sourceEndOffset))
      sourceStartOffset += sourceColumnStart.toInt() - 1

      val sourceLineEndOffset = sourceEndOffset + sourceColumnEnd.toInt()
      return RenderDocSourcePosition(virtualFile, sourceLineStart, sourceStartOffset, sourceLineEndOffset)
    }

    val decompiledLineStartOffset = lines.slice(0..<decompiledLineStart).sumOf { it.length + 1 }
    val decompiledLineEndOffset = lines.slice(0..<decompiledLineEnd).sumOf { it.length + 1 }

    if (frame.columnStart == 0u && frame.columnEnd == 0u)
      return RenderDocSourcePosition(decompiled, frame.lineStart.toInt(), decompiledLineStartOffset, decompiledLineEndOffset + lines[decompiledLineEnd].length + 1)

    return RenderDocSourcePosition(decompiled, frame.lineStart.toInt(), decompiledLineStartOffset + frame.columnStart.toInt() - 1, decompiledLineEndOffset + frame.columnEnd.toInt())
  }

  fun collectSnippetInfo(line: String) : Pair<Int, VirtualFile?>? {
    val parts = collectSnippetParts(line) ?: return null
    val filePath = parts.second ?: return Pair(parts.first, null)
    return Pair(parts.first, tryResolveFile(filePath))
  }

  private fun tryResolveFile(filePath: String) : VirtualFile? {
    var virtualFile = filePath.toVirtualFile(true)

    if (virtualFile == null) {
      if (baseSearchPath == null) return null
      virtualFile = ("${baseSearchPath}/$filePath").toVirtualFile(true)
    }
    return virtualFile
  }

  private fun collectSnippetParts(line: String) : Pair<Int, String?>? {
    val matchResult = SOURCE_REFERENCE_LINE_REGEX.find(line) ?: return null

    if (matchResult.range.last != line.lastIndex) return null
    val lineIdx = matchResult.groupValues[1].toInt()

    val filePath = matchResult.groupValues[2]
    if (filePath.isNotEmpty()) {
      return Pair(lineIdx, filePath)
    }
    return Pair(lineIdx, null)
  }

  private fun RdcAction.dumpContent(padding: Int, hasNext: Boolean, startLine: Int, startOffset: Int, prefix: String = ""): Pair<Int, String> {
    var lines = 1
    val content = buildString {
      append(prefix)
      append(if (hasNext) "+-- " else "\\-- ")

      val id = eventId.toString()
      if (children.isEmpty())
        captureFileLineMapping[eventId] = RenderDocSourcePosition.ContentFrame(startLine + lines, startOffset + this.length + padding - id.length, startOffset + this.length + padding + 2 + name.length)

      append(id.padStart(padding, ' '))
      append(": ")
      append(name)
      append("\n")

      if (children.isEmpty())
        return@buildString

      val childPrefix = if (hasNext) "$prefix|" + " ".repeat(3 + padding - id.length) else prefix + " ".repeat(4 + padding - id.length)
      children.dropLast(1).forEach {
        val content = it.dumpContent(padding, true, startLine + lines, startOffset + this.length, childPrefix)
        lines += content.first
        append(content.second)
      }
      val content = children.last().dumpContent(padding, false, startLine + lines, startOffset + this.length, childPrefix)
      lines += content.first
      append(content.second)
    }
    return Pair(lines, content)
  }


  fun buildAndRegisterCaptureFileContent(capture: RdcCapture) : String {
    return buildString {
      val padding = floor(log10(capture.rootActions.last().eventId.toDouble())).toInt() + 1
      var curLine = 1
      capture.rootActions.dropLast(1).forEach {
        val content = it.dumpContent(padding, true, curLine, this.length)
        curLine += content.first
        append(content.second)
      }
      val content = capture.rootActions.last().dumpContent(padding, false, curLine, this.length)
      curLine += content.first
      append(content.second)
    }
  }

  fun getCaptureTreeFilePosition(eventId: UInt) : RenderDocSourcePosition.ContentFrame {
    return captureFileLineMapping[eventId]!!
  }

  /**
   * Maps position in preprocessed file to position in source file.
   * Returns:
   * column index in source file if mapping is possible.
   * -1 if mapping is not possible (e.g. source code was changed after snapshot was taken).
   */
  private fun mapPreprocessedToSourceLinePosition(
    preprocessedPos: Int,
    preprocessed: String,
    source: String,
    startOffset: Int,
    macroExpansionsInFile: HashMap<Int, RdExpandedMacro>,
    isStartPosition: Boolean
  ) : Int {
    var preprocessedIdx = 0
    var sourceIdx = 0
    var resultPosition = -1


    fun skipWhitespacesPreprocessed() {
      while (preprocessedIdx < preprocessed.length && preprocessed[preprocessedIdx].isWhitespace())
        ++preprocessedIdx
    }

    while (preprocessedIdx < preprocessed.length && sourceIdx < source.length) {
      skipWhitespacesPreprocessed()
      while (sourceIdx < source.length && source[sourceIdx].isWhitespace()) ++sourceIdx
      if (preprocessedIdx >= preprocessed.length || sourceIdx >= source.length) break

      val macro = macroExpansionsInFile[startOffset + sourceIdx]
      if (macro == null) {
        if (preprocessed[preprocessedIdx] == source[sourceIdx]) {
          if (preprocessedIdx == preprocessedPos)
            resultPosition = sourceIdx + 1
          ++preprocessedIdx
          ++sourceIdx
          continue
        }
        return -1
      }

      val (_, endOffset, expansion) = macro

      for (c in expansion) {
        if (preprocessedIdx == preprocessedPos)
          resultPosition = if (isStartPosition) sourceIdx + 1 else endOffset - startOffset

        if (c.isWhitespace())
          continue
        skipWhitespacesPreprocessed()
        if (preprocessedIdx == preprocessedPos)
          resultPosition = if (isStartPosition) sourceIdx + 1 else endOffset - startOffset

        if (preprocessedIdx >= preprocessed.length || preprocessed[preprocessedIdx] != c)
          return -1
        ++preprocessedIdx
      }

      sourceIdx = endOffset - startOffset
    }

    return resultPosition
  }

  private suspend fun mapDecompiledToSourceColumns(decompiledLines: List<String>, destPositions: List<Int>, sourceLines: List<String>, sourcePath: String, offsets: List<Int>): Pair<UInt, UInt> {
    if (destPositions[0] == -1 && destPositions[1] == -1)
      return Pair(0u, sourceLines.last().length.toUInt())

    val file = sourcePath.toVirtualFile(true)
    val expandedMacros = if (file != null)
      definedSymbolsCache.getOrCalculate(file)
    else
      hashMapOf()

    var startColumn = mapPreprocessedToSourceLinePosition(destPositions[0], decompiledLines[0], sourceLines[0], offsets[0], expandedMacros, true)
    var endColumn = mapPreprocessedToSourceLinePosition(destPositions[1], decompiledLines[1], sourceLines[1], offsets[1], expandedMacros, false)
    if (startColumn == -1)
      startColumn = 0
    if (endColumn == -1)
      endColumn = sourceLines.last().length + 1
    return Pair(startColumn.toUInt(), endColumn.toUInt())
  }
}