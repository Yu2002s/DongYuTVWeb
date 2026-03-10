package xyz.jdynb.tv.model.response

import kotlinx.serialization.Serializable

@Serializable
data class XMResolveResponse(
  val code: Int = 0,
  val key: String = "",
  val iv: String = "",
  val data: String = "",
)
