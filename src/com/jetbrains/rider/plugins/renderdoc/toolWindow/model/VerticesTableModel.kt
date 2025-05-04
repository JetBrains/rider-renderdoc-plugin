package com.jetbrains.rider.plugins.renderdoc.toolWindow.model

import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import javax.swing.table.AbstractTableModel

internal class VerticesTableModel(
  private val columnNames: List<String>,
  private val indices: List<UInt>,
  values: List<List<List<Float>>>
) : AbstractTableModel() {

  private val flatValues = values.map { it.flatten() }
  val columnStartIndices = values.first().let {
    it.fold(listOf(2)) { arr, el -> arr + listOf(el.size + arr.last()) }
  }.dropLast(1)

  override fun getRowCount(): Int = flatValues.size

  override fun getColumnCount(): Int = flatValues.first().size + 2

  override fun getColumnName(column: Int): String =
    when (column) {
      0 -> RenderDocBundle.message("renderdoc.tool.previewer.mesh.columns.index")
      1 -> RenderDocBundle.message("renderdoc.tool.previewer.mesh.columns.vertexIndex")
      else -> {
        val index = columnStartIndices.indexOf(column)
        if (index == -1) "" else columnNames[index]
      }
    }

  override fun getValueAt(rowIndex: Int, columnIndex: Int): Any =
    when (columnIndex) {
      0 -> rowIndex
      1 -> indices[rowIndex]
      else -> flatValues[rowIndex][columnIndex - 2]
    }

  override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
}