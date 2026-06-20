package xyz.jdynb.tv.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.utils.showToast
import xyz.jdynb.tv.utils.toArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.random.Random

/**
 * 通用的播放器
 */
open class BaseLivePlayerFragment : LivePlayerFragment() {

  companion object {

    /**
     * 加载超时时间
     */
    private const val LOAD_TIMEOUT_TIME = 10 * 1000L

  }

  /**
   * 页面加载开始的时间
   */
  private var loadStartTime = 0L

  private val handler = Handler(Looper.getMainLooper())

  private val loadMap = mutableMapOf<Int, Int>()

  private val runnable = Runnable {
    if (getProgress() < 100) {
      Timber.i("播放超时了，自动换源")
      // 播放超时了，自动换源
      if (mainViewModel.nextSource(false)) {
        loadMap[currentChannelModel.number] = loadMap.getOrElse(currentChannelModel.number) { 0 } + 1
      } else {
        "加载超时，自动下一个频道".showToast(Toast.LENGTH_LONG)
        mainViewModel.up()
      }
    }
  }

  fun startLoadTimeout() {
    loadStartTime = System.currentTimeMillis()
    val count = loadMap.getOrPut(currentChannelModel.number) { 0 }
    if (count > currentChannelModel.children.size) {
      return
    }
    handler.removeCallbacks(runnable)
    val timeout = SPKeyConstants.PLAY_TIMEOUT_DURATION.getRequired<Long>(LOAD_TIMEOUT_TIME)
    if (timeout == 0L) return
    handler.postDelayed(runnable, timeout)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    webView.addJavascriptInterface(JSBridge(), "JSBridge")
  }

  override fun onLoadUrl(url: String?, channelModel: LiveChannelModel) {
    if (url.isNullOrEmpty()) {
      return
    }
    var finialUrl: String = url
    val array = channelModel.args
    array.forEach {
      finialUrl = finialUrl.replace("{{${it.key}}}", it.value)
    }
    webView.loadUrl(finialUrl)
  }

  override fun play(channel: LiveChannelModel) {
    setProgress()
    // 默认的播放
    execJs(JsType.PLAY, *channel.toArray())
    startLoadTimeout()
  }

  override fun refresh() {
    // 重置加载次数
    loadMap[currentChannelModel.number] = 0
    super.refresh()
  }

  override fun resumeOrPause() {
    execJs(JsType.RESUME_PAUSE)
  }

  override fun shouldOverride(url: String): Boolean {
    return false
  }

  override fun onPageStarted(url: String, channelModel: LiveChannelModel) {
  }

  override fun onPageFinished(url: String, channelModel: LiveChannelModel) {
    execJs(JsType.INIT, *channelModel.toArray()) {
      startLoadTimeout()
    }
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
        if (!headers.isNullOrEmpty()) {
          val headersJson = JSONObject(headers)
          val keys = headersJson.keys()
          while (keys.hasNext()) {
            val key = keys.next()
            connection.setRequestProperty(key, headersJson.getString(key))
          }
        }

        // 处理请求体
        if (!body.isNullOrEmpty() && !method.equals(
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
        val inputStream = if (statusCode in 200..<400) {
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
      Timber.i("hideLoading")
    }

    @JavascriptInterface
    fun showLoading() {
      Timber.i("showLoading")
      setProgress(Random.nextInt(1, 95))
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.removeCallbacksAndMessages(null)
  }

}