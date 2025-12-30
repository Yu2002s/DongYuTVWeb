package xyz.jdynb.tv.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import kotlinx.serialization.Serializable
import xyz.jdynb.tv.BR

@Serializable
data class LiveChannelTypeModel(
  val channelType: String = "央视",
  val player: String = "ysp",
  val channelList: List<LiveChannelModel> = listOf()
): BaseObservable() {

  @get:Bindable
  var isSelected: Boolean = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.selected)
    }

}