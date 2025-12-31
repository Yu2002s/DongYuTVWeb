package xyz.jdynb.tv.fragment

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.utils.NetworkUtils
import xyz.jdynb.tv.utils.NetworkUtils.inputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import androidx.core.net.toUri


class SimpleLivePlayerFragment : LivePlayerFragment() {

  companion object {

    private const val TAG = "SimpleLivePlayerFragment"

  }

  // JavaScript 桥接类
  class JSBridge(private val context: Context?) {
    @JavascriptInterface
    fun httpRequest(url: String, method: String, headers: String?, body: String?): String {
      try {
        val connection = URL(url).openConnection() as HttpURLConnection

        // 设置请求方法
        connection.setRequestMethod(method.uppercase(Locale.getDefault()))

        // 设置超时
        connection.setConnectTimeout(5000)
        connection.setReadTimeout(5000)

        // 解析并设置请求头
        if (headers != null && !headers.isEmpty()) {
          val headersJson = JSONObject(headers)
          val keys = headersJson.keys()
          while (keys.hasNext()) {
            val key = keys.next()
            connection.setRequestProperty(key, headersJson.getString(key))
          }
        }

        // 处理请求体
        if (body != null && !body.isEmpty() && !method.equals(
            "GET",
            ignoreCase = true
          ) && !method.equals("HEAD", ignoreCase = true)
        ) {
          connection.setDoOutput(true)
          connection.getOutputStream().use { os ->
            os.write(body.toByteArray(StandardCharsets.UTF_8))
            os.flush()
          }
        }

        // 获取响应
        val statusCode = connection.getResponseCode()

        // 读取响应内容
        val inputStream = if (statusCode >= 200 && statusCode < 400) {
          connection.getInputStream()
        } else {
          connection.errorStream
        }

        var responseBody = ""
        if (inputStream != null) {
          BufferedReader(
            InputStreamReader(inputStream, StandardCharsets.UTF_8)
          ).use { reader ->
            val response = StringBuilder()
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
              response.append(line)
            }
            responseBody = response.toString()
          }
        }

        // 获取响应头
        val responseHeaders = JSONObject()
        val headerFields = connection.headerFields
        for (entry in headerFields.entries) {
          if (entry.key != null && entry.value != null) {
            responseHeaders.put(
              entry.key,
              JSONArray(entry.value)
            )
          }
        }

        // 构建响应 JSON
        val response = JSONObject()
        response.put("status", statusCode)
        response.put("statusText", connection.getResponseMessage())
        response.put("body", responseBody)
        response.put("headers", responseHeaders)

        return response.toString()
      } catch (e: Exception) {
        try {
          val error = JSONObject()
          error.put("error", true)
          error.put("message", e.message)
          return error.toString()
        } catch (jsonException: JSONException) {
          return "{\"error\": true, \"message\": \"Unknown error\"}"
        }
      }
    }

    @JavascriptInterface
    fun showToast(message: String?) {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
  }

  protected fun createHttpUtilJsResponse(): WebResourceResponse {
    val cryptoJs = requireContext().assets.open("js/lib/dy-http-util.js")
    // 创建一个空的响应
    return WebResourceResponse(
      "application/javascript",
      "UTF-8",
      cryptoJs
    )
  }

  protected fun createHlsJsResponse(): WebResourceResponse {
    val hlsJs = requireContext().assets.open("js/lib/dy-hls.min.js")
    // 创建一个空的响应
    return WebResourceResponse(
      "application/javascript",
      "UTF-8",
      hlsJs
    )
  }

  override fun shouldInterceptRequest(
    url: String,
    request: WebResourceRequest
  ): WebResourceResponse? {
    val shouldIntercept = super.shouldInterceptRequest(url, request)
    if (url.endsWith("dy-crypto-js.min.js")) {
      // 注入 CRYPTO.JS
      return createCryptoJsResponse()
    } else if (url.endsWith("dy-http-util")) {
      // 注入网络请求 JS
      return createHttpUtilJsResponse()
    } else if (url.endsWith("dy-hls.min.js")) {
      return createHlsJsResponse()
    }

    if (url.contains(".m3u8") || url.contains(".ts")) {
      val uri = url.toUri()
      return WebResourceResponse(
        "application/vnd.apple.mpegurl", "UTF-8", url
          .inputStream(
            mapOf(
              "Referer" to uri.scheme + "://" + uri.host + "/",
              "User-Agent" to USER_AGENT,
            )
          )
      )
    }

    return shouldIntercept
  }

  private val jsBridge by lazy {
    JSBridge(requireContext())
  }

  override fun onLoadUrl(url: String?) {
    webView.addJavascriptInterface(jsBridge, "JSBridge")
    webView.loadUrl("file:///android_asset/html/simple_player.html")
  }

  override fun onPageFinished(url: String) {
    super.onPageFinished(url)

    // 调试代码
    /*requireContext().assets.open("js/anhui/init.js").use {
      it.readBytes().toString(Charsets.UTF_8)
    }.let {
      val js = it.replace("{{channelName}}", "安徽卫视")
      webView.evaluateJavascript(js, null)
    }*/
  }

  override fun play(channel: LiveChannelModel) {
    super.play(channel)
  }

  override fun resumeOrPause() {
    webView.evaluateJavascript("resumeOrPause()", null)
  }
}