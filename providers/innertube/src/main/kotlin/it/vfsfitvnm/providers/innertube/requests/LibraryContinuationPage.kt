package it.vfsfitvnm.providers.innertube.requests

import it.vfsfitvnm.providers.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
