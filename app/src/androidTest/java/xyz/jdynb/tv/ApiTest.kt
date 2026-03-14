package xyz.jdynb.tv

import org.junit.Test
import xyz.jdynb.tv.utils.AESUtils
import xyz.jdynb.tv.utils.EncryptUtils.md5

class ApiTest {

  @Test
  fun test() {
    val ts = 1773497305183L
    val url = "https%3A%2F%2Fv.youku.com%2Fvideo%3Fs%3Dcc044d06962411de83b1"

    val key = (ts.toString() + url).md5()
    val encrypt = AESUtils.encrypt(key, key.md5(), "fUU9eRmkYzsgbkEK", "AES/CBC/NoPadding")
    println("key=$key, encrypt=$encrypt")
  }

}