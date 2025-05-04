package com.jetbrains.rider.plugins.renderdoc.toolWindow

import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileEditor.impl.createSplitter
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.defineNestedLifetime
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.filterField.FilterField
import com.intellij.ui.filterField.FilterFieldAction
import com.intellij.ui.filterField.FilterItem
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocService
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.CaptureInfo
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.RdcCaptureTreeModel
import com.jetbrains.rider.plugins.renderdoc.toolWindow.ui.ActionTreeNode
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.threading.coroutines.launch
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import com.jetbrains.rider.plugins.renderdoc.core.RenderDocDebugInput
import com.jetbrains.rider.plugins.renderdoc.toolWindow.model.RenderDocTreeFilter
import com.jetbrains.rider.plugins.renderdoc.toolWindow.ui.CaptureRootTreeNode
import com.jetbrains.rider.plugins.renderdoc.toolWindow.ui.RenderDocActionTree
import com.jetbrains.rider.plugins.renderdoc.toolWindow.utils.isDrawCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.util.UUID
import javax.swing.JComponent
import javax.swing.JPanel

class RenderDocTool(val project: Project, private val service: RenderDocService) : JPanel(BorderLayout()), Disposable, UiDataProvider {
  private val lifetime = defineNestedLifetime()
  private val captureLifetimes = SequentialLifetimes(lifetime)

  private lateinit var filterToolbar : JComponent
  private val actionTree = RenderDocActionTree()
  private var captureInfo: CaptureInfo? = null

  private val previewerPane = RenderDocPreviewerTabbedPane(service)
  private val splitter : Splitter

  init {
    val upperPanel = initTreeActionsPanel()
    initPreviewer()
    splitter = setupSplitter(upperPanel)
  }

  private fun setupSplitter(upperPanel: JPanel) : Splitter {
    val splitter = createSplitter(true, 0.5f, 0.3f, 0.8f)
    splitter.firstComponent = upperPanel
    splitter.secondComponent = previewerPane
    splitter.isVisible = false
    add(splitter, BorderLayout.CENTER)
    return splitter
  }

  private fun initTreeActionsPanel() : JPanel {
    val actionManager = ActionManager.getInstance()
    filterToolbar = initFiltersToolbar()
    val toolbar = actionManager.createActionToolbar("RenderDocToolbar", actionManager.getAction("RenderDocActions.Toolbar") as ActionGroup, true)
    toolbar.targetComponent = this
    toolbar.setReservePlaceAutoPopupIcon(false)

    val toolbarWrapper = BorderLayoutPanel().apply {
      border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
      addToRight(filterToolbar)
      addToLeft(toolbar.component)
    }
    add(toolbarWrapper, BorderLayout.NORTH)
    val treeScrollPane = ScrollPaneFactory.createScrollPane(actionTree)
    return BorderLayoutPanel().apply {
      addToTop(toolbarWrapper)
      addToCenter(treeScrollPane)
    }
  }

  private fun initFiltersToolbar() : JComponent {
    val actionGroup = DefaultActionGroup()
    actionGroup.add(FilterFieldAction(::createShaderFilter))

    val toolbar = ActionManager.getInstance().createActionToolbar(RenderDocBundle.message("renderdoc.tool.title"), actionGroup, true)
    toolbar.targetComponent = this

    return toolbar.component
  }

  private fun createShaderFilter() : FilterField {
    return object : FilterField(RenderDocBundle.message("renderdoc.tool.tree.filter.shader")) {
      override fun buildActions(): Collection<AnAction> {
        val captureInfo = captureInfo ?: return emptyList()
        val model = actionTree.model as? RdcCaptureTreeModel ?: return emptyList()
        val allShaders = captureInfo.shadersFileUsages.getFilePaths().mapTo(mutableListOf()) { FilterItem(it, it) }
        val currentFilter = model.filter.getFilterValues()
        val applierFactory = RenderDocTreeFilter.filterApplierFactory(project, captureInfo, actionTree, filterToolbar)
        return RenderDocTreeFilter.createFilterActions(this, "Shader", RenderDocBundle.message("renderdoc.tool.tree.filter.select.shader"), allShaders, currentFilter, applierFactory)
      }

      override fun getCurrentText(): String? {
        val model = actionTree.model as? RdcCaptureTreeModel
        if (model == null || model.filter.isEmpty())
          return null
        if (model.filter.isAllUserFilesFilter())
          return StringUtil.shortenTextWithEllipsis(RenderDocBundle.message("renderdoc.tool.tree.filter.source.files"), 30, 0, true)
        val text = model.filter.getFilterValues().joinToString()
        return StringUtil.shortenTextWithEllipsis(text, 30, 0, true)
      }
    }
  }

  private fun initPreviewer() {
    previewerPane.isVisible = false
    previewerPane.addChangeListener {
      syncSelectedEventId(previewerPane.eventId, previewerPane.getCurrentInput())
    }
  }

  internal fun syncSelectedEventId(eventId: Long?, input: RenderDocDebugInput = previewerPane.getCurrentInput()) {
    captureInfo?.let { info ->
      info.lifetime.launch { previewerPane.syncWithEventId(info.capture, eventId, input) }
    }
  }

  internal fun syncWithEventIdInDebug(sessionId: UUID, eventId: UInt?, input: RenderDocDebugInput) {
    captureInfo?.let { info ->
      info.lifetime.launch {
        if (eventId != null) {
          // If we have an event ID, highlight it and sync the previewer
          actionTree.highlightActiveEvent(sessionId, eventId)
          previewerPane.syncWithEventId(info.capture, eventId.toLong(), input)
          return@launch
        }

        // Otherwise, use the last selection
        actionTree.setToLastSelection(sessionId)
        val lastSelectedEventId = when (val selectedNode = actionTree.selectionPath?.lastPathComponent) {
          is ActionTreeNode -> selectedNode.value.eventId.toLong()
          is CaptureRootTreeNode -> -1
          else -> null
        }
        previewerPane.syncWithEventId(info.capture, lastSelectedEventId, input)
      }
    }
  }

  internal fun openCapture() {
    val chooser = FileChooserFactory.getInstance().createFileChooser(FileChooserDescriptorFactory.createSingleFileDescriptor("rdc"),
                                                                     project, null)
    val file = chooser.choose(project).getOrNull(0) ?: return
    val lifetime = captureLifetimes.next()
    lifetime.launch {
      val rdcCapture = service.openCapture(lifetime, file)
      captureInfo = CaptureInfo(lifetime, rdcCapture, file).also { captureInfo ->
        withContext(Dispatchers.EDT) {
          previewerPane.defaultPreview()
          actionTree.model = RdcCaptureTreeModel(project, captureInfo)
          for (filter in filterToolbar.components) {
            filter.revalidate()
          }
          filterToolbar.repaint()
          if (actionTree.model.getChildCount(actionTree.model.root) > 0)
            DefaultTreeExpander(actionTree).expandAll()
          splitter.isVisible = true
        }
      }
    }
  }

  override fun dispose(): Unit = Disposer.dispose(previewerPane)

  override fun uiDataSnapshot(sink: DataSink) {
    sink[RenderDocDataKeys.RENDERDOC_TOOL] = this
    sink[RenderDocDataKeys.RENDERDOC_CAPTURE_INFO] = captureInfo
    sink[RenderDocDataKeys.DRAW_CALL_ID] = (actionTree.lastSelectedPathComponent as? ActionTreeNode)?.value?.takeIf { it.isDrawCall() }?.eventId
    sink[RenderDocDataKeys.RENDERDOC_PIXEL_DEBUG_INPUT] = previewerPane.getPixelInput()
    sink[RenderDocDataKeys.RENDERDOC_VERTEX_DEBUG_INPUT] = previewerPane.getVertexInput()
  }
}
