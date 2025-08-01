package it.vfsfitvnm.providers.innertube.models.bodies

import it.vfsfitvnm.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
