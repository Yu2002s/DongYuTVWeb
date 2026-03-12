package xyz.jdynb.tv.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParseModel(
    @SerialName("url")
    var url: String = "",
    @SerialName("params")
    var params: Map<String, String> = mapOf(),
    @SerialName("requestTemplate")
    var requestTemplate: String = "",
    @SerialName("decryptRule")
    var decryptRule: DecryptRule = DecryptRule()
) {

    @Serializable
    data class DecryptRule(
        @SerialName("algorithm")
        var algorithm: String = "",
        @SerialName("dataSource")
        var dataSource: String = "",
        @SerialName("keySource")
        var keySource: String = "",
        @SerialName("ivSource")
        var ivSource: String = "",
        @SerialName("removeStr")
        var removeStr: List<String> = listOf(),
        @SerialName("videoUrl")
        @Serializable
        var videoUrl: String = "url"
    )
}