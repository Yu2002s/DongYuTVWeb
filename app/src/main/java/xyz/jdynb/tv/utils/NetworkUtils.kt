package xyz.jdynb.tv.utils

import android.util.Log
import com.drake.engine.utils.EncryptUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.DongYuTVApplication
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.model.ConfigModel
import xyz.jdynb.tv.model.ResultModel
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.coroutines.suspendCoroutine

object NetworkUtils {

  val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private var baseUrl = ""

  private const val CONFIG_URL = "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/config.json"

  val okHttpClient = OkHttpClient.Builder()
    .build()

  fun getBaseUrl(): String {
    if (BuildConfig.DEBUG) {
      return Api.BASE_URL
    }
    if (baseUrl.isEmpty()) {
      try {
        val responseBody = getResponseBody(CONFIG_URL) ?: return Api.BASE_URL
        val configModel = json.decodeFromString<ConfigModel>(responseBody)
        return configModel.network.baseUrl.ifEmpty { Api.BASE_URL }
      } catch (_: Exception) {
        return Api.BASE_URL
      }
    }
    return baseUrl
  }

  fun String.inputStream(
    method: String = "GET",
    headers: Map<String, String>? = null,
    body: String? = null
  ): InputStream? {
    return try {
      createConnection(this, method).apply {
        headers?.let {
          for ((key, value) in headers) {
            setRequestProperty(key, value)
          }
        }
        body?.let {
          doOutput = true
          outputStream.use {
            it.write(body.toByteArray())
          }
        }
      }.inputStream
    } catch (_: IOException) {
      null
    }
  }

  @Throws(IOException::class)
  fun createConnection(url: String, method: String = "GET"): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 6000
    connection.readTimeout = 6000
    connection.requestMethod = method
    connection.setRequestProperty("Accept", "*/*")
    return connection
  }

  fun getResponseBody(url: String): String? {
    return try {
      val connection = createConnection(url)
      connection.inputStream.use { inputStream ->
        inputStream.readBytes().toString(StandardCharsets.UTF_8)
      }
    } catch (_: Exception) {
      null
    }
  }

  fun getResponseBodyCache(url: String, assetFileName: String? = null): String {
    // 调试模式下，优先从assets目录读取
    if (BuildConfig.DEBUG && assetFileName != null) {
      return DongYuTVApplication.context.assets.open(assetFileName).use {
        it.readBytes().toString(StandardCharsets.UTF_8)
      }
    }
    val md5 = EncryptUtil.encryptMD5ToString(url)
    val file = File(DongYuTVApplication.context.filesDir, md5)

    var content = getResponseBody(url)?.also {
      file.writeText(it)
    }

    if (content.isNullOrEmpty()) {
      content = file.readText()
    }

    if (content.isEmpty() && assetFileName != null) {
      content = DongYuTVApplication.context.assets.open(assetFileName).use {
        it.readBytes().toString(StandardCharsets.UTF_8)
      }
    }

    return content
  }

  suspend inline fun <reified T> request(
    path: String,
    params: Map<String, String>? = null,
  ): Result<T> {
    Log.i("NetworkUtils", "BASE_URL: ${Api.BASE_URL} request: $path, params: $params")
    return runCatching {
      val result = withContext(Dispatchers.IO) {
        val url = getBaseUrl() + path + "?${params?.toQueryString()}"
        val responseBody = getResponseBody(url)
        requireNotNull(responseBody) { "Response body is null" }
        json.decodeFromString(Json.serializersModule.serializer<ResultModel<T>>(), responseBody)
      }
      if (result.code != 200) {
        throw RuntimeException(result.msg)
      }
      result.data
    }
  }

  suspend inline fun <reified T> request(request: Request) = withContext(Dispatchers.IO) {
    runCatching {
      withContext(Dispatchers.IO) {
        getBodyEntity<T>(request)
      }
    }
  }

  inline fun <reified T> requestBlock(request: Request): Result<T> {
    return runCatching {
      getBodyEntity<T>(request)
    }
  }

  inline fun <reified T> getBodyEntity(request: Request): T {
    val response = okHttpClient.newCall(request).execute()
    val responseBody = response.body?.string()
    requireNotNull(responseBody) { "Response body is null" }
    Log.i("NetworkUtils", "responseBody: $responseBody")
    return json.decodeFromString(Json.serializersModule.serializer<T>(), responseBody)
  }

  fun Map<String, String>?.toQueryString(): String {
    return this?.map { "${it.key}=${it.value}" }?.joinToString("&") ?: ""
  }

}