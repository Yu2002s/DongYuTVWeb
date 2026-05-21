package xyz.jdynb.tv.model


import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.PropertyChangeRegistry
import com.drake.brv.binding.ObservableIml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport
import xyz.jdynb.tv.BR

/**
 * 直播频道
 */
@Serializable
data class LiveChannelModel(
  /**
   * 名称
   */
  @SerialName("channelName")
  var channelName: String = "",
  /**
   * 台标，目前没有用上
   */
  @SerialName("tvLogo")
  var tvLogo: String = "",
  /**
   * 频道分类
   */
  @SerialName("channelType")
  var channelType: String = "",
  /**
   * 直播序号（对应键盘输入）唯一值
   */
  @Transient
  var number: Int = 0,

  /**
   * 额外所需的参数
   */
  var args: Map<String, String> = mapOf(),

  /**
   * 播放器 id
   */
  var player: String = "",

  /**
   * 源
   */
  var source: String = "",

  /**
   * 是否隐藏
   */
  var hidden: Boolean = false,

  /**
   * 描述
   */
  val desc: String? = null,

  /**
   * 子频道
   */
  @Column(ignore = true)
  val children: List<LiveChannelModel> = listOf(),
): LitePalSupport(), ObservableIml {

  @Transient
  override val registry: PropertyChangeRegistry = PropertyChangeRegistry()

  @Transient
  var id: Long = 0L

  val showDesc: String get() = desc ?: ""

  /**
   * 收藏
   */
  var isFavorite: Boolean = false

  /**
   * 选中状态
   */
  @get:Bindable
  @Column(ignore = true)
  var isSelected: Boolean = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.selected)
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LiveChannelModel

    if (number != other.number) return false
    if (channelName != other.channelName) return false
    if (channelType != other.channelType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = number
    result = 31 * result + channelName.hashCode()
    result = 31 * result + channelType.hashCode()
    return result
  }


}