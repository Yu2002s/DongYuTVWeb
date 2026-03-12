package xyz.jdynb.tv.utils

import android.util.Log
import com.drake.engine.utils.EncryptUtil
import kotlinx.coroutines.Dispatchers
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

object NetworkUtils {

  const val TAG = "NetworkUtils"

  val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private var baseUrl = ""

  private const val CONFIG_URL = "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/config.json"

  val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
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

  fun getRealRequestUrl(path: String, params: Map<String, String>? = null): String {
    var url = path + "?${params?.toQueryString()}"
    if (!path.startsWith("http")) {
      url = getBaseUrl() + url
    }
    return url
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
    connection.connectTimeout = 10000
    connection.readTimeout = 10000
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

  suspend inline fun <reified T> requestSuspendCache(url: String, params: Map<String, String>? = null): Result<T> {
    return withContext(Dispatchers.IO) {
      runCatching {
        val fullUrl = getRealRequestUrl(url, params)
        Log.i(TAG, "fullUrl: $fullUrl")
        val responseBody = getResponseBody(fullUrl)
        val key = EncryptUtil.encryptMD5ToString(fullUrl)
        val file = File(DongYuTVApplication.context.externalCacheDir, key)
        if (responseBody != null) {
          file.writeText(responseBody)
          getBodyStringEntity(responseBody, true)
        } else if (file.exists()) {
          val cachedBody = file.readText()
          if (cachedBody.isEmpty()) {
            throw IOException("Cached response body is empty")
          } else {
            getBodyStringEntity(cachedBody, true)
          }
        } else {
          throw IOException("Response body is null")
        }
      }
    }
  }

  @Throws(Exception::class)
  inline fun <reified T> requestSyncResult(path: String, params: Map<String, String>? = null) = runCatching {
    requestSync<T>(path, params)
  }

  @Throws(Exception::class)
  inline fun <reified T> requestSync(path: String, params: Map<String, String>? = null): T {
    val url = getRealRequestUrl(path, params)
    val responseBody = getResponseBody(url)
    return getBodyStringEntity<T>(responseBody)
  }

  inline fun <reified T> requestSync(request: Request): Result<T> {
    return runCatching {
      getBodyEntity<T>(request, true)
    }
  }

  suspend inline fun <reified T> requestSuspendResult(
    path: String,
    params: Map<String, String>? = null,
  ): Result<T> {
    Log.i(TAG, "BASE_URL: ${Api.BASE_URL} request: $path, params: $params")
    return runCatching {
      withContext(Dispatchers.IO) {
        requestSync<T>(path, params)
      }
    }
  }

  suspend inline fun <reified T> requestSuspend(request: Request) = withContext(Dispatchers.IO) {
    runCatching {
      withContext(Dispatchers.IO) {
        getBodyEntity<T>(request)
      }
    }
  }

  @Throws(Exception::class)
  inline fun <reified T> getBodyEntity(request: Request, raw: Boolean = false): T {
    val response = okHttpClient.newCall(request).execute()
    val responseBody = response.body?.string()
    return getBodyStringEntity(responseBody, raw)
  }

  @Throws(Exception::class)
  inline fun <reified T> getBodyStringEntity(responseBody: String?, raw: Boolean = false): T {
    requireNotNull(responseBody) { "Response body is null" }
    if (BuildConfig.DEBUG) {
      Log.i(TAG, "responseBody: $responseBody, T: ${T::class.java}")
    }
    if (T::class == String::class) {
      return responseBody as T
    }
    val serializersModule = if (raw) {
      Json.serializersModule.serializer<T>()
    } else {
      json.serializersModule.serializer<ResultModel<T>>()
    }
    val result =
      json.decodeFromString(serializersModule, responseBody)
    if (result is ResultModel<*>) {
      if (result.code != 200) {
        throw RuntimeException(result.msg)
      }
      return result.data as T
    }
    return result as T
  }

  fun Map<String, String>?.toQueryString(): String {
    return this?.map { "${it.key}=${it.value}" }?.joinToString("&") ?: ""
  }

}