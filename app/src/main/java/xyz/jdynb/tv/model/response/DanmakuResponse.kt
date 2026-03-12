package xyz.jdynb.tv.model.response

import android.graphics.Color
import androidx.core.graphics.toColorInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import top.littlefogcat.easydanmaku.danmakus.DanmakuItem

@Serializable
data class DanmakuResponse(
  val code: Int,
  val name: String,
  val danum: Int,
  @Serializable(with = DanmakuListSerializer::class)
  val danmuku: List<Danmaku>
)

@Serializable
data class Danmaku(
  val time: Long,
  val position: String,
  val color: String,
  val fontSize: String,
  val text: String
) {

  fun toDanmakuItem(): DanmakuItem {
    val fullColor = if (color.length == 4) {
      color + buildString {
        repeat(3) {
          append(color.last())
        }
      }
    } else color
    val colorInt =  try {
      fullColor.toColorInt()
    } catch (_: Exception) {
      Color.WHITE
    }

    return DanmakuItem(text, time * 1000, top.littlefogcat.easydanmaku.danmakus.views.Danmaku.TYPE_RL, colorInt, 0)

    /*return TextData().also {
      it.text = text
      it.showAtTime = time
      it.textColor = colorInt
      it.layerType = LAYER_TYPE_SCROLL
    }*/
  }

}

// 自定义反序列化器
object DanmakuListSerializer : kotlinx.serialization.KSerializer<List<Danmaku>> {
  override val descriptor =
    kotlinx.serialization.descriptors.buildClassSerialDescriptor("DanmakuList")

  override fun deserialize(decoder: Decoder): List<Danmaku> {
    val jsonDecoder = decoder as? JsonDecoder
      ?: throw SerializationException("This serializer can be used only with Json format")

    val element = jsonDecoder.decodeJsonElement()
    val array = element as? JsonArray
      ?: throw SerializationException("Expected JsonArray")

    return array.map { element ->
      val list = (element as? JsonArray)
        ?.map { it.toString().trim('"') }
        ?: throw SerializationException("Expected inner JsonArray")

      Danmaku(
        time = list[0].toLong(),
        position = list[1],
        color = list[2],
        fontSize = list[3],
        text = list[4]
      )
    }
  }

  override fun serialize(encoder: Encoder, value: List<Danmaku>) {
    // 如果需要序列化，实现这个方法
  }
}

// 使用示例
fun main() {
  val jsonString = """
    {
      "code": 23,
      "name": "7b0d9cb3c155932a",
      "danum": 11274,
      "danmuku": [
        [
          2,
          "right",
          "#fff",
          "32",
          "🔥有 11274 条弹幕列队来袭~做好准备吧！🔥"
        ],
        [
          1,
          "right",
          "#22a7b0",
          "32px",
          "康恩奇：跟原来不一样"
        ]
      ]
    }
    """

  val json = Json { ignoreUnknownKeys = true }
  val response = json.decodeFromString<DanmakuResponse>(jsonString)

  println(response)
}