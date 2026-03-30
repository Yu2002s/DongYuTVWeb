package xyz.jdynb.tv.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppFaqModel(
    @SerialName("id")
    var id: Int = 0,
    @SerialName("title")
    var title: String = "",
    @SerialName("content")
    var content: String = "",
    @SerialName("externalUrl")
    var externalUrl: String? = null,
    @SerialName("createTime")
    var createTime: String = "",
    @SerialName("updateTime")
    var updateTime: String = ""
)