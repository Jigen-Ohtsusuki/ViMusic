package it.vfsfitvnm.vimusic.ui.screens.searchresult

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.providers.innertube.models.YTItem
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.components.ShimmerHost
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.secondary
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias ItemsProvider<T> = suspend (continuation: String?) -> Pair<List<T>, String?>?

@Composable
fun <T : YTItem> ItemsPage(
    tag: String,
    header: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
    itemPlaceholderContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initialPlaceholderCount: Int = 8,
    continuationPlaceholderCount: Int = 3,
    emptyItemsText: String = stringResource(R.string.no_items_found),
    provider: ItemsProvider<T>? = null
) {
    val (_, typography) = LocalAppearance.current
    val updatedProvider by rememberUpdatedState(provider)
    val lazyListState = rememberLazyListState()

    var items by persistList<T>("$tag/items")
    var continuation by persist<String?>("$tag/continuation")
    var isInitialized by persist("$tag/isInitialized", false)

    val shouldLoad by remember {
        derivedStateOf {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }
    }

    LaunchedEffect(shouldLoad, updatedProvider) {
        if (!shouldLoad || (continuation == null && isInitialized)) return@LaunchedEffect
        val provideItems = updatedProvider ?: return@LaunchedEffect

        withContext(Dispatchers.IO) {
            provideItems(continuation)
        }?.let { (newItems, newContinuation) ->
            items = (items + newItems).toImmutableList()
            continuation = newContinuation
            isInitialized = true
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = "header"
            ) {
                header(null)
            }

            items(
                items = items,
                key = { it.id },
                itemContent = itemContent
            )

            if (isInitialized && items.isEmpty()) item(key = "empty") {
                BasicText(
                    text = emptyItemsText,
                    style = typography.xs.secondary.center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                        .fillMaxWidth()
                )
            }

            if (continuation != null || !isInitialized) item(key = "loading") {
                val isFirstLoad = !isInitialized

                ShimmerHost(
                    modifier = if (isFirstLoad) Modifier.fillParentMaxSize() else Modifier
                ) {
                    repeat(if (isFirstLoad) initialPlaceholderCount else continuationPlaceholderCount) {
                        itemPlaceholderContent()
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
    }
}
