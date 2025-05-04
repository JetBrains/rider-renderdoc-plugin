package com.jetbrains.rider.plugins.renderdoc

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

object RenderDocBundle {
  private const val BUNDLE: @NonNls String = "messages.RenderDocBundle"
  private val INSTANCE = DynamicBundle(RenderDocBundle::class.java, BUNDLE)

  @Nls
  fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): String = INSTANCE.getMessage(key, *params)
}
