package xyz.jdynb.tv.utils

import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

fun Any?.toBundle(): Bundle {
  return this?.javaClass?.let { clazz ->
    val fields = clazz.declaredFields
    val bundle = bundleOf()
    fields.forEach {  field ->
      field.isAccessible = true
      val name = field.name
      when (val value = field.get(this)) {
        is String -> bundle.putString(name, value)
        is Int -> bundle.putInt(name, value)
        is Boolean -> bundle.putBoolean(name, value)
      }
    }
    bundle
  } ?: Bundle.EMPTY
}

fun Any?.toArray(): Array<Pair<String, Any>> {
  return this?.javaClass?.let { clazz ->
    val fields = clazz.declaredFields
    val list = mutableListOf<Pair<String, Any>>()
    fields.forEach {  field ->
      field.isAccessible = true
      val name = field.name
      val value = field.get(this)
      if (value is Map<*, *>) {
        value.forEach {
          if (it.value != null) {
            list.add((it.key as String) to it.value!!)
          }
        }
      } else {
        if (value != null) {
          list.add(name to value)
        }
      }
    }
    list.toTypedArray()
  } ?: arrayOf()
}

inline fun <reified T> Bundle.toObj(): T? {
  val objClass = T::class.java
  val obj = objClass.getDeclaredConstructor().newInstance()
  objClass.declaredFields.forEach {
    it.isAccessible = true
    val value = get(it.name)
    it.set(obj, value)
  }
  return obj
}

/**
 * Bundle 中添加序列化参数
 */
inline fun <reified T : @Serializable Any> Bundle.putSerializable(key: String, value: T) {
  val jsonString = Json.encodeToString(value)
  putString(key, jsonString)
}

inline fun <reified T : @Serializable Any> Intent.putSerializable(key: String, value: T) {
  putExtra(key, Json.encodeToString(value))
}

/**
 * Bundle 中获取序列化参数
 */
inline fun <reified T : @Serializable Any> Bundle.getSerializableForKey(key: String): T? {
  val jsonString = getString(key) ?: return null
  return Json.decodeFromString<T>(jsonString)
}


/**
* Fragment 中添加序列化参数
*/
inline fun <reified T : @Serializable Any> Fragment.setSerializableArguments(
  key: String,
  value: T
) {
  arguments = (arguments ?: Bundle()).apply {
    putSerializable(key, value)
  }
}

/**
 * Fragment 中获取序列化参数
 */
inline fun <reified T : @Serializable Any> Fragment.getSerializableArguments(key: String): T? {
  return arguments?.getSerializableForKey(key)
}
