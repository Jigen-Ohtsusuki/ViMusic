package it.vfsfitvnm.vimusic.utils

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import it.vfsfitvnm.providers.innertube.YouTube
import it.vfsfitvnm.providers.innertube.models.SongItem
import it.vfsfitvnm.providers.innertube.models.WatchEndpoint
import it.vfsfitvnm.providers.innertube.models.YouTubeClient
import it.vfsfitvnm.providers.innertube.requests.NextResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class YouTubeRadio(
    private var videoId: String? = null,
    private var playlistId: String? = null,
    private var playlistSetVideoId: String? = null,
    private var params: String? = null,
    private val client: YouTubeClient = YouTubeClient.WEB_REMIX,
) {
    private var nextContinuation: String? = null

    suspend fun process(): List<MediaItem> {
        var mediaItems: List<MediaItem>? = null

        // Create the WatchEndpoint from the current state of the radio.
        val endpoint = WatchEndpoint(
            videoId = videoId,
            playlistId = playlistId,
            params = params,
            playlistSetVideoId = playlistSetVideoId,
        )

        // Call the correct function: YouTube.next(), which takes an endpoint and an optional continuation.
        val nextResult: Result<NextResult> = withContext(Dispatchers.IO) {
            YouTube.next(
                endpoint = endpoint,
                continuation = nextContinuation
            )
        }

        // The rest of the logic correctly processes the NextResult.
        nextContinuation = nextResult.getOrNull()?.let { result ->
            mediaItems = result.items.map { it.asMediaItem() }
            result.continuation?.takeUnless { nextContinuation == it }
        }

        return mediaItems ?: emptyList()
    }

    companion object {
        fun from(endpoint: WatchEndpoint?, client: YouTubeClient = YouTubeClient.WEB_REMIX): YouTubeRadio {
            return YouTubeRadio(
                videoId = endpoint?.videoId,
                playlistId = endpoint?.playlistId,
                playlistSetVideoId = endpoint?.playlistSetVideoId,
                params = endpoint?.params,
                client = client
            )
        }
    }
}

fun SongItem.asMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artists.joinToString { it.name })
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail.toUri())
                .build()
        )
        .build()
}
