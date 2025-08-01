package it.vfsfitvnm.providers.innertube.models.bodies

import it.vfsfitvnm.providers.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class ContinuationBody(
    val context: YouTubeClient = YouTubeClient.WEB_REMIX,
    val continuation: String
)
