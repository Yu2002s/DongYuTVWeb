package xyz.jdynb.tv.utils

import okio.ByteString.Companion.encodeUtf8

object EncryptUtils {

  fun String.md5(): String {
    return this.encodeUtf8().md5().hex()
  }

}