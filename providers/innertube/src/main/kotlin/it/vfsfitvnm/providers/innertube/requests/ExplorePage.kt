package it.vfsfitvnm.providers.innertube.requests

import it.vfsfitvnm.providers.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres>,
)
