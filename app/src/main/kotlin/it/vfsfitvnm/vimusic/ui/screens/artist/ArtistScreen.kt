package it.vfsfitvnm.vimusic.ui.screens.artist

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.valentinilk.shimmer.shimmer
import it.vfsfitvnm.compose.persist.PersistMapCleanup
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.compose.routing.RouteHandler
import it.vfsfitvnm.core.ui.Dimensions
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.providers.innertube.YouTube
import it.vfsfitvnm.providers.innertube.models.AlbumItem as InnertubeAlbumItem
import it.vfsfitvnm.providers.innertube.models.SongItem as InnertubeSongItem
import it.vfsfitvnm.providers.innertube.requests.ArtistPage
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Artist
import it.vfsfitvnm.vimusic.preferences.UIStatePreferences
import it.vfsfitvnm.vimusic.preferences.UIStatePreferences.artistScreenTabIndexProperty
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.themed.*
import it.vfsfitvnm.vimusic.ui.items.AlbumItem
import it.vfsfitvnm.vimusic.ui.items.AlbumItemPlaceholder
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.items.SongItemPlaceholder
import it.vfsfitvnm.vimusic.ui.screens.GlobalRoutes
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.vimusic.ui.screens.albumRoute
import it.vfsfitvnm.vimusic.ui.screens.searchresult.ItemsPage
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.forcePlay
import it.vfsfitvnm.vimusic.utils.playingSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Route
@Composable
fun ArtistScreen(browseId: String) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(prefix = "artist/$browseId/")

    var artist by persist<Artist?>("artist/$browseId/artist")
    var artistPage by persist<ArtistPage?>("artist/$browseId/artistPage")

    LaunchedEffect(Unit) {
        Database.instance
            .artist(browseId)
            .combine(
                flow = artistScreenTabIndexProperty.stateFlow.map { it != 4 },
                transform = ::Pair
            )
            .distinctUntilChanged()
            .collect { (currentArtist, mustFetch) ->
                artist = currentArtist

                if (artistPage == null && (currentArtist?.timestamp == null || mustFetch))
                    withContext(Dispatchers.IO) {
                        YouTube.artist(browseId)
                            .onSuccess { currentArtistPage ->
                                artistPage = currentArtistPage

                                Database.instance.upsert(
                                    Artist(
                                        id = browseId,
                                        name = currentArtistPage.artist.title,
                                        thumbnailUrl = currentArtistPage.artist.thumbnail,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = currentArtist?.bookmarkedAt
                                    )
                                )
                            }
                    }
            }
    }

    RouteHandler {
        GlobalRoutes()

        Content {
            val (currentMediaId, playing) = playingSong(binder)

            val thumbnailContent = adaptiveThumbnailContent(
                isLoading = artist?.timestamp == null,
                url = artist?.thumbnailUrl,
                shape = CircleShape
            )

            val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit =
                { textButton ->
                    if (artist?.timestamp == null) HeaderPlaceholder(
                        modifier = Modifier.shimmer()
                    ) else {
                        val (colorPalette) = LocalAppearance.current
                        val context = LocalContext.current

                        Header(title = artist?.name ?: stringResource(R.string.unknown)) {
                            textButton?.invoke()

                            Spacer(modifier = Modifier.weight(1f))

                            HeaderIconButton(
                                icon = if (artist?.bookmarkedAt == null) R.drawable.bookmark_outline
                                else R.drawable.bookmark,
                                color = colorPalette.accent,
                                onClick = {
                                    val bookmarkedAt = if (artist?.bookmarkedAt == null)
                                        System.currentTimeMillis() else null

                                    query {
                                        artist
                                            ?.copy(bookmarkedAt = bookmarkedAt)
                                            ?.let(Database.instance::update)
                                    }
                                }
                            )

                            HeaderIconButton(
                                icon = R.drawable.share_social,
                                color = colorPalette.text,
                                onClick = {
                                    val url = artistPage?.artist?.shareLink ?: "https://music.youtube.com/channel/$browseId"
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
                }

            Scaffold(
                key = "artist",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = UIStatePreferences.artistScreenTabIndex,
                onTabChange = { UIStatePreferences.artistScreenTabIndex = it },
                tabColumnContent = {
                    tab(0, R.string.overview, R.drawable.sparkles)
                    tab(1, R.string.songs, R.drawable.musical_notes)
                    tab(2, R.string.albums, R.drawable.disc)
                    tab(3, R.string.singles, R.drawable.disc)
                    tab(4, R.string.library, R.drawable.library)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> ArtistOverview(
                            youtubeArtistPage = artistPage,
                            thumbnailContent = thumbnailContent,
                            headerContent = headerContent,
                            onAlbumClick = { albumRoute(it) },
                            onViewAllSongsClick = { UIStatePreferences.artistScreenTabIndex = 1 },
                            onViewAllAlbumsClick = { UIStatePreferences.artistScreenTabIndex = 2 },
                            onViewAllSinglesClick = { UIStatePreferences.artistScreenTabIndex = 3 }
                        )

                        1 -> {
                            // Replaced ItemsPage with a manual LazyColumn
                            val songsSection = artistPage?.sections?.find { it.title == "Songs" }
                            LazyColumn {
                                item { headerContent(null) }
                                if (songsSection?.items?.isNotEmpty() == true) {
                                    items(songsSection.items.filterIsInstance<InnertubeSongItem>()) { song ->
                                        SongItem(
                                            song = song,
                                            thumbnailSize = Dimensions.thumbnails.song,
                                            modifier = Modifier.combinedClickable(
                                                onLongClick = {
                                                    menuState.display {
                                                        NonQueuedMediaItemMenu(
                                                            onDismiss = menuState::hide,
                                                            mediaItem = song.asMediaItem()
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlay(song.asMediaItem())
                                                    binder?.setupRadio(song.endpoint)
                                                }
                                            ),
                                            isPlaying = playing && currentMediaId == song.id
                                        )
                                    }
                                } else if (artist?.timestamp != null) {
                                    item {
                                        Text(stringResource(R.string.artist_has_no_songs))
                                    }
                                } else {
                                    // Placeholder for when data is loading
                                    item {
                                        HeaderPlaceholder(modifier = Modifier.shimmer())
                                        repeat(6) {
                                            SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Replaced ItemsPage with a manual LazyColumn
                            val albumsSection = artistPage?.sections?.find { it.title == "Albums" }
                            LazyColumn {
                                item { headerContent(null) }
                                if (albumsSection?.items?.isNotEmpty() == true) {
                                    items(albumsSection.items.filterIsInstance<InnertubeAlbumItem>()) { album ->
                                        AlbumItem(
                                            album = album,
                                            thumbnailSize = Dimensions.thumbnails.album,
                                            modifier = Modifier.clickable { albumRoute(album.browseId) }
                                        )
                                    }
                                } else if (artist?.timestamp != null) {
                                    item {
                                        Text(stringResource(R.string.artist_has_no_albums))
                                    }
                                } else {
                                    // Placeholder for when data is loading
                                    item {
                                        HeaderPlaceholder(modifier = Modifier.shimmer())
                                        repeat(6) {
                                            AlbumItemPlaceholder(thumbnailSize = Dimensions.thumbnails.album)
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Replaced ItemsPage with a manual LazyColumn
                            val singlesSection = artistPage?.sections?.find { it.title == "Singles" }
                            LazyColumn {
                                item { headerContent(null) }
                                if (singlesSection?.items?.isNotEmpty() == true) {
                                    items(singlesSection.items.filterIsInstance<InnertubeAlbumItem>()) { album ->
                                        AlbumItem(
                                            album = album,
                                            thumbnailSize = Dimensions.thumbnails.album,
                                            modifier = Modifier.clickable { albumRoute(album.browseId) }
                                        )
                                    }
                                } else if (artist?.timestamp != null) {
                                    item {
                                        Text(stringResource(R.string.artist_has_no_singles))
                                    }
                                } else {
                                    // Placeholder for when data is loading
                                    item {
                                        HeaderPlaceholder(modifier = Modifier.shimmer())
                                        repeat(6) {
                                            AlbumItemPlaceholder(thumbnailSize = Dimensions.thumbnails.album)
                                        }
                                    }
                                }
                            }
                        }

                        4 -> ArtistLocalSongs(
                            browseId = browseId,
                            headerContent = headerContent,
                            thumbnailContent = thumbnailContent
                        )
                    }
                }
            }
        }
    }
}
