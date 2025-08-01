package it.vfsfitvnm.vimusic.models

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import it.vfsfitvnm.core.ui.ColorParceler
import it.vfsfitvnm.providers.innertube.requests.MoodAndGenres
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

@Parcelize
data class Mood(
    val name: String,
    val color: @WriteWith<ColorParceler> Color,
    val browseId: String?,
    val params: String?
) : Parcelable

fun MoodAndGenres.Item.toUiMood() = Mood(
    name = title,
    color = Color(stripeColor.toInt()),
    browseId = endpoint.browseId,
    params = endpoint.params
)
