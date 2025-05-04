package com.jetbrains.rider.plugins.renderdoc.vfs

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager.OptionallyIncluded
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem
import com.intellij.util.LocalTimeCounter
import com.jetbrains.renderdoc.rdClient.model.RdcCapture
import com.jetbrains.rider.plugins.renderdoc.debugger.RenderDocSourcesMapper
import com.jetbrains.rider.plugins.renderdoc.debugger.ShaderDebugUserDataKeys
import com.jetbrains.renderdoc.rdClient.model.RdcSourceFile
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

class RenderDocCaptureFileSystem : NewVirtualFileSystem(), NonPhysicalFileSystem, Disposable {
  companion object {
    const val PROTOCOL: String = "renderdoc"
  }

  private val files = mutableMapOf<String, VirtualFile>()

  override fun dispose() {
    synchronized(files) {
      files.clear()
    }
  }

  fun addSourceFile(rootDomain: String, sourceMapper: RenderDocSourcesMapper, sourceFile: RdcSourceFile, index: Int): VirtualFile {
    val sanitizedName = sourceFile.name.replace(":", "_")
    val path = "${rootDomain}_${sanitizedName}"
    val file = MyFile(this, Path(path), sourceFile.content)
    file.putUserData(ShaderDebugUserDataKeys.SHADER_SOURCE_FILE_INDEX, index)
    files[path] = file
    return file
  }

  internal fun createDrawCallsTreeFile(rootDomain: String, sourceMapper: RenderDocSourcesMapper, capture: RdcCapture) : MyFile {
    val path = "${rootDomain}_CaptureTree"
    val file = MyFile(this, Path(path), sourceMapper.buildAndRegisterCaptureFileContent(capture))
    files[path] = file
    return file
  }

  override fun getProtocol(): String = PROTOCOL

  override fun findFileByPath(path: String): VirtualFile? = synchronized(files) { files[path] }

  override fun refresh(asynchronous: Boolean) {
  }

  override fun refreshAndFindFileByPath(path: String): VirtualFile? = findFileByPath(path)

  override fun deleteFile(requestor: Any?, file: VirtualFile) {
    synchronized(files) {
      files.remove(file.path)
    }
  }

  override fun moveFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile) {
    TODO("Not yet implemented")
  }

  override fun renameFile(requestor: Any?, file: VirtualFile, newName: String) {
    TODO("Not yet implemented")
  }

  override fun createChildFile(requestor: Any?, parent: VirtualFile, file: String): VirtualFile {
    TODO("Not yet implemented")
  }

  override fun createChildDirectory(requestor: Any?, parent: VirtualFile, dir: String): VirtualFile {
    TODO("Not yet implemented")
  }

  override fun copyFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile {
    TODO("Not yet implemented")
  }

  override fun exists(file: VirtualFile): Boolean = synchronized(files) { files.contains(file.path) }

  override fun list(file: VirtualFile): Array<String> {
    TODO("Not yet implemented")
  }

  override fun isDirectory(file: VirtualFile): Boolean = false

  override fun getTimeStamp(file: VirtualFile): Long = file.timeStamp

  override fun setTimeStamp(file: VirtualFile, timeStamp: Long) {
    TODO("Not yet implemented")
  }

  override fun isWritable(file: VirtualFile): Boolean = file.isWritable

  override fun setWritable(file: VirtualFile, writableFlag: Boolean) {
    file.isWritable = writableFlag
  }

  override fun contentsToByteArray(file: VirtualFile): ByteArray = file.contentsToByteArray()

  override fun getInputStream(file: VirtualFile): InputStream = file.inputStream

  override fun getOutputStream(file: VirtualFile, requestor: Any?, modStamp: Long, timeStamp: Long): OutputStream = file.getOutputStream(requestor)

  override fun getLength(file: VirtualFile): Long = file.length

  override fun extractRootPath(normalizedPath: String): String {
    TODO("Not yet implemented")
  }

  override fun findFileByPathIfCached(path: String): VirtualFile? = findFileByPath(path)

  override fun getRank(): Int = 0

  override fun getAttributes(file: VirtualFile): FileAttributes? = null

  internal class RenderDocGeneratedFileType : FileType {
    override fun getName(): String = "RenderDoc Generated File"
    override fun getDescription(): String = RenderDocBundle.message("renderdoc.filetype.RenderDocGeneratedFile.name")
    override fun getDefaultExtension(): String = ""
    override fun getDisplayName() = RenderDocBundle.message("renderdoc.filetype.RenderDocGeneratedFile.name")
    override fun getIcon(): javax.swing.Icon? = null
    override fun isBinary(): Boolean = false
    override fun isReadOnly(): Boolean = true
  }

  internal class MyFile(private val fileSystem: RenderDocCaptureFileSystem, val path: Path, val content: String) : VirtualFile(), OptionallyIncluded {
    private val creationTimestamp = LocalTimeCounter.currentTime()

    override fun getFileType(): FileType = RenderDocGeneratedFileType()

    override fun getName(): String = path.name

    override fun getFileSystem(): VirtualFileSystem = fileSystem

    override fun getPath(): String = path.pathString

    override fun isWritable(): Boolean = false

    override fun isDirectory(): Boolean = false

    override fun isValid(): Boolean = true

    override fun getParent(): VirtualFile? = null

    override fun getChildren(): Array<VirtualFile>? = null

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
      TODO("Not yet implemented")
    }

    override fun contentsToByteArray(): ByteArray = content.toByteArray()

    override fun getModificationStamp(): Long = creationTimestamp

    override fun getTimeStamp(): Long = creationTimestamp

    override fun getLength(): Long = content.length.toLong()

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

    override fun getInputStream(): InputStream = content.byteInputStream()

    override fun isIncludedInEditorHistory(project: Project): Boolean  = false
  }
}
