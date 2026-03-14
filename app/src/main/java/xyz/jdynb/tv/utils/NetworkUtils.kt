package xyz.jdynb.tv.utils

import android.content.Intent
import android.util.Log
import com.drake.engine.utils.EncryptUtil
import io.nerdythings.okhttp.profiler.OkHttpProfilerInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.music.utils.SpUtils.put
import xyz.jdynb.music.utils.SpUtils.remove
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.DongYuTVApplication
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.constants.IntentActionConstants
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.exception.NotLoginException
import xyz.jdynb.tv.exception.ResultDataNullException
import xyz.jdynb.tv.exception.ResultStatusException
import xyz.jdynb.tv.model.ConfigModel
import xyz.jdynb.tv.model.ResultModel
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.CookieStore
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

  val persistentCookieJar = PersistentCookieJar()

  val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
    .cookieJar(persistentCookieJar)
    .addInterceptor { chan ->
      val request = chan.request().newBuilder()
        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36")
        .addHeader("Accept", "*/*")
        .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
        .build()

      chan.proceed(request)
    }
    .also {
      if (BuildConfig.DEBUG) {
        it.addInterceptor(OkHttpProfilerInterceptor())
      }
    }
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
    var url = path + "?${params.toQueryString()}"
    if (!path.startsWith("http")) {
      url = Api.BASE_URL + url
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
    Log.i(TAG, "getResponseBody: $url")
    return try {
      val request = Request.Builder()
        .url(url)
        .build()
      okHttpClient.newCall(request).execute().body?.string()
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

  suspend inline fun <reified T> requestSuspendCache(
    url: String,
    params: Map<String, String>? = null,
    raw: Boolean = false,
  ): Result<T> {
    return withContext(Dispatchers.IO) {
      runCatching {
        val fullUrl = getRealRequestUrl(url, params)
        Log.i(TAG, "fullUrl: $fullUrl")
        val responseBody = getResponseBody(fullUrl)
        val key = EncryptUtil.encryptMD5ToString(fullUrl)
        val file = File(DongYuTVApplication.context.externalCacheDir, key)
        if (responseBody != null) {
          file.writeText(responseBody)
          getBodyStringEntity(responseBody, raw = raw)
        } else if (file.exists()) {
          val cachedBody = file.readText()
          if (cachedBody.isEmpty()) {
            throw IOException("Cached response body is empty")
          } else {
            getBodyStringEntity(cachedBody, raw = raw)
          }
        } else {
          throw IOException("Response body is null")
        }
      }
    }
  }

  @Throws(Exception::class)
  inline fun <reified T> requestSyncResult(
    path: String,
    params: Map<String, String>? = null,
    raw: Boolean = false
  ) =
    runCatching {
      requestSync<T>(path, params, raw = raw)
    }

  @Throws(Exception::class)
  inline fun <reified T> requestSync(
    path: String,
    params: Map<String, String>? = null,
    raw: Boolean = false
  ): T {
    val url = getRealRequestUrl(path, params)
    val responseBody = getResponseBody(url)
    return getBodyStringEntity<T>(responseBody, raw = raw)
  }

  inline fun <reified T> requestSyncResult(request: Request, raw: Boolean = false): Result<T> {
    return runCatching {
      getBodyEntity<T>(request, raw)
    }
  }

  inline fun <reified T> requestSyncResult(
    path: String,
    formBody: RequestBody,
    raw: Boolean = false
  ): Result<T> {
    val request = Request.Builder()
      .url(getRealRequestUrl(path))
      .post(formBody)
      .build()
    return requestSyncResult<T>(request, raw = raw)
  }

  suspend inline fun <reified T> requestSuspendResult(
    path: String,
    params: Map<String, String>? = null,
    raw: Boolean = false,
  ): Result<T> {
    Log.i(TAG, "BASE_URL: ${Api.BASE_URL} request: $path, params: $params")
    return runCatching {
      withContext(Dispatchers.IO) {
        requestSync<T>(path, params, raw = raw)
      }
    }
  }

  suspend inline fun <reified T> requestSuspend(
    path: String,
    params: Map<String, String>? = null,
    raw: Boolean = false
  ) =
    withContext(Dispatchers.IO) {
      requestSync<T>(path, params, raw = raw)
    }

  suspend inline fun <reified T> requestSuspend(request: Request, raw: Boolean = false) =
    withContext(Dispatchers.IO) {
      runCatching {
        withContext(Dispatchers.IO) {
          getBodyEntity<T>(request, raw = raw)
        }
      }
    }

  @Throws(Exception::class)
  inline fun <reified T> getBodyEntity(request: Request, raw: Boolean = false): T {
    val response = okHttpClient.newCall(request).execute()
    val responseBody = response.body?.string()
    return getBodyStringEntity(responseBody, raw = raw)
  }

  @Throws(Exception::class)
  inline fun <reified T> getBodyStringEntity(responseBody: String?, raw: Boolean = false): T {
    requireNotNull(responseBody) { "服务器响应异常，请稍后重试" }
    if (BuildConfig.DEBUG) {
      Log.i(TAG, "responseBody: $responseBody, T: ${T::class.java}")
    }
    if (T::class == String::class) {
      return responseBody as T
    }
    val serializersModule = if (raw) {
      json.serializersModule.serializer<T>()
    } else {
      json.serializersModule.serializer<ResultModel<T>>()
    }
    val result =
      json.decodeFromString(serializersModule, responseBody)
    if (result is ResultModel<*>) {
      if (result.code == 200) {
        if (raw) {
          return result as T
        }

        if (result.data == null) {
          // Result data is null
          throw ResultDataNullException(result.msg)
        }
        return result.data as T
      } else if (result.code == 401) {
        SPKeyConstants.USER_AUTH.remove()
        persistentCookieJar.clearAllCookies()
        // Unauthorized
        DongYuTVApplication.context.sendBroadcast(
          Intent(IntentActionConstants.UN_AUTHORIZED)
            .also {
              it.`package` = DongYuTVApplication.context.packageName
            })
        throw NotLoginException(result.msg)
      } else {
        throw ResultStatusException(result.msg)
      }
    }
    return result as T
  }

  fun Map<String, String>?.toQueryString(): String {
    return (this?.map { "${it.key}=${it.value}" }?.joinToString("&")) ?: ""
  }

}