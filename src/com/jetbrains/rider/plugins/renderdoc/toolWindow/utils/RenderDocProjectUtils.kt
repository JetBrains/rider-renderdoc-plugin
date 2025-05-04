package com.jetbrains.rider.plugins.renderdoc.toolWindow.utils

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rdclient.util.idea.toVirtualFile
import com.jetbrains.rider.projectView.solutionDirectory

// This file duplicates logic from the Unity plugin to avoid dependency on it.
val Project.projectDir: VirtualFile
  get() = this.solutionDirectory.toVirtualFile(true) ?: error("Virtual file not found for solution directory: ${this.solutionDirectory}")

private fun hasUnityFileStructure(project: Project): Boolean {
  return !project.isDefault && hasUnityFileStructure(project.projectDir)
}

private fun hasUnityFileStructure(projectDir: VirtualFile): Boolean {
  if (projectDir.findChild("Assets")?.isDirectory == false)
    return false
  val projectSettings = projectDir.findChild("ProjectSettings")
  if (projectSettings == null || !projectSettings.isDirectory)
    return false
  return projectSettings.children.any {
    it.name == "ProjectVersion.txt" || it.extension == "asset"
  }
}

private fun hasLibraryFolder(project: Project): Boolean {
  val projectDir = project.projectDir
  return projectDir.findChild("Library")?.isDirectory != false
}

internal fun Project.isUnityProject() : Boolean {
  return hasUnityFileStructure(this) && hasLibraryFolder(this)
}

internal fun isUnityPluginEnabled() : Boolean {
  return PluginManager.getInstance().findEnabledPlugin(PluginId.getId("com.intellij.resharper.unity")) != null
}