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

  fun String.inputStream(method: String = "GET",headers: Map<String, String>? = null, body: String? = null): InputStream? {
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
  private fun createConnection(url: String, method: String = "GET"): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 3000
    connection.readTimeout = 3000
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