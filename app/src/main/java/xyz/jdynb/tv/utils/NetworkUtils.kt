package xyz.jdynb.tv.utils

import com.drake.engine.utils.EncryptUtil
import xyz.jdynb.tv.DongYuTVApplication
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object NetworkUtils {

  fun String.inputStream(headers: Map<String, String>? = null): InputStream? {
    return try {
      createConnection(this).apply {
        headers?.let {
          for ((key, value) in headers) {
            setRequestProperty(key, value)
          }
        }
      }.inputStream
    } catch (_: IOException) {
      null
    }
  }

  @Throws(IOException::class)
  private fun createConnection(url: String): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 3000
    connection.readTimeout = 3000
    connection.requestMethod = "GET"
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

    return  content
  }

}