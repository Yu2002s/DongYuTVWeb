package xyz.jdynb.tv.model

import kotlinx.serialization.Serializable

@Serializable
data class PageModel<T>(
  val total: Int = 0,
  val data: List<T> = emptyList()
)
