package xyz.jdynb.tv.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import xyz.jdynb.tv.BR

data class TipsModel(
  val title: String,
  val subTitle: String
): BaseObservable() {

  @get:Bindable
  var countdown: Long = 0L
    set(value) {
      field = value
      notifyPropertyChanged(BR.countdown)
    }
}
