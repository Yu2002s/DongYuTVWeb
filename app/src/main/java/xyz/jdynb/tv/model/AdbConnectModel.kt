package xyz.jdynb.tv.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import xyz.jdynb.tv.BR

data class AdbConnectModel(
  var host: String = "192.168.",
  var port: String = "",
  var code: String = "",
): BaseObservable() {

  @get:Bindable
  var command: String = ""
    set(value) {
      field = value
      notifyPropertyChanged(BR.command)
    }

  @get:Bindable
  var isConnected: Boolean = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.connected)
    }

  @get:Bindable("isConnected")
  val status: String get() = if (isConnected) "已连接" else "未连接"

  @get:Bindable
  var log: String = ""
    set(value) {
      field = value
      notifyPropertyChanged(BR.log)
    }
}
