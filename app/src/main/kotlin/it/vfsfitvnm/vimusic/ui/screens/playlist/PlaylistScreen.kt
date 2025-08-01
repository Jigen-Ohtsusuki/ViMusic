package it.vfsfitvnm.vimusic.ui.screens.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.components.themed.Scaffold
import it.vfsfitvnm.vimusic.ui.screens.GlobalRoutes
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.compose.persist.PersistMapCleanup
import it.vfsfitvnm.compose.routing.RouteHandler

@Route
@Composable
fun PlaylistScreen(
    browseId: String,
    params: String?,
    shouldDedup: Boolean,
    maxDepth: Int? = null
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    PersistMapCleanup(prefix = "playlist/$browseId")

    RouteHandler {
        GlobalRoutes()

        Content {
            Scaffold(
                key = "playlist",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChange = { },
                tabColumnContent = {
                    tab(0, R.string.songs, R.drawable.musical_notes)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> PlaylistSongList(browseId = browseId)

                    }
                }
            }
        }
    }
}
