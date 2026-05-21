package xyz.jdynb.tv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelModel(
  @SerialName("id")
  var id: Int = 0,
  @SerialName("parentId")
  var parentId: Int? = null,
  @SerialName("channelType")
  var channelType: String = "",
  @SerialName("playerId")
  var playerId: Int = 0,
  @SerialName("logo")
  var logo: String = "",
  @SerialName("channelName")
  var channelName: String = "",
  @SerialName("description")
  var description: String = "",
  @SerialName("children")
  var children: List<Children> = listOf()
) {
  @Serializable
  data class Children(
    @SerialName("id")
    var id: Int = 0,
    @SerialName("parentId")
    var parentId: Int = 0,
    @SerialName("channelType")
    var channelType: String = "",
    @SerialName("playerId")
    var playerId: Int = 0,
    @SerialName("logo")
    var logo: String = "",
    @SerialName("channelName")
    var channelName: String = "",
    @SerialName("description")
    var description: String? = ""
  )
}