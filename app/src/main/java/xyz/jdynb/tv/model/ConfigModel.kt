package xyz.jdynb.tv.model

import kotlinx.serialization.Serializable

@Serializable
data class ConfigModel(
  var network: NetworkConfigModel,
) {
}

@Serializable
data class NetworkConfigModel(
  var baseUrl: String = "http://tv.jdynb.xyz"
)