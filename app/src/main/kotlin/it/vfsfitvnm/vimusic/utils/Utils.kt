@file:OptIn(UnstableApi::class)

package it.vfsfitvnm.vimusic.utils

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.preferences.AppearancePreferences
import it.vfsfitvnm.vimusic.service.LOCAL_KEY_PREFIX
import it.vfsfitvnm.vimusic.service.isLocal
import it.vfsfitvnm.core.ui.utils.SongBundleAccessor
import it.vfsfitvnm.providers.piped.models.Playlist
import it.vfsfitvnm.vimusic.models.Song
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlin.time.Duration

// NEW asMediaItem extension for the current SongItem model (RETAINED from last step)
val it.vfsfitvnm.providers.innertube.models.SongItem.asMediaItem: MediaItem
    get() = MediaItem.Builder()
        .setMediaId(id)
        .setUri(id) // Assuming id can be used as URI or actual media URL is derived elsewhere
        .setCustomCacheKey(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artists.joinToString("") { it.name }) // Use the 'artists' list
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail.toUri()) // Use the 'thumbnail' string directly
                .setExtras(
                    SongBundleAccessor.bundle {
                        durationText = duration?.let { formatAsDuration(it.toLong()) } // Assuming duration is in milliseconds
                        artistNames = artists.map { it.name }
                        artistIds = artists.mapNotNull { it.id }
                        explicit = this@asMediaItem.explicit
                    }
                )
                .build()
        )
        .build()

// REMOVED: This extension is for a VideoItem that no longer exists in your module.
/*
val it.vfsfitvnm.providers.innertube.models.VideoItem.asMediaItem: MediaItem
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString("") { it.name.orEmpty() })
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    SongBundleAccessor.bundle {
                        durationText = this@asMediaItem.durationText
                        artistNames = if (isOfficialMusicVideo) authors
                            ?.filter { it.endpoint != null }
                            ?.mapNotNull { it.name }
                        else null
                        artistIds = if (isOfficialMusicVideo) authors
                            ?.mapNotNull { it.endpoint?.browseId }
                        else null
                    }
                )
                .build()
        )
        .build()
*/

// Existing asMediaItem for Playlist.Video (RETAINED)
val Playlist.Video.asMediaItem: MediaItem?
    get() {
        val key = id ?: return null

        return MediaItem.Builder()
            .setMediaId(key)
            .setUri(key)
            .setCustomCacheKey(key)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(uploaderName)
                    .setArtworkUri(Uri.parse(thumbnailUrl.toString()))
                    .setExtras(
                        SongBundleAccessor.bundle {
                            durationText = duration.toComponents { minutes, seconds, _ ->
                                "$minutes:${seconds.toString().padStart(2, '0')}"
                            }
                            artistNames = listOf(uploaderName)
                            artistIds = uploaderId?.let { listOf(it) }
                        }
                    )
                    .build()
            )
            .build()
    }

val Song.asMediaItem: MediaItem
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    SongBundleAccessor.bundle {
                        durationText = this@asMediaItem.durationText
                        explicit = this@asMediaItem.explicit
                    }
                )
                .build()
        )
        .setMediaId(id)
        .setUri(
            if (isLocal) ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id.substringAfter(LOCAL_KEY_PREFIX).toLong()
            ) else id.toUri()
        )
        .setCustomCacheKey(id)
        .build()

val Duration.formatted
    @Composable get() = toComponents { hours, minutes, _, _ ->
        when {
            hours == 0L -> stringResource(id = R.string.format_minutes, minutes)
            hours < 24L -> stringResource(id = R.string.format_hours, hours)
            else -> stringResource(id = R.string.format_days, hours / 24)
        }
    }

fun String?.thumbnail(
    size: Int,
    maxSize: Int = AppearancePreferences.maxThumbnailSize
): String? {
    val actualSize = size.coerceAtMost(maxSize)
    return when {
        this?.startsWith("https://lh3.googleusercontent.com") == true -> "$this-w$actualSize-h$actualSize"
        this?.startsWith("https://yt3.ggpht.com") == true -> "$this-w$actualSize-h$actualSize-s$actualSize"
        else -> this
    }
}

fun Uri?.thumbnail(size: Int) = toString().thumbnail(size)?.toUri()

fun formatAsDuration(millis: Long) = DateUtils.formatElapsedTime(millis / 1000).removePrefix("0")

// This function needs to be updated to use the new YouTube.playlist and BrowseResponse structure.
// It remains commented out for now.
/*
@Suppress("LoopWithTooManyJumpStatements")
suspend fun Result<Innertube.PlaylistOrAlbumPage>.completed(
    maxDepth: Int = Int.MAX_VALUE,
    shouldDedup: Boolean = false
) = runCatching {
    val page = getOrThrow()
    val songs = page.songsPage?.items.orEmpty().toMutableList()

    if (songs.isEmpty()) return@runCatching page

    var continuation = page.songsPage?.continuation
    var depth = 0

    val context = currentCoroutineContext()

    while (continuation != null && depth++ < maxDepth && context.isActive) {
        val newSongs = Innertube
            .playlistPage(
                body = ContinuationBody(continuation = continuation)
            )
            ?.getOrNull()
            ?.takeUnless { it.items.isNullOrEmpty() } ?: break

        if (shouldDedup && newSongs.items?.any { it in songs } != false) break

        newSongs.items?.let { songs += it }
        continuation = newSongs.continuation
    }

    page.copy(
        songsPage = Innertube.ItemsPage(
            items = songs,
            continuation = null
        )
    )
}.also { it.exceptionOrNull()?.printStackTrace() }
*/

fun <T> Flow<T>.onFirst(block: suspend (T) -> Unit): Flow<T> {
    var isFirst = true

    return onEach {
        if (!isFirst) return@onEach

        block(it)
        isFirst = false
    }
}

inline fun <reified T : Throwable> Throwable.findCause(): T? {
    if (this is T) return this

    var th = cause
    while (th != null) {
        if (th is T) return th
        th = th.cause
    }

    return null
}
