package xyz.jdynb.tv.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAuthModel(
  val userId: String = "",
  val tokenName: String = "",
  val tokenValue: String = "",
  val expiredTime: Long = 0,
)