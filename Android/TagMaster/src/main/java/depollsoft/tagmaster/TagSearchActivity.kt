package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions
import depollsoft.tagmaster.ui.BarberPoleWatermark
import depollsoft.tagmaster.ui.DropdownField
import depollsoft.tagmaster.ui.FieldIcon
import depollsoft.tagmaster.ui.OutlinedField
import depollsoft.tagmaster.ui.SearchFab
import depollsoft.tagmaster.ui.TagMasterTheme
import depollsoft.tagmaster.ui.TagMasterTopBar
import depollsoft.tagmaster.ui.TagMasterType
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.setTagMasterContent

/** The search form: text, sort order and the catalog filters, which open [TagSearchResultsActivity]. */
class TagSearchActivity : AppCompatActivity() {
    var model: QueryModel =
        QueryModel().apply {
            maxResults = Integer.MAX_VALUE
            sortBy = TagSortOptions.Title
        }
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(TagSearchResultsActivity.QUERY_MODEL)?.let { saved ->
            model = JsonSerializer.deserialize(org.json.JSONObject(saved)) as QueryModel
        }
        setTagMasterContent { SearchScreen(model, onSearch = ::search, onNavigateUp = { navigateUpOrHome() }) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(TagSearchResultsActivity.QUERY_MODEL, JsonSerializer.serialize(model).toString())
        super.onSaveInstanceState(outState)
    }

    fun search() {
        startActivity(
            Intent(this, TagSearchResultsActivity::class.java)
                .putExtra(TagSearchResultsActivity.QUERY_MODEL, JsonSerializer.serialize(model).toString()),
        )
    }
}

private val booleanChoices = listOf(null, true, false)
private val partChoices = listOf(null, 3, 4, 5, 6, 7, 8)
private val collectionChoices = listOf(null, TagCollection.ClassicTags, TagCollection.EasyTags)
private val sortChoices =
    listOf(TagSortOptions.Title, TagSortOptions.Downloaded, TagSortOptions.Posted, TagSortOptions.Rating, TagSortOptions.Classic)

@Composable
private fun SearchScreen(
    model: QueryModel,
    onSearch: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    val colors = TagMasterTheme.colors
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val query = model.query.orEmpty()
        mutableStateOf(TextFieldValue(query, TextRange(query.length)))
    }
    Column(Modifier.fillMaxSize()) {
        TagMasterTopBar(title = stringResource(R.string.app_name), brandTitle = true, onNavigateUp = onNavigateUp)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            BarberPoleWatermark()
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    OutlinedField(
                        label = stringResource(R.string.SearchBoxHint),
                        value = text,
                        onValueChange = {
                            text = it
                            model.query = it.text
                        },
                        startIcon = FieldIcon(R.drawable.ic_search),
                        endIcon =
                            if (text.text.isNotEmpty()) {
                                FieldIcon(
                                    com.google.android.material.R.drawable.mtrl_ic_cancel,
                                    stringResource(com.google.android.material.R.string.clear_text_end_icon_content_description),
                                ) {
                                    text = TextFieldValue("")
                                    model.query = ""
                                }
                            } else {
                                null
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        fieldModifier =
                            Modifier
                                .onPreviewKeyEvent {
                                    if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) {
                                        onSearch()
                                        true
                                    } else {
                                        false
                                    }
                                }.testTag("searchTextBox"),
                    )
                    Text(
                        stringResource(R.string.SearchOptions),
                        Modifier
                            .padding(top = 24.dp)
                            .semantics { heading() },
                        style = TagMasterType.titleLarge,
                        color = colors.text,
                    )
                    Choice(R.string.SortBy, R.array.SortByChoices, sortChoices.indexOf(model.sortBy), "sortBySpinner") {
                        model.sortBy = sortChoices[it]
                    }
                    Choice(R.string.SheetMusicSentence, R.array.SheetMusicChoices, booleanChoices.indexOf(model.hasSheetMusic), "sheetMusicSpinner") {
                        model.hasSheetMusic = booleanChoices[it]
                    }
                    Choice(
                        R.string.LearningTracks,
                        R.array.LearningTracksChoices,
                        booleanChoices.indexOf(model.hasLearningTracks),
                        "learningTracksSpinner",
                    ) { model.hasLearningTracks = booleanChoices[it] }
                    Choice(R.string.Parts, R.array.PartsChoices, partChoices.indexOf(model.parts), "partsSpinner") {
                        model.parts = partChoices[it]
                    }
                    Choice(
                        R.string.TagCollectionSentence,
                        R.array.TagCollectionChoices,
                        collectionChoices.indexOf(model.collection),
                        "tagCollectionSpinner",
                    ) { model.collection = collectionChoices[it] }
                }
                // The button sits under the form, 8dp below it; SearchFab keeps its own end and
                // bottom margins.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    SearchFab(onSearch)
                }
            }
        }
    }
}

@Composable
private fun Choice(
    label: Int,
    choices: Int,
    selected: Int,
    tag: String,
    onSelect: (Int) -> Unit,
) {
    DropdownField(
        label = stringResource(label),
        choices = stringArrayResource(choices).toList(),
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.padding(top = 8.dp),
        tag = tag,
    )
}
