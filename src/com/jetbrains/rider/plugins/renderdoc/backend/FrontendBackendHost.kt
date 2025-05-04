package com.jetbrains.rider.plugins.renderdoc.backend

import com.intellij.openapi.client.ClientProjectSession
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.protocol.SolutionExtListener
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.WriteOnceProperty
import com.jetbrains.rider.model.renderdoc.frontendBackend.RenderdocFrontendBackendModel

@Service(Service.Level.PROJECT)
internal class FrontendBackendHost {
  private val _model = WriteOnceProperty<RenderdocFrontendBackendModel>()


  private fun setUpModel(model: RenderdocFrontendBackendModel) {
    _model.set(model)
  }

  class ProtocolListener : SolutionExtListener<RenderdocFrontendBackendModel> {
    override fun extensionCreated(lifetime: Lifetime, session: ClientProjectSession, model: RenderdocFrontendBackendModel) {
      session.project.service<FrontendBackendHost>().setUpModel(model)
    }
  }

  companion object {
    fun getInstance(project: Project): FrontendBackendHost = project.getService(FrontendBackendHost::class.java)
  }
}