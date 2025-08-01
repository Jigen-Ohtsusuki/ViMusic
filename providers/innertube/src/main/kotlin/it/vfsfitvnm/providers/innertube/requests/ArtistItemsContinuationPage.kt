package it.vfsfitvnm.providers.innertube.requests

import it.vfsfitvnm.providers.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
