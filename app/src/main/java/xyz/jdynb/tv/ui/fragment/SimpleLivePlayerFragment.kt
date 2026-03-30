package xyz.jdynb.tv.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.net.toUri
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.utils.NetworkUtils.inputStream
import xyz.jdynb.tv.utils.SpUtils.remove
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.random.Random

class SimpleLivePlayerFragment : BaseLivePlayerFragment() {

  companion object {

    private const val TAG = "SimpleLivePlayer"

    // private const val PLAYER_URL = "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/html/simple_player.html"
    private const val PLAYER_URL = "file:///android_asset/html/simple_player.html"
  }

  override fun shouldInterceptRequest(
    url: String,
    request: WebResourceRequest
  ): WebResourceResponse? {
    Log.i(TAG, "${request.method} url: $url")

    val shouldIntercept = super.shouldInterceptRequest(url, request)
    // 网页已自动注入，这里不需要修改
    /*if (url.endsWith("dy-crypto-js.min.js")) {
      // 注入 CRYPTO.JS
      return createJsResponse("js/lib/dy-crypto-js.min.js")
    } else if (url.endsWith("dy-http-util.js")) {
      // 注入网络请求 JS
      return createJsResponse("js/lib/dy-http-util.js")
    } else if (url.endsWith("dy-hls.min.js")) {
      // 注入 HLS.JS
      return createJsResponse("js/lib/dy-hls.min.js")
    }*/

    val referer = request.requestHeaders["X-Referer"]
    Log.i(TAG, "Referer: $referer")

    val isSteamFile = url.contains(".m3u8") || url.contains(".ts")

    /*if (isSteamFile) {
      Log.i(TAG, "streamFile: $url")
      return createResponse(url, "application/x-mpegURL", request.requestHeaders)
    }*/

    if (referer.isNullOrEmpty() && !isSteamFile) {
      // 没有自定义 Referer 加上 非 M3U8、TS 才进行默认请求，否则下面进行重写
      return shouldIntercept
    }

    // 自定义网络请求 Referer

    request.requestHeaders["X-Referer"]?.remove()
    val body = request.requestHeaders["X-Body"]
    request.requestHeaders["X-Body"]?.remove()

    val urlObj = url.toUri()
    val extension = urlObj.path?.substringAfterLast(".") ?: return shouldIntercept
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

    request.requestHeaders["Referer"] = (referer ?: (urlObj.scheme + "://" + urlObj.host + "/"))

    Log.i(TAG, "headers: ${request.requestHeaders}")

    return WebResourceResponse(
      mimeType, "UTF-8", url
        .inputStream(
          method = request.method,
          request.requestHeaders,
          body = body
        )
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    webView.addJavascriptInterface(JSBridge(), "JSBridge")
  }

  override fun onLoadUrl(url: String?, channelModel: LiveChannelModel) {
    webView.loadUrl(PLAYER_URL)
  }

  override fun onPageFinished(url: String, channelModel: LiveChannelModel) {
    super.onPageFinished(url, channelModel)

    // 调试代码
    /*requireContext().assets.open("js/jiangxi/init.js").use {
      it.readBytes().toString(Charsets.UTF_8)
    }.let {
      val js = it.replace("{{m3u8Name}}", "tv_jxtv1.m3u8")
      webView.evaluateJavascript(js, null)
    }*/
  }

  override fun resumeOrPause() {
    webView.evaluateJavascript("resumeOrPause()", null)
  }

  override fun onProgressChanged(newProgress: Int): Int {
    return Random.nextInt(50, 95) // 固定值，表示正在加载中...
  }

  /**
   * JavaScript 桥接类。适配内部的 HttpRequest 工具类
   */
  inner class JSBridge {
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

    @JavascriptInterface
    fun hideLoading() {
      setProgress(100)
    }
  }
}