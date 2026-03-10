package xyz.jdynb.tv.config

import xyz.jdynb.tv.BuildConfig

object Api {

  val BASE_URL: String = if (!BuildConfig.DEBUG) {
    "http://tv.jdynb.xyz"
  } else {
    "http://192.168.1.42:8080"
  }

  const val SEARCH_SUGGEST = "/search/suggest"

  const val SEARCH = "/search"
}