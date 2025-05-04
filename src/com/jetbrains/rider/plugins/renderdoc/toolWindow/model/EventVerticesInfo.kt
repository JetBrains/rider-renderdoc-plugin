package com.jetbrains.rider.plugins.renderdoc.toolWindow.model

import com.jetbrains.renderdoc.rdClient.model.RdcVertexStageInOutputs


internal data class EventVerticesInfo(val eventId: Long?, val inOutputs: RdcVertexStageInOutputs?) {
  private val inputTableModel = inOutputs?.let { VerticesTableModel(it.input_columns, it.input_indices, it.inputs) }
  private val outputTableModel = inOutputs?.let { VerticesTableModel(it.output_columns, it.input_indices, it.outputs) }

  fun getTableModel(showInput: Boolean) = if (showInput) inputTableModel else outputTableModel
}