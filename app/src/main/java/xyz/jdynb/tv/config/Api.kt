package xyz.jdynb.tv.config

import xyz.jdynb.tv.BuildConfig

object Api {

  val BASE_URL: String = if (/*!BuildConfig.DEBUG*/true) {
    "http://tv.jdynb.xyz"
  } else {
    "http://192.168.31.140:8080"
  }

  /**
   * 搜索建议
   */
  const val SEARCH_SUGGEST = "/search/suggest"

  /**
   * 搜索
   */
  const val SEARCH = "/search"

  /**
   * 获取解析规则
   */
  const val GET_PARSE_RULE = "/video/get-parse-rule"

  /**
   * 获取验证码
   */
  const val VERIFY_CODE = "/user/verifyCode"

  /**
   * 检查更新
   */
  const val CHECK_UPDATE = "/update/check"

  /**
   * App崩溃记录
   */
  const val APP_CRASH = "/app/crash/add"

  /**
   * webview 列表
   */
  const val WEBVIEW_LIST = "/webview/list"

  /**
   * App FAQ 列表
   */
  const val APP_FAQ_LIST = "/app/faq/list"

  /**
   * 频道列表
   */
  const val CHANNEL_LIST = "/movies/live/channel/list"
}