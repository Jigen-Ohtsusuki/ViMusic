package it.vfsfitvnm.vimusic.ui.screens.album

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.valentinilk.shimmer.shimmer
import it.vfsfitvnm.compose.persist.PersistMapCleanup
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.compose.routing.RouteHandler
import it.vfsfitvnm.core.ui.Dimensions
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.core.ui.utils.stateFlowSaver
import it.vfsfitvnm.providers.innertube.YouTube
import it.vfsfitvnm.providers.innertube.models.AlbumItem as InnertubeAlbumItem
import it.vfsfitvnm.providers.innertube.models.SongItem as InnertubeSongItem
import it.vfsfitvnm.providers.innertube.requests.AlbumPage
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Album
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.models.SongAlbumMap
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.transaction
import it.vfsfitvnm.vimusic.ui.components.themed.*
import it.vfsfitvnm.vimusic.ui.items.AlbumItem
import it.vfsfitvnm.vimusic.ui.items.AlbumItemPlaceholder
import it.vfsfitvnm.vimusic.ui.screens.GlobalRoutes
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.vimusic.ui.screens.albumRoute
import it.vfsfitvnm.vimusic.ui.screens.searchresult.ItemsPage
import it.vfsfitvnm.vimusic.utils.formatAsDuration
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

fun InnertubeSongItem.toSong(): Song {
    return Song(
        id = this.id,
        title = this.title,
        artistsText = this.artists.joinToString { it.name },
        durationText = this.duration?.let { formatAsDuration(it.toLong()) },
        thumbnailUrl = this.thumbnail,
        explicit = this.explicit,
    )
}

@Route
@Composable
fun AlbumScreen(browseId: String) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val tabIndexState = rememberSaveable(saver = stateFlowSaver()) { MutableStateFlow(0) }
    val tabIndex by tabIndexState.collectAsState()

    var album by persist<Album?>("album/$browseId/album")
    var albumPage by persist<AlbumPage?>("album/$browseId/albumPage")
    var songs by persistList<Song>("album/$browseId/songs")

    PersistMapCleanup(prefix = "album/$browseId/")

    LaunchedEffect(Unit) {
        Database.instance
            .albumSongs(browseId)
            .distinctUntilChanged()
            .combine(
                Database.instance
                    .album(browseId)
                    .distinctUntilChanged()
                    .cancellable()
            ) { currentSongs, currentAlbum ->
                album = currentAlbum
                songs = currentSongs.toImmutableList()

                if (currentAlbum?.timestamp != null && currentSongs.isNotEmpty()) return@combine

                withContext(Dispatchers.IO) {
                    YouTube.album(browseId = browseId)
                        .onSuccess { newAlbumPage ->
                            albumPage = newAlbumPage

                            transaction {
                                Database.instance.clearAlbum(browseId)

                                Database.instance.upsert(
                                    album = Album(
                                        id = browseId,
                                        title = newAlbumPage.album.title,
                                        description = null,
                                        thumbnailUrl = newAlbumPage.album.thumbnail,
                                        year = newAlbumPage.album.year?.toString(),
                                        authorsText = newAlbumPage.album.artists
                                            ?.joinToString("") { it.name },
                                        shareUrl = newAlbumPage.album.shareLink,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = album?.bookmarkedAt,
                                        otherInfo = null
                                    ),
                                    songAlbumMaps = newAlbumPage
                                        .songs
                                        .map { it.toSong() }
                                        .onEach { Database.instance.insert(it) }
                                        .mapIndexed { position, localSong ->
                                            SongAlbumMap(
                                                songId = localSong.id,
                                                albumId = browseId,
                                                position = position
                                            )
                                        }
                                )
                            }
                        }.exceptionOrNull()?.printStackTrace()
                }
            }.collect()
    }

    RouteHandler {
        GlobalRoutes()

        Content {
            val headerContent: @Composable (
                beforeContent: (@Composable () -> Unit)?,
                afterContent: (@Composable () -> Unit)?
            ) -> Unit = { beforeContent, afterContent ->
                if (album?.timestamp == null) HeaderPlaceholder(modifier = Modifier.shimmer())
                else {
                    val (colorPalette) = LocalAppearance.current
                    val context = LocalContext.current

                    Header(title = album?.title ?: stringResource(R.string.unknown)) {
                        beforeContent?.invoke()

                        Spacer(modifier = Modifier.weight(1f))

                        afterContent?.invoke()

                        HeaderIconButton(
                            icon = if (album?.bookmarkedAt == null) R.drawable.bookmark_outline
                            else R.drawable.bookmark,
                            color = colorPalette.accent,
                            onClick = {
                                val bookmarkedAt =
                                    if (album?.bookmarkedAt == null) System.currentTimeMillis() else null

                                query {
                                    album
                                        ?.copy(bookmarkedAt = bookmarkedAt)
                                        ?.let(Database.instance::update)
                                }
                            }
                        )

                        HeaderIconButton(
                            icon = R.drawable.share_social,
                            color = colorPalette.text,
                            onClick = {
                                album?.shareUrl?.let { url ->
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, url)
                                    }

                                    context.startActivity(
                                        Intent.createChooser(sendIntent, null)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            val thumbnailContent = adaptiveThumbnailContent(
                isLoading = album?.timestamp == null,
                url = album?.thumbnailUrl
            )

            Scaffold(
                key = "album",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChange = { newTab -> tabIndexState.update { newTab } },
                tabColumnContent = {
                    tab(0, R.string.songs, R.drawable.musical_notes, canHide = false)
                    tab(1, R.string.other_versions, R.drawable.disc)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> AlbumSongs(
                            songs = songs,
                            headerContent = headerContent,
                            thumbnailContent = thumbnailContent,
                            afterHeaderContent = {
                                if (album == null) PlaylistInfo(playlist = null as Album?)
                                else PlaylistInfo(playlist = album)
                            }
                        )

                        1 -> {
                            // Replaced ItemsPage with a manual check and LazyRow.
                            if (albumPage?.otherVersions?.isNotEmpty() == true) {
                                LazyRow {
                                    items(albumPage!!.otherVersions) { item: InnertubeAlbumItem ->
                                        AlbumItem(
                                            album = item,
                                            thumbnailSize = Dimensions.thumbnails.album,
                                            modifier = Modifier.clickable { albumRoute(item.browseId) }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_alternative_version)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
