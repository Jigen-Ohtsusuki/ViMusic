package it.vfsfitvnm.vimusic.ui.screens.playlist

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.valentinilk.shimmer.shimmer
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.core.ui.Dimensions
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.core.ui.utils.isLandscape
import it.vfsfitvnm.providers.innertube.YouTube
import it.vfsfitvnm.providers.innertube.models.SongItem as InnertubeSongItem
import it.vfsfitvnm.providers.innertube.requests.PlaylistPage
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Playlist
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.models.SongPlaylistMap
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.transaction
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.ShimmerHost
import it.vfsfitvnm.vimusic.ui.components.themed.*
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.items.SongItemPlaceholder
import it.vfsfitvnm.vimusic.utils.*
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistSongList(
    browseId: String,
    modifier: Modifier = Modifier,
    params: String? = null,
    maxDepth: Int? = null,
    shouldDedup: Boolean = false,
)
 {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    var playlistPage by persist<PlaylistPage?>("playlist/$browseId/playlistPage")

    LaunchedEffect(Unit) {
        if (playlistPage?.songsContinuation == null) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            YouTube.playlist(browseId)
        }.onSuccess {
            playlistPage = it
        }
    }

    var isImportingPlaylist by rememberSaveable { mutableStateOf(false) }

    if (isImportingPlaylist) TextFieldDialog(
        hintText = stringResource(R.string.enter_playlist_name_prompt),
        initialTextInput = playlistPage?.playlist?.title.orEmpty(),
        onDismiss = { isImportingPlaylist = false },
        onAccept = { text ->
            query {
                transaction {
                    val playlistId = Database.instance.insert(
                        Playlist(
                            name = text,
                            browseId = browseId,
                            thumbnail = playlistPage?.playlist?.thumbnail
                        )
                    )

                    playlistPage?.songs
                        ?.map { it.toLocalSong() }
                        ?.onEach { song -> Database.instance.insert(song) }
                        ?.mapIndexed { index, localSong ->
                            SongPlaylistMap(
                                songId = localSong.id,
                                playlistId = playlistId,
                                position = index
                            )
                        }?.let(Database.instance::insertSongPlaylistMaps)
                }
            }
        }
    )

    val headerContent: @Composable () -> Unit = {
        if (playlistPage == null) HeaderPlaceholder(modifier = Modifier.shimmer())
        else Header(title = playlistPage?.playlist?.title ?: stringResource(R.string.unknown)) {
            SecondaryTextButton(
                text = stringResource(R.string.enqueue),
                enabled = !playlistPage?.songs.isNullOrEmpty(),
                onClick = {
                    playlistPage?.songs?.map(InnertubeSongItem::asMediaItem)
                        ?.let { mediaItems ->
                            binder?.player?.enqueue(mediaItems)
                        }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            playlistPage?.songs?.map(InnertubeSongItem::asMediaItem)
                ?.let { PlaylistDownloadIcon(songs = it.toImmutableList()) }

            HeaderIconButton(
                icon = R.drawable.add,
                color = colorPalette.text,
                onClick = { isImportingPlaylist = true }
            )

            HeaderIconButton(
                icon = R.drawable.share_social,
                color = colorPalette.text,
                onClick = {
                    val url = playlistPage?.playlist?.shareLink
                        ?: "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}"

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }

                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
            )
        }
    }

    val thumbnailContent = adaptiveThumbnailContent(
        isLoading = playlistPage == null,
        url = playlistPage?.playlist?.thumbnail
    )

    val lazyListState = rememberLazyListState()

    val (currentMediaId, playing) = playingSong(binder)

    LayoutWithAdaptiveThumbnail(
        thumbnailContent = thumbnailContent,
        modifier = modifier
    ) {
        Box {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        headerContent()
                        if (!isLandscape) thumbnailContent()
                        PlaylistInfo(playlist = playlistPage)
                    }
                }

                itemsIndexed(
                    items = playlistPage?.songs ?: emptyList(),
                    key = { _, song -> song.id }
                ) { index, song ->
                    SongItem(
                        song = song,
                        thumbnailSize = Dimensions.thumbnails.song,
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        NonQueuedMediaItemMenu(
                                            onDismiss = menuState::hide,
                                            mediaItem = song.asMediaItem()
                                        )
                                    }
                                },
                                onClick = {
                                    playlistPage?.songs?.map(InnertubeSongItem::asMediaItem)
                                        ?.let { mediaItems ->
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(mediaItems, index)
                                        }
                                }
                            ),
                        isPlaying = playing && currentMediaId == song.id
                    )
                }

                if (playlistPage == null) item(key = "loading") {
                    ShimmerHost(modifier = Modifier.fillParentMaxSize()) {
                        repeat(4) {
                            SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                        }
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    playlistPage?.songs?.let { songs ->
                        if (songs.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(
                                songs.shuffled().map(InnertubeSongItem::asMediaItem)
                            )
                        }
                    }
                }
            )
        }
    }
}

private fun InnertubeSongItem.toLocalSong(): Song {
    return Song(
        id = id,
        title = title,
        artistsText = artists.joinToString { it.name },
        durationText = duration?.let { formatAsDuration(it.toLong()) } ?: "0:00",
        thumbnailUrl = thumbnail,
        explicit = explicit
    )
}

private fun InnertubeSongItem.asMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artists.joinToString { it.name })
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail.toUri())
                .build()
        )
        .build()
}
