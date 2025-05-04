package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.intellij.openapi.rd.defineNestedLifetime
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.threading.coroutines.launch
import com.jetbrains.renderdoc.rdClient.model.RdcCapture
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocService
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocVertexInput
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.EventVerticesInfo
import com.jetbrains.rider.services.popups.nova.layouter.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.table.*

class RenderDocMeshPreviewPanel(private val service: RenderDocService) : RenderDocAbstractPreviewer() {
  private val lifetime = defineNestedLifetime()
  private val meshPreviewLifetime = SequentialLifetimes(lifetime)

  private val inputsRadioButton = JRadioButton(RenderDocBundle.message("renderdoc.tool.previewer.type.inputs"))
  private val outputsRadioButton = JRadioButton(RenderDocBundle.message("renderdoc.tool.previewer.type.outputs"))
  private val typeButtonsGroup = ButtonGroup()
  private val showInput: Boolean
    get() = inputsRadioButton.isSelected

  private val titlePanel = JPanel(BorderLayout())
  private val verticesTable = JBTable()
  private val scrollPane = ScrollPaneFactory.createScrollPane(verticesTable)

  private var eventVerticesInfo : EventVerticesInfo = EventVerticesInfo(null, null)

  init {
    initTitlePanel()
    initTable()
  }

  fun initTitlePanel() {
    val radioButtonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    typeButtonsGroup.apply {
      add(inputsRadioButton)
      add(outputsRadioButton)
    }
    outputsRadioButton.isSelected = true

    radioButtonPanel.add(inputsRadioButton)
    radioButtonPanel.add(outputsRadioButton)

    inputsRadioButton.addActionListener {
      if (inputsRadioButton.isSelected)
        updateVerticesTable(eventVerticesInfo.eventId ?: 0, getCurrentInput())
    }

    outputsRadioButton.addActionListener {
      if (outputsRadioButton.isSelected)
        updateVerticesTable(eventVerticesInfo.eventId ?: 0, getCurrentInput())
    }

    titlePanel.add(radioButtonPanel, BorderLayout.WEST)
    add(titlePanel, BorderLayout.NORTH)
  }

  fun initTable() {
    verticesTable.emptyText.text = RenderDocBundle.message("renderdoc.tool.previewer.default")
    verticesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    verticesTable.rowSelectionAllowed = true
    verticesTable.columnSelectionAllowed = false

    val resizeListener = object : ComponentAdapter() {
      override fun componentShown(e: ComponentEvent) {
        adjustComponentWidth()
      }

      override fun componentResized(e: ComponentEvent) {
        adjustComponentWidth()
      }

      private fun adjustComponentWidth() {
        val scrollPaneWidth = scrollPane.width
        val normalTableWidth = verticesTable.preferredSize.width

        if (normalTableWidth < scrollPaneWidth && verticesTable.width < scrollPaneWidth)
          verticesTable.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        else
          verticesTable.autoResizeMode = JTable.AUTO_RESIZE_OFF
      }
    }
    verticesTable.addComponentListener(resizeListener)
    scrollPane.addComponentListener(resizeListener)

    add(scrollPane, BorderLayout.CENTER)
  }

  override fun getCurrentInput(): RenderDocVertexInput = RenderDocVertexInput(verticesTable.selectedRow.takeIf { it != -1 }?.toUInt() ?: 0u)

  override fun getDefaultInput() : RenderDocDebugInput = RenderDocVertexInput.DEFAULT

  override fun syncPreviewWithEvent(capture: RdcCapture, eventId: Long, input: RenderDocDebugInput) {
    val lifetime = meshPreviewLifetime.next()
    lifetime.launch {
      if (eventVerticesInfo.eventId != eventId) {
        eventVerticesInfo = fetchVerticesInfo(lifetime, capture, eventId)
      }
      updateVerticesTable(eventId, input as RenderDocVertexInput)
    }
  }

  private fun updateVerticesTable(eventId: Long, input: RenderDocVertexInput) {
    val tableModel = eventVerticesInfo.getTableModel(showInput)
    if (tableModel == null) {
      resetToDefaultState(eventId)
      return
    }
    scrollPane.isVisible = false
    verticesTable.model = tableModel
    verticesTable.tableHeader = GroupedTableHeader(verticesTable.columnModel, tableModel.columnStartIndices)
    scrollPane.setColumnHeaderView(verticesTable.tableHeader)

    val cellRenderer = object : DefaultTableCellRenderer() {
      override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (!isSelected && !hasFocus) {
          component.background = JBColor.LIGHT_GRAY
        }
        return component
      }
    }

    verticesTable.columnModel.columns.asSequence().drop(2).forEachIndexed { i, col ->
      val idx = tableModel.columnStartIndices.indexOfLast { it <= i + 2 }
      if (idx != -1 && idx % 2 == 0)
        col.cellRenderer = cellRenderer
    }

    val rowSelection = input.index.toInt()
    if (rowSelection in 0..<verticesTable.rowCount) {
      verticesTable.setRowSelectionInterval(rowSelection, rowSelection)
      verticesTable.scrollRectToVisible(verticesTable.getCellRect(rowSelection, 0, true))
    }
    scrollPane.isVisible = true
  }

  override fun defaultPreview() {
    val lt = meshPreviewLifetime.next()
    lt.launch {
      resetToDefaultState(null)
    }
  }

  override fun dispose(): Unit = Unit

  private suspend fun fetchVerticesInfo(lifetime: Lifetime, capture: RdcCapture, eventId: Long) : EventVerticesInfo {
    val vs_info = service.getVertexStageInOutputs(lifetime, capture, eventId)
    return EventVerticesInfo(eventId, vs_info)
  }

  private fun resetToDefaultState(eventId : Long?) {
    scrollPane.isVisible = false
    verticesTable.model = DefaultTableModel()
    verticesTable.tableHeader = null
    scrollPane.setColumnHeaderView(null)
    scrollPane.isVisible = true
    eventVerticesInfo = EventVerticesInfo(eventId, null)
  }

  private class GroupedTableHeader(columnModel: TableColumnModel, private val columnStartIndices: List<Int>) : JTableHeader(columnModel) {
    init {
      reorderingAllowed = false
    }

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)

      for ((i, colStart) in columnStartIndices.withIndex()) {
        val endColumnIdx = (columnStartIndices.getOrNull(i + 1) ?: columnModel.columnCount) - 1
        val leftCell = getHeaderRect(colStart)
        val rightCell = getHeaderRect(endColumnIdx)

        val cellRect = Rectangle(leftCell.left + 1, leftCell.top + 1, rightCell.right - leftCell.left - 2, leftCell.height - 2)
        g.color = background
        g.fillRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height)

        g.color = foreground
        val name = columnModel.getColumn(colStart).headerValue as? String ?: return

        val label = JBLabel(name, SwingConstants.CENTER)
        label.background = background
        label.foreground = foreground
        label.bounds = cellRect

        val lg = g.create(cellRect.x + insets.left, cellRect.y, cellRect.width, cellRect.height) as Graphics2D
        label.paint(lg)
        lg.dispose()
      }
    }
  }
}