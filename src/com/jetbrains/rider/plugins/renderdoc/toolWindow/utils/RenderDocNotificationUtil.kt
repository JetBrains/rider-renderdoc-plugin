package com.jetbrains.rider.plugins.renderdoc.toolWindow.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfoRt
import com.jetbrains.rider.plugins.renderdoc.RenderDocBundle

class RenderDocNotificationUtil {
  companion object {
    private val WINDOWS_FAILURE_CODES = setOf(
      0xc0000005.toInt(),   // Access Violation
      0xc0000135.toInt()    // DLL Not Found
    )

    fun notifyProcessStartFailure(project: Project, exitCode: Int) {
      if (SystemInfoRt.isWindows && exitCode in WINDOWS_FAILURE_CODES) {
        NotificationGroupManager.getInstance().getNotificationGroup("RenderDoc")
          .createNotification(RenderDocBundle.message("notification.processFailure.windows.message"), NotificationType.ERROR)
          .notify(project)
      }
    }
  }
}