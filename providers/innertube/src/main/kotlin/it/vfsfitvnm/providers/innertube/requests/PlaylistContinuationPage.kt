package it.vfsfitvnm.providers.innertube.requests

import it.vfsfitvnm.providers.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
