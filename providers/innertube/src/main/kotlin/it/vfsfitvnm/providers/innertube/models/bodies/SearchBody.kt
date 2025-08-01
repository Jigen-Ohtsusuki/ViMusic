package it.vfsfitvnm.providers.innertube.models.bodies

import it.vfsfitvnm.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
