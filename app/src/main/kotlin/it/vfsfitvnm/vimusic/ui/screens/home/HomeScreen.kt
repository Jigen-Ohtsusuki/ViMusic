package it.vfsfitvnm.vimusic.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import it.vfsfitvnm.compose.persist.PersistMapCleanup
import it.vfsfitvnm.compose.routing.Route0
import it.vfsfitvnm.compose.routing.RouteHandler
import it.vfsfitvnm.providers.innertube.models.AlbumItem
import it.vfsfitvnm.providers.innertube.models.ArtistItem
import it.vfsfitvnm.providers.innertube.models.PlaylistItem
import it.vfsfitvnm.providers.innertube.requests.MoodAndGenres
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.toUiMood
import it.vfsfitvnm.vimusic.preferences.UIStatePreferences
import it.vfsfitvnm.vimusic.ui.components.themed.Scaffold
import it.vfsfitvnm.vimusic.ui.screens.*
import it.vfsfitvnm.vimusic.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import it.vfsfitvnm.vimusic.ui.screens.localplaylist.LocalPlaylistScreen
import it.vfsfitvnm.vimusic.ui.screens.mood.MoodScreen
import it.vfsfitvnm.vimusic.ui.screens.mood.MoreAlbumsScreen
import it.vfsfitvnm.vimusic.ui.screens.mood.MoreMoodsScreen

private val moreMoodsRoute = Route0("moreMoodsRoute")
private val moreAlbumsRoute = Route0("moreAlbumsRoute")

@Route
@Composable
fun HomeScreen() {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup("home/")

    RouteHandler {
        GlobalRoutes()

        localPlaylistRoute { playlistId ->
            LocalPlaylistScreen(playlistId = playlistId)
        }

        builtInPlaylistRoute { builtInPlaylist ->
            BuiltInPlaylistScreen(builtInPlaylist = builtInPlaylist)
        }

        moodRoute { mood ->
            MoodScreen(mood = mood)
        }

        moreMoodsRoute {
            MoreMoodsScreen()
        }

        moreAlbumsRoute {
            MoreAlbumsScreen()
        }

        Content {
            Scaffold(
                key = "home",
                topIconButtonId = R.drawable.equalizer,
                onTopIconButtonClick = { settingsRoute() },
                tabIndex = UIStatePreferences.homeScreenTabIndex,
                onTabChange = { UIStatePreferences.homeScreenTabIndex = it },
                tabColumnContent = {
                    tab(0, R.string.quick_picks, R.drawable.sparkles)
                    tab(1, R.string.discover, R.drawable.globe)
                    tab(2, R.string.songs, R.drawable.musical_notes)
                    tab(3, R.string.playlists, R.drawable.playlist)
                    tab(4, R.string.artists, R.drawable.person)
                    tab(5, R.string.albums, R.drawable.disc)
                    tab(6, R.string.local, R.drawable.download)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    val onSearchClick = { searchRoute("") }
                    when (currentTabIndex) {
                        0 -> QuickPicks(
                            onAlbumClick = { album: AlbumItem -> albumRoute(album.browseId) },
                            onArtistClick = { artist: ArtistItem -> artistRoute(artist.id) },
                            onPlaylistClick = { playlist: PlaylistItem ->
                                // Correctly calling playlistRoute with all four arguments.
                                playlistRoute(
                                    p0 = playlist.id,
                                    p1 = null,
                                    p2 = null,
                                    p3 = playlist.author?.name == "YouTube Music"
                                )
                            },
                            onSearchClick = onSearchClick
                        )

                        1 -> HomeDiscovery(
                            onMoodClick = { mood: MoodAndGenres.Item -> moodRoute(mood.toUiMood()) },
                            onNewReleaseAlbumClick = { albumId -> albumRoute(albumId) },
                            onSearchClick = onSearchClick,
                            onMoreMoodsClick = { moreMoodsRoute() },
                            onMoreAlbumsClick = { moreAlbumsRoute() }
                        )

                        2 -> HomeSongs(
                            onSearchClick = onSearchClick
                        )

                        3 -> HomePlaylists(
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onPipedPlaylistClick = { session, playlist ->
                                pipedPlaylistRoute(
                                    p0 = session.apiBaseUrl.toString(),
                                    p1 = session.token,
                                    p2 = playlist.id.toString()
                                )
                            },
                            onSearchClick = onSearchClick
                        )

                        4 -> HomeArtistList(
                            onArtistClick = { artistRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        5 -> HomeAlbums(
                            onAlbumClick = { albumRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        6 -> HomeLocalSongs(
                            onSearchClick = onSearchClick
                        )
                    }
                }
            }
        }
    }
}
