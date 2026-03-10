package xyz.jdynb.tv.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoProgressModel(
  var currentIndex: Int = 0,
  var currentProgress: Long = 0
)
