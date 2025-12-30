package xyz.jdynb.tv.enums

import xyz.jdynb.tv.fragment.SimpleLivePlayerFragment
import xyz.jdynb.tv.fragment.YspLivePlayerFragment

/**
 * 播放器配置
 */
enum class LivePlayer(val player: String, val clazz: Class<*>) {
  /**
   * 央视频播放器
   */
  YSP("ysp", YspLivePlayerFragment::class.java),

  /**
   * 简单视频播放器
   */
  SIMPLE("simple", SimpleLivePlayerFragment::class.java)

  ;

  companion object {

    @JvmStatic
    fun getLivePlayerForPlayer(player: String): LivePlayer {
      return LivePlayer.entries.find { it.player == player } ?: YSP
    }

    @JvmStatic
    fun getLivePlayerForClass(clazz: Class<*>): LivePlayer {
      return LivePlayer.entries.find { it.clazz == clazz } ?: YSP
    }
  }
}