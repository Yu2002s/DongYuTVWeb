package xyz.jdynb.tv.enums

import xyz.jdynb.tv.ui.fragment.BaseLivePlayerFragment
import xyz.jdynb.tv.ui.fragment.SimpleLivePlayerFragment
import xyz.jdynb.tv.ui.fragment.YspLivePlayerFragment

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
  SIMPLE("simple", SimpleLivePlayerFragment::class.java),

  /**
   * 通用视频播放器
   */
  BASE("base", BaseLivePlayerFragment::class.java)
  ;

  companion object {

    /**
     * 通过 player 名称获取对应的 LivePlayer 配置
     */
    @JvmStatic
    fun getLivePlayerForPlayer(player: String): LivePlayer? {
      return LivePlayer.entries.find { it.player == player }
    }

    /**
     * 通过 class 获取对应的 LivePlayer 配置
     */
    @JvmStatic
    fun getLivePlayerForClass(clazz: Class<*>): LivePlayer {
      return LivePlayer.entries.find { it.clazz == clazz } ?: YSP
    }
  }
}