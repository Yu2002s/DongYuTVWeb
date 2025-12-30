package xyz.jdynb.tv.model


import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.jdynb.tv.BR
import xyz.jdynb.tv.constants.LiveModeConstants

@Serializable
data class LiveChannelModel(
  @SerialName("channelName")
  var channelName: String = "CCTV1",
  @SerialName("pid")
  var pid: String? = "600001859",
  @SerialName("tvLogo")
  var tvLogo: String = "https://resources.yangshipin.cn/assets/oms/image/202306/d57905b93540bd15f0c48230dbbbff7ee0d645ff539e38866e2d15c8b9f7dfcd.png?imageMogr2/format/webp",
  @SerialName("streamId")
  var streamId: String? = "2024078201",
  @SerialName("channelType")
  var channelType: String = "央视",
  /**
   * 直播序号（对应键盘输入）唯一值
   */
  var number: Int = 1,

  /**
   * 额外所需的参数
   */
  var args: Map<String, String> = mapOf(),

  /**
   * 播放器 id
   */
  var player: String = "ysp",
) : BaseObservable() {

  @get:Bindable
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