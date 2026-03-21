package xyz.jdynb.tv.utils

import android.util.Log
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.model.ParseModel
import xyz.jdynb.tv.model.response.XMResolveResponse
import xyz.jdynb.tv.utils.EncryptUtils.md5
import java.net.URLDecoder
import java.net.URLEncoder

object ParseUtils {

  /**
   * 获取解析规则
   */
  fun getParseRule(videoUrl: String) =
    NetworkUtils.requestSyncResult<ParseModel>(Api.GET_PARSE_RULE, mapOf("videoUrl" to videoUrl))

  @Throws(Exception::class)
  fun parseVideo(url: String): String? {
    val parseModel = getParseRule(url).getOrThrow()
    val requestUrl = parseModel.url
    val fromBodyBuilder = FormBody.Builder()
    parseModel.params.entries.forEach { (key, value) ->
      fromBodyBuilder.add(key, value)
    }
    val request = Request.Builder()
      .url(requestUrl)
      .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
      .addHeader("Origin", "https://jx.xmflv.cc")
      .post(fromBodyBuilder.build())
      .build()

    val result = NetworkUtils.requestSyncResult<String>(request, true).getOrThrow()

    val jsonElement = NetworkUtils.json.parseToJsonElement(result)
    val jsonObject = jsonElement.jsonObject
    val decryptRule = parseModel.decryptRule
    val data = jsonObject[decryptRule.dataSource]?.jsonPrimitive?.contentOrNull ?: return null
    val key = jsonObject[decryptRule.keySource]?.jsonPrimitive?.contentOrNull ?: return null
    val iv = jsonObject[decryptRule.ivSource]?.jsonPrimitive?.contentOrNull ?: return null

    var decrypt = AESUtils.decrypt(data, key, iv, decryptRule.algorithm) ?: return null

    decryptRule.removeStr.forEach { decrypt = decrypt.replace(it, "") }

    // 移除 BOM 头
    decrypt = decrypt.replace("\uFEFF", "")
      // 移除零宽空格和零宽连接符
      .replace("[\\u200B-\\u200D\\uFEFF]".toRegex(), "")
      // 只保留有效的 JSON 字符
      .filter { it.isDefined() && it.code >= 32 }
      .trim()
    Log.i("ParseUtils", "decrypt: $decrypt")

    val decryptJsonElement = NetworkUtils.json.parseToJsonElement(decrypt)

    val videoUrl = decryptJsonElement.jsonObject[decryptRule.videoUrl]?.jsonPrimitive?.contentOrNull

    Log.i("ParseUtils", "videoUrl: $videoUrl")

    return videoUrl
  }

  fun parseVideoLocal(url: String): String? {
    try {
      val urlEncoded = URLEncoder.encode(url, "UTF-8")
      val now = System.currentTimeMillis()
      val key = "$now$urlEncoded".md5()

      // Log.i(TAG, "key: $key")
      val sign = AESUtils.encrypt(key, key.md5(), "fUU9eRmkYzsgbkEK") ?: ""
      val formBody = FormBody.Builder()
        .add("tm", now.toString())
        .add("url", urlEncoded)
        .add("key", key)
        .add("sign", sign)
        .build()
      val request = Request.Builder()
        .url("https://202.189.8.170/Api")
        .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .addHeader("Origin", "https://jx.xmflv.cc")
        .post(formBody)
        .build()
      val result = NetworkUtils.requestSyncResult<XMResolveResponse>(request, true).getOrThrow()

      val decrypt = AESUtils.decrypt(result.data, result.key, result.iv)
        ?: throw NullPointerException("decrypt is null")
      // Log.i(TAG, "decrypt: $decrypt")
      val json = decrypt.replace("tg:@xmflv", "")
        .replace("\u0003\u0003\u0003", "")
        // 移除 BOM 头
        .replace("\uFEFF", "")
        // 移除零宽空格和零宽连接符
        .replace("[\\u200B-\\u200D\\uFEFF]".toRegex(), "")
        // 只保留有效的 JSON 字符
        .filter { it.isDefined() && it.code >= 32 }
        .trim()
      // Log.i(TAG, "json: $json")
      val jsonObject = JSONObject(json)
      return jsonObject.getString("url")
    } catch (e: Exception) {
      return null
    }
  }

}