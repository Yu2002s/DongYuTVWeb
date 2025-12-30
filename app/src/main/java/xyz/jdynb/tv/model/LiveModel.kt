package xyz.jdynb.tv.model

import kotlinx.serialization.Serializable

@Serializable
data class LiveModel(
  val player: List<Player> = listOf(),
  val channel: List<LiveChannelTypeModel> = listOf()
) {

  @Serializable
  data class Player(
    val id: String = "",
    val name: String = "",
    val url: String? = null,
    val script: Script = Script(),
    val exclude: Exclude? = Exclude(),
  ) {

    @Serializable
    data class Exclude(
      val url: List<String>? = listOf(),
      val suffix: List<String>? = listOf()
    )

    @Serializable
    data class Script(
      val async: Boolean = true,
      val init: List<String> = listOf(),
      val play: List<String> = listOf(),
      val resumePause: List<String> = listOf()
    )
  }
}