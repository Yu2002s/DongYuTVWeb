package xyz.jdynb.tv.utils

import android.util.Log
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import xyz.jdynb.tv.DongYuTVApplication
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.model.LiveModel
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object JsManager {

  private val jsMap = mutableMapOf<JsType, List<String>>()

  private fun createConnection(url: String): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 3000
    connection.readTimeout = 3000
    connection.requestMethod = "GET"
    // connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Accept", "application/json")
    return connection
  }

  private fun createJsFile(name: String, type: JsType): File {
    val file = File(DongYuTVApplication.context.filesDir, "js/${name}")
    if (!file.exists()) {
      file.mkdirs()
    }
    val jsFile = File(
      file, when (type) {
        JsType.INIT -> "init.js"
        JsType.PLAY -> "play.js"
        JsType.RESUME_PAUSE -> "resume_pause.js"
      }
    )
    return jsFile
  }

  suspend fun writeJsToLocal(name: String, type: JsType, content: String) = withContext(Dispatchers.IO) {
    createJsFile(name, type).writeText(content)
  }

  suspend fun getJsFromLocal(name: String, type: JsType) =
    withContext(Dispatchers.IO) {
      val jsFile = createJsFile(name, type)
      if (jsFile.exists()) {
        jsFile.readText()
      } else {
        null
      }
    }

  private suspend fun getOrWriteJs(url: String, name: String, type: JsType): String? {
    return NetworkUtils.getResponseBody(url)?.also { content ->
      writeJsToLocal(name, type, content)
    } ?: getJsFromLocal(name, type)
  }

  /**
   * 加载 js 列表
   *
   * 加载策略：
   * 1. 先从网络加载，加载成功后保存到 本地
   * 2. 如果网络加载失败，则使用本地已保存的
   *
   * @param playerConfig 播放器配置
   * @param type js 类型
   */
  @Suppress("UNCHECKED_CAST")
  suspend fun getJsList(playerConfig: LiveModel.Player, type: JsType) =
    withContext(Dispatchers.IO) {
      try {
        val scripts = when (type) {
          JsType.INIT -> playerConfig.script.init
          JsType.PLAY -> playerConfig.script.play
          JsType.RESUME_PAUSE -> playerConfig.script.resumePause
        }
        if (playerConfig.script.async) {
          scripts.map {
            async {
              getOrWriteJs(it, playerConfig.name, type)
            }
          }.map {
            it.await()
          }
        } else {
          scripts.map {
            getOrWriteJs(it, playerConfig.name, type)
          }
        }
      } catch (_: Exception) {
        null
      }
    } as List<String>?

  suspend fun getJs(playerConfig: LiveModel.Player, type: JsType): List<String>? {
    val jsList = jsMap[type]
    if (jsList.isNullOrEmpty()) {
      getJsList(playerConfig, type)?.also {
        jsMap[type] = it
      }
    }
    return jsList
  }

  suspend fun WebView.execJs(
    playerConfig: LiveModel.Player,
    type: JsType,
    vararg args: Pair<String, Any?>
  ) {
    getJs(playerConfig, type)?.let {
      var index = 0
      it.forEach { jsStr ->
        var result = jsStr
        for ((key, value) in args.slice(index until args.size)) {
          index++
          if (value != null) {
            result = result.replace("{{${key}}}", value.toString())
          }
        }
        evaluateJavascript(result) { i ->
          Log.i("JsManager", i)
        }
      }
    }
  }
}