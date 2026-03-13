package xyz.jdynb.tv.config

import xyz.jdynb.tv.BuildConfig

object Api {

  val BASE_URL: String = if (/*!BuildConfig.DEBUG*/true) {
    // "http://tv.jdynb.xyz"
    "http://47.103.74.108:8005"
  } else {
    "http://192.168.1.42:8080"
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
}