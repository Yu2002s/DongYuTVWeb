package xyz.jdynb.tv.utils

import com.tencent.smtt.sdk.WebView
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.model.LiveModel
import xyz.jdynb.tv.utils.JsManager.getJs

object X5JsManager {

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
          // Log.i("JsManager", i)
        }
      }
    }
  }

}