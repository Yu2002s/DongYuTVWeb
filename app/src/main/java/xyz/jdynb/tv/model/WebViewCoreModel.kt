package xyz.jdynb.tv.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebViewCoreModel(
    @SerialName("id")
    var id: Int = 0,
    @SerialName("name")
    var name: String = "",
    @SerialName("type")
    var type: Int = 0,
    @SerialName("url")
    var url: String = "",
    @SerialName("abi")
    var abi: String = "",
    @SerialName("info")
    var info: String = "",
    @SerialName("minSdk")
    var minSdk: Int = 0,
    @SerialName("targetSdk")
    var targetSdk: Int = 0,
    @SerialName("status")
    var status: Int = 0,
    @SerialName("weight")
    var weight: Int = 0
)