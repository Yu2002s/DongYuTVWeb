package xyz.jdynb.tv.model


import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.internal.notify
import xyz.jdynb.tv.BR
import xyz.jdynb.tv.R

@Serializable
data class MovieModel(
    @SerialName("id")
    var id: String = "",
    @SerialName("title")
    var title: String = "",
    @SerialName("category")
    var category: String = "",
    @SerialName("thumbUrl")
    var thumbUrl: String = "",
    @SerialName("director")
    var director: String? = "",
    @SerialName("actor")
    var actor: String? = "",
    @SerialName("introduction")
    var introduction: String? = "",
    @SerialName("area")
    var area: String? = "",
    @SerialName("year")
    var year: String? = "",
    @SerialName("url")
    var url: String? = "",
    @SerialName("items")
    var items: List<Item>? = emptyList()
) {
    @Serializable
    data class Item(
        @SerialName("name")
        var name: String = "",
        @SerialName("url")
        var url: String = "",
        @SerialName("videoId")
        var videoId: String? = null,
        @SerialName("parentId")
        var parentId: String = ""
    ): BaseObservable() {

        @get:Bindable
        var checked: Boolean = false
            set(value) {
                field = value
                notifyPropertyChanged(BR.checked)
            }

    }
}