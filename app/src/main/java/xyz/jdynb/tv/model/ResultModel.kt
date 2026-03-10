package xyz.jdynb.tv.model

import kotlinx.serialization.Serializable

@Serializable
data class ResultModel<T>(
  val code: Int = 400,
  val msg: String = "",
  val data: T
)