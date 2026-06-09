package eu.kanade.tachiyomi.ui.setting.gallery

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.orm.dao.TagFilterDao
import eu.kanade.tachiyomi.data.orm.models.DBTagFilter
import eu.kanade.tachiyomi.theme.GalleryTheme
import kotlinx.coroutines.launch

class AiTagFilterDialog(
    private val dao: TagFilterDao,
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ai_tag_filter_settings)
            .setView(
                ComposeView(requireContext()).apply {
                    setContent {
                        GalleryTheme {
                            val isDarkTheme = when (AppCompatDelegate.getDefaultNightMode()) {
                                AppCompatDelegate.MODE_NIGHT_YES -> true
                                AppCompatDelegate.MODE_NIGHT_NO -> false
                                else -> isSystemInDarkTheme() // You can define this function to check system theme preference if needed
                            }
                            ProvideTextStyle(
                                value = LocalTextStyle.current.copy(
                                    color = if (isDarkTheme) Color.White else Color.Black,
                                ),
                            ) {
                                Content(dao)
                            }
                        }
                    }
                },
            )
            .setNegativeButton(R.string.close, null)
            .create()
    }
}

@Composable
private fun Content(dao: TagFilterDao) {
    var searchQuery by remember { mutableStateOf("") }
    var addText by remember { mutableStateOf("") }
    var filterList by remember { mutableStateOf(emptyList<DBTagFilter>()) }
    val scope = rememberCoroutineScope()

    // Initial load
    LaunchedEffect(Unit) {
        filterList = dao.getAll()
    }

    // Search effect
    LaunchedEffect(searchQuery) {
        filterList = if (searchQuery.isBlank()) {
            dao.getAll()
        } else {
            dao.searchByName(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.ai_tag_filter_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            },
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        // List or empty state
        if (filterList.isEmpty()) {
            Text(
                text = stringResource(R.string.ai_tag_filter_empty),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            ) {
                items(filterList, key = { it.tagName }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.tagName,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        IconButton(onClick = {
                            scope.launch {
                                dao.deleteByName(item.tagName)
                                filterList = if (searchQuery.isBlank()) {
                                    dao.getAll()
                                } else {
                                    dao.searchByName(searchQuery)
                                }
                            }
                        },) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        OutlinedTextField(
            value = addText,
            onValueChange = { addText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text(stringResource(R.string.ai_tag_filter_add_hint)) },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        val name = addText.trim()
                        if (name.isNotBlank()) {
                            scope.launch {
                                dao.insert(DBTagFilter(tagName = name))
                                addText = ""
                                filterList = if (searchQuery.isBlank()) {
                                    dao.getAll()
                                } else {
                                    dao.searchByName(searchQuery)
                                }
                            }
                        }
                    },
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string._add))
                }
            },
        )
    }
}
