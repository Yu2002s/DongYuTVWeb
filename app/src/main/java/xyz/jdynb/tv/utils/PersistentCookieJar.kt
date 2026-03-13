package xyz.jdynb.tv.utils

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import xyz.jdynb.tv.DongYuTVApplication

class PersistentCookieJar: CookieJar {

  private val cookieCacheMap = mutableMapOf<String, MutableList<Cookie>>()

  private val sharedPreferences = DongYuTVApplication.context.getSharedPreferences("cookie", Context.MODE_PRIVATE)

  init {
    sharedPreferences.all.forEach { (key, value) ->
      val host = key.removePrefix("cookie_")
      val json = value as? String ?: return@forEach

      try {
        val serializableCookies = NetworkUtils.json.decodeFromString<List<SerializableCookie>>(json)
        val cookies = serializableCookies.mapNotNull { it.toCookie() }
          .filter { it.expiresAt > System.currentTimeMillis() }
        if (cookies.isNotEmpty()) {
          cookieCacheMap[host] = cookies.toMutableList()
        }
      } catch (_: Exception) {
      }
    }
  }

  override fun loadForRequest(url: HttpUrl): List<Cookie> {
    val host = url.host
    val cookies = cookieCacheMap[host] ?: return emptyList()

    val validCookies = cookies.filter { it.expiresAt > System.currentTimeMillis() }

    if (validCookies.size != cookies.size) {
      cookieCacheMap[host] = validCookies.toMutableList()
      saveCookies(host, validCookies)
    }

    return validCookies
  }

  override fun saveFromResponse(
    url: HttpUrl,
    cookies: List<Cookie>
  ) {
    if (cookies.isEmpty()) return

    val host = url.host
    val existingCookies = cookieCacheMap[host]?.toMutableList() ?: mutableListOf()

    // 合并新旧 Cookie
    val cookieMap = (existingCookies + cookies).associateBy { it.name }.toMutableMap()
    val newCookies = cookieMap.values.toMutableList()

    cookieCacheMap[host] = newCookies
    saveCookies(host, newCookies)
  }

  private fun saveCookies(host: String, cookies: List<Cookie>) {
    val serializableList = cookies.map { SerializableCookie(it) }
    val json = NetworkUtils.json.encodeToString(serializableList)
    sharedPreferences.edit { putString("cookie_$host", json) }
  }

  fun clearAllCookies() {
    cookieCacheMap.clear()
    sharedPreferences.edit { clear() }
  }

  @Serializable
  private data class SerializableCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean
  ) {
    constructor(cookie: Cookie) : this(
      name = cookie.name,
      value = cookie.value,
      expiresAt = cookie.expiresAt,
      domain = cookie.domain,
      path = cookie.path,
      secure = cookie.secure,
      httpOnly = cookie.httpOnly
    )

    fun toCookie(): Cookie? = try {
      Cookie.Builder()
        .name(name)
        .value(value)
        .domain(domain)
        .path(path)
        .expiresAt(expiresAt)
        .apply { if (secure) secure() }
        .apply { if (httpOnly) httpOnly() }
        .build()
    } catch (e: Exception) {
      null
    }
  }
}