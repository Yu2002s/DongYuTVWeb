package xyz.jdynb.tv.utils

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
import java.net.URLEncoder

object ParseUtils {

  /**
   * 获取解析规则
   */
  fun getParseRule(videoUrl: String) = NetworkUtils.requestSyncResult<ParseModel>(Api.GET_PARSE_RULE, mapOf("videoUrl" to videoUrl))

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

    val decryptJsonElement = NetworkUtils.json.parseToJsonElement(decrypt)

    return decryptJsonElement.jsonObject[decryptRule.videoUrl]?.jsonPrimitive?.contentOrNull
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
        .post(formBody)
        .build()
      val result = NetworkUtils.requestSyncResult<XMResolveResponse>(request, true).getOrThrow()

      val decrypt = AESUtils.decrypt(result.data, result.key, result.iv)
        ?: throw NullPointerException("decrypt is null")
      // Log.i(TAG, "decrypt: $decrypt")
      val json = decrypt.replace("tg:@xmflv", "")
        .replace("\u0003\u0003\u0003", "")
      // Log.i(TAG, "json: $json")
      val jsonObject = JSONObject(json)
      return jsonObject.getString("url")
    } catch (e: Exception) {
      return null
    }
  }

}