package it.vfsfitvnm.vimusic.ui.screens.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.http.Url
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.providers.innertube.YouTube
import it.vfsfitvnm.providers.innertube.models.SearchSuggestions
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.SearchQuery
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.utils.align
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.disabled
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.secondary
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@Composable
fun OnlineSearch(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit,
    focused: Boolean,
    modifier: Modifier = Modifier
) = Box(modifier = modifier) {
    val (colorPalette, typography) = LocalAppearance.current

    var history by persistList<SearchQuery>("search/online/history")
    var suggestionsResult by persist<Result<SearchSuggestions>?>("search/online/suggestionsResult")

    LaunchedEffect(textFieldValue.text) {
        if (DataPreferences.pauseSearchHistory) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            Database.instance.queries("%${textFieldValue.text}%")
                .distinctUntilChanged { old, new -> old.size == new.size }
                .collect { history = it.toImmutableList() }
        }
    }

    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.isEmpty()) {
            suggestionsResult = null
            return@LaunchedEffect
        }

        delay(500)
        withContext(Dispatchers.IO) {
            suggestionsResult = YouTube.searchSuggestions(textFieldValue.text)
        }
    }

    val playlistId = remember(textFieldValue.text) {
        runCatching {
            Url(textFieldValue.text).takeIf {
                it.host.endsWith("youtube.com", ignoreCase = true) &&
                    it.segments.lastOrNull()?.equals("playlist", ignoreCase = true) == true
            }?.parameters?.get("list")
        }.getOrNull()
    }

    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
            .asPaddingValues(),
        modifier = Modifier.fillMaxSize()
    ) {
        item(
            key = "header",
            contentType = 0
        ) {
            val container = LocalPinnableContainer.current

            DisposableEffect(Unit) {
                val handle = container?.pin()

                onDispose {
                    handle?.release()
                }
            }

            LaunchedEffect(focused) {
                if (!focused) return@LaunchedEffect

                delay(300)
                focusRequester.requestFocus()
            }

            Header(
                titleContent = {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = onTextFieldValueChange,
                        textStyle = typography.xxl.medium.align(TextAlign.End),
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (textFieldValue.text.isNotEmpty()) onSearch(textFieldValue.text)
                            }
                        ),
                        cursorBrush = SolidColor(colorPalette.text),
                        decorationBox = decorationBox,
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                },
                actionsContent = {
                    if (playlistId != null) {
                        val isAlbum = playlistId.startsWith("OLAK5uy_")

                        SecondaryTextButton(
                            text = if (isAlbum) stringResource(R.string.view_album)
                            else stringResource(R.string.view_playlist),
                            onClick = { onViewPlaylist(textFieldValue.text) }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (textFieldValue.text.isNotEmpty()) SecondaryTextButton(
                        text = stringResource(R.string.clear),
                        onClick = { onTextFieldValueChange(TextFieldValue()) }
                    )
                }
            )
        }

        items(
            items = history,
            key = SearchQuery::id
        ) { searchQuery ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onSearch(searchQuery.query) }
                    .fillMaxWidth()
                    .padding(all = 16.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(20.dp)
                        .paint(
                            painter = painterResource(R.drawable.time),
                            colorFilter = ColorFilter.disabled
                        )
                )

                BasicText(
                    text = searchQuery.query,
                    style = typography.s.secondary,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .weight(1f)
                )

                Image(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    colorFilter = ColorFilter.disabled,
                    modifier = Modifier
                        .clickable(
                            indication = ripple(bounded = false),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                query {
                                    Database.instance.delete(searchQuery)
                                }
                            }
                        )
                        .padding(horizontal = 8.dp)
                        .size(20.dp)
                )

                Image(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    colorFilter = ColorFilter.disabled,
                    modifier = Modifier
                        .clickable(
                            indication = ripple(bounded = false),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                onTextFieldValueChange(
                                    TextFieldValue(
                                        text = searchQuery.query,
                                        selection = TextRange(searchQuery.query.length)
                                    )
                                )
                            }
                        )
                        .rotate(225f)
                        .padding(horizontal = 8.dp)
                        .size(22.dp)
                )
            }
        }

        suggestionsResult?.getOrNull()?.queries?.let { suggestions ->
            items(items = suggestions) { suggestion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onSearch(suggestion) }
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {
                    Spacer(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(20.dp)
                            .paint(
                                painter = painterResource(R.drawable.search),
                                colorFilter = ColorFilter.disabled
                            )
                    )

                    BasicText(
                        text = suggestion,
                        style = typography.s.secondary,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .weight(1f)
                    )

                    Image(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        colorFilter = ColorFilter.disabled,
                        modifier = Modifier
                            .clickable(
                                indication = ripple(bounded = false),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    onTextFieldValueChange(
                                        TextFieldValue(
                                            text = suggestion,
                                            selection = TextRange(suggestion.length)
                                        )
                                    )
                                }
                            )
                            .rotate(225f)
                            .padding(horizontal = 8.dp)
                            .size(22.dp)
                    )
                }
            }
        } ?: suggestionsResult?.exceptionOrNull()?.let {
            item {
                Box(modifier = Modifier.fillMaxSize()) {
                    BasicText(
                        text = stringResource(R.string.error_message),
                        style = typography.s.secondary.center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
}
