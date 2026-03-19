package xyz.jdynb.tv.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import kotlinx.serialization.Serializable
import xyz.jdynb.tv.BR

/**
 * 频道分类
 */
@Serializable
data class LiveChannelTypeModel(
  /**
   * 平道分类名
   */
  val channelType: String = "央视",
  /**
   * 源名称
   */
  val source: String = "",
  /**
   * 播放器名称
   */
  val player: String = "ysp",
  /**
   * 是否隐藏
   */
  var hidden: Boolean = false,
  /**
   * 分类下频道列表
   */
  val channelList: List<LiveChannelModel> = listOf()
): BaseObservable() {

  /**
   * 选中状态
   */
  @get:Bindable
  var isSelected: Boolean = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.selected)
    }

}