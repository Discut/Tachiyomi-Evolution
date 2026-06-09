---
change: ai-tag-filter
design-doc: docs/superpowers/specs/2026-06-09-ai-tag-filter-design.md
base-ref: ec689eddf73e0fddd82689815da7251878cecf15
---

# AI Tag Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Implement persistent AI tag filtering in the reader tag settings sheet and gallery settings, with Room-backed storage, settings dialog management, and long-press action mode for quick filter/unfilter.

**Architecture:** New `ai_tag_filters` Room table with `DBTagFilter` entity and `TagFilterDao`. Settings page uses Flow-driven summary. Reader sheet uses one-shot `getAll()` per prediction, then in-memory dual StateFlow (allResults + filtered results) with ViewModel-level recompute. Long-press ActionMode for filter/unfilter. Two visual dimensions: background color (adoption) + suffix text (filter status).

**Tech Stack:** Kotlin, Room, AndroidX, Material Components, Injekt DI, Coroutines/StateFlow

---

## Task 1: Create Room entity DBTagFilter

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/orm/models/DBTagFilter.kt`

- [x] **Step 1: Create the entity class**

Follow the `DBTag` pattern from `data/orm/models/DBTag.kt`:

```kotlin
package eu.kanade.tachiyomi.data.orm.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_tag_filters",
)
data class DBTagFilter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "tag_name")
    val tagName: String,

    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis() / 1000,
)
```

- [x] **Step 2: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/data/orm/models/DBTagFilter.kt
git commit -m "feat: add DBTagFilter entity for ai_tag_filters table"
```

## Task 2: Create table constants AITagFilterTable

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/database/tables/AITagFilterTable.kt`

- [x] **Step 1: Create the table constants object**

Follow the `TagTable` pattern from `data/database/tables/TagTable.kt`:

```kotlin
package eu.kanade.tachiyomi.data.database.tables

object AITagFilterTable {
    const val TABLE = "ai_tag_filters"
    const val TAG_NAME = "tag_name"
    const val CREATED_AT = "created_at"
}
```

- [x] **Step 2: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/data/database/tables/AITagFilterTable.kt
git commit -m "feat: add AITagFilterTable constants for ai_tag_filters table"
```

## Task 3: Create TagFilterDao

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/orm/dao/TagFilterDao.kt`

- [x] **Step 1: Create the DAO interface**

Follow the `TagDao` pattern from `data/orm/dao/TagDao.kt`:

```kotlin
package eu.kanade.tachiyomi.data.orm.dao

import androidx.room.Dao
import androidx.room.Query
import eu.kanade.tachiyomi.data.database.tables.AITagFilterTable
import eu.kanade.tachiyomi.data.orm.models.DBTagFilter
import kotlinx.coroutines.flow.Flow

@Dao
interface TagFilterDao : BaseDao<DBTagFilter> {

    @Query("SELECT * FROM ${AITagFilterTable.TABLE}")
    suspend fun getAll(): List<DBTagFilter>

    @Query("SELECT * FROM ${AITagFilterTable.TABLE}")
    fun getAllAsFlow(): Flow<List<DBTagFilter>>

    @Query("DELETE FROM ${AITagFilterTable.TABLE} WHERE ${AITagFilterTable.TAG_NAME} = :name")
    suspend fun deleteByName(name: String): Int

    @Query("SELECT * FROM ${AITagFilterTable.TABLE} WHERE ${AITagFilterTable.TAG_NAME} LIKE '%' || :query || '%'")
    suspend fun searchByName(query: String): List<DBTagFilter>
}
```

- [x] **Step 2: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/data/orm/dao/TagFilterDao.kt
git commit -m "feat: add TagFilterDao with getAll, getAllAsFlow, deleteByName, searchByName"
```

## Task 4: Register entity and DAO in GalleryDatabase, add migration v6→v7

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/data/orm/GalleryDatabase.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/data/orm/migrations/DatabaseMigration.kt`

- [x] **Step 1: Update GalleryDatabase — add import, entity, DAO method, bump version**

Modify `GalleryDatabase.kt`:

Add import:
```kotlin
import eu.kanade.tachiyomi.data.orm.dao.TagFilterDao
import eu.kanade.tachiyomi.data.orm.models.DBTagFilter
```

Add `DBTagFilter::class` to the entities array (after `DBDiffGroupImage::class`):
```kotlin
@Database(
    entities = [
        AnimationSequence::class,
        DBDiffGroup::class,
        DBImage::class,
        DBImageAndTag::class,
        DBTag::class,
        DBTagType::class,
        DBDiffGroupImage::class,
        DBTagFilter::class,
    ],
    version = 7,
    exportSchema = false,
)
```

Add new abstract DAO method after `getDiffGroupDao()`:
```kotlin
abstract fun getTagFilterDao(): TagFilterDao
```

- [x] **Step 2: Add v6→v7 migration**

Modify `DatabaseMigration.kt`. Add a comma after the existing v5→v6 migration block `})` and append:

```kotlin
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_tag_filters (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    tag_name TEXT NOT NULL UNIQUE,
                    created_at INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
        }
    },
```

Make sure this comes before the closing `)` of `migrationObjets` list.

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/data/orm/GalleryDatabase.kt
git add app/src/main/java/eu/kanade/tachiyomi/data/orm/migrations/DatabaseMigration.kt
git commit -m "feat: register DBTagFilter entity in GalleryDatabase v7 with migration v6→v7"
```

## Task 5: Add string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

- [x] **Step 1: Add English strings to values/strings.xml**

Append inside the `<resources>` block, near the existing tag-related strings:

```xml
<!-- AI Tag Filter -->
<string name="ai_tag_filter_settings">AI Tag Filter</string>
<string name="ai_tag_filter_summary">Filtered %d tags</string>
<string name="ai_tag_filtered_suffix"> (filtered)</string>
<string name="ai_tag_filter_add_hint">Enter tag name to filter</string>
<string name="ai_tag_filter_search_hint">Search filtered tags</string>
<string name="ai_tag_filter_empty">No filtered tags</string>
<string name="ai_tag_filter_action_filter">Filter</string>
<string name="ai_tag_filter_action_unfilter">Remove filter</string>
```

- [x] **Step 2: Add Chinese strings to values-zh-rCN/strings.xml**

Append inside the `<resources>` block:

```xml
<!-- AI Tag Filter -->
<string name="ai_tag_filter_settings">AI标签过滤</string>
<string name="ai_tag_filter_summary">已过滤 %d 个标签</string>
<string name="ai_tag_filtered_suffix">（已过滤）</string>
<string name="ai_tag_filter_add_hint">输入要过滤的标签名</string>
<string name="ai_tag_filter_search_hint">搜索过滤标签</string>
<string name="ai_tag_filter_empty">暂无过滤标签</string>
<string name="ai_tag_filter_action_filter">过滤</string>
<string name="ai_tag_filter_action_unfilter">取消过滤</string>
```

- [x] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git add app/src/main/res/values-zh-rCN/strings.xml
git commit -m "feat: add AI tag filter string resources (en + zh-rCN)"
```

## Task 6: Update AIPredictResult data class

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/sheet/AIPredictResult.kt`

- [x] **Step 1: Add isFiltered field**

Current file content:
```kotlin
package eu.kanade.tachiyomi.ui.reader.sheet

data class AIPredictResult(
    val tagName: String,
    val score: Float,
    val isAdopted: Boolean = false,
)

enum class AIPredictState { IDLE, LOADING, RESULTS, EMPTY, ERROR }
```

Replace with:
```kotlin
package eu.kanade.tachiyomi.ui.reader.sheet

data class AIPredictResult(
    val tagName: String,
    val score: Float,
    val isAdopted: Boolean = false,
    val isFiltered: Boolean = false,
)

enum class AIPredictState { IDLE, LOADING, RESULTS, EMPTY, ERROR }
```

- [x] **Step 2: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/ui/reader/sheet/AIPredictResult.kt
git commit -m "feat: add isFiltered field to AIPredictResult"
```

## Task 7: Update TagSettingsViewModel — filter logic and state management

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/sheet/TagSettingsViewModel.kt`

- [x] **Step 1: Add imports and inject TagFilterDao**

Add imports at top of file:
```kotlin
import eu.kanade.tachiyomi.data.orm.GalleryDatabase
import eu.kanade.tachiyomi.data.orm.dao.TagFilterDao
import eu.kanade.tachiyomi.data.orm.models.DBTagFilter
```

Add field injection alongside existing injectLazy calls:
```kotlin
private val galleryDatabase by injectLazy<GalleryDatabase>()
private val tagFilterDao: TagFilterDao get() = galleryDatabase.getTagFilterDao()
```

- [x] **Step 2: Add new state flows**

After the existing `aiPredictState` and `aiPredictResults` fields, add:
```kotlin
// AI tag filter: full results (with isFiltered markers) + show/hide toggle
val aiAllPredictResults = MutableStateFlow<List<AIPredictResult>>(emptyList())
val showFiltered = MutableStateFlow(false)
```

- [x] **Step 3: Modify predictTags() to apply filtering**

Replace the existing `predictTags()` method block. After `val predictions = wdTagger.predictAsync(bitmap)`, change the results section from:
```kotlin
            if (predictions.isEmpty()) {
                aiPredictResults.value = emptyList()
                aiPredictState.value = AIPredictState.EMPTY
            } else {
                aiPredictResults.value = predictions.map { (name, score) ->
                    AIPredictResult(
                        tagName = name,
                        score = score,
                        isAdopted = name.lowercase() in existingTagNames,
                    )
                }
                aiPredictState.value = AIPredictState.RESULTS
            }
```

To:
```kotlin
            if (predictions.isEmpty()) {
                aiAllPredictResults.value = emptyList()
                aiPredictResults.value = emptyList()
                aiPredictState.value = AIPredictState.EMPTY
            } else {
                val filterNames = tagFilterDao.getAll().map { it.tagName }.toSet()
                val allResults = predictions.map { (name, score) ->
                    AIPredictResult(
                        tagName = name,
                        score = score,
                        isAdopted = name.lowercase() in existingTagNames,
                        isFiltered = name in filterNames,
                    )
                }
                aiAllPredictResults.value = allResults
                recomputeAiPredictResults()
                aiPredictState.value = AIPredictState.RESULTS
            }
```

- [x] **Step 4: Add recompute and mutation methods**

Add after `adoptAITag()` method, before `filterTag()`:

```kotlin
    private fun recomputeAiPredictResults() {
        aiPredictResults.value = if (showFiltered.value) {
            aiAllPredictResults.value
        } else {
            aiAllPredictResults.value.filter { !it.isFiltered || it.isAdopted }
        }
    }

    fun addFilter(tagName: String) {
        val list = aiAllPredictResults.value.toMutableList()
        val idx = list.indexOfFirst { it.tagName == tagName }
        if (idx >= 0 && !list[idx].isAdopted) {
            list[idx] = list[idx].copy(isFiltered = true)
            aiAllPredictResults.value = list
            recomputeAiPredictResults()
        }
        scope.launch {
            tagFilterDao.insert(DBTagFilter(tagName = tagName))
        }
    }

    fun removeFilter(tagName: String) {
        val list = aiAllPredictResults.value.toMutableList()
        val idx = list.indexOfFirst { it.tagName == tagName }
        if (idx >= 0) {
            list[idx] = list[idx].copy(isFiltered = false)
            aiAllPredictResults.value = list
            recomputeAiPredictResults()
        }
        scope.launch {
            tagFilterDao.deleteByName(tagName)
        }
    }
```

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/ui/reader/sheet/TagSettingsViewModel.kt
git commit -m "feat: add AI tag filter logic to TagSettingsViewModel (dual StateFlow, addFilter/removeFilter)"
```

## Task 8: Update reader_tag_settings_sheet.xml layout

**Files:**
- Modify: `app/src/main/res/layout/reader_tag_settings_sheet.xml`

- [x] **Step 1: Insert stats section between AI button and ProgressBar**

Replace the section starting from `<!-- AI 智能打标区域 -->` through to the `<ProgressBar`. The current relevant lines are:
```xml
            <!-- AI 智能打标区域 -->
            <Button
                android:id="@+id/ai_tag_button"
                ...
            <ProgressBar
                android:id="@+id/ai_loading_progress"
```

Replace that segment with:
```xml
            <!-- AI 智能打标区域 -->
            <Button
                android:id="@+id/ai_tag_button"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="AI智能打标"
                style="@style/Widget.Material3.Button.TonalButton" />

            <!-- AI 标签统计 + 过滤切换 -->
            <LinearLayout
                android:id="@+id/ai_tag_stats_layout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:paddingVertical="8dp"
                android:visibility="gone"
                tools:visibility="visible">

                <TextView
                    android:id="@+id/ai_tag_stats_text"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:textAppearance="?attr/textAppearanceBodySmall"
                    tools:text="共识别 15 个标签，已过滤 3 个" />

                <Button
                    android:id="@+id/ai_filter_toggle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="显示已过滤"
                    style="@style/Widget.Material3.Button.TextButton" />
            </LinearLayout>

            <ProgressBar
                android:id="@+id/ai_loading_progress"
```

Leave the rest of the file (progress bar and RecyclerView) unchanged.

- [x] **Step 2: Commit**

```bash
git add app/src/main/res/layout/reader_tag_settings_sheet.xml
git commit -m "feat: add AI tag stats section and filter toggle to reader tag settings sheet layout"
```

## Task 9: Update AITagResultAdapter — visual states and long-click

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/sheet/AITagResultAdapter.kt`

- [x] **Step 1: Rewrite AITagResultAdapter with filter visual states and long-click**

The adapter needs to:
- Show `(filtered)` suffix on filtered items
- Apply correct background color and alpha per visual state matrix
- Set long-click listener for filter/unfilter ActionMode
- Pass long-click event to a callback

```kotlin
package eu.kanade.tachiyomi.ui.reader.sheet

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ItemAiTagResultBinding
import eu.kanade.tachiyomi.util.system.toast
import timber.log.Timber

class AITagResultAdapter(
    val tagClickedListener: (result: AIPredictResult) -> Unit,
    val tagLongClickListener: ((result: AIPredictResult, view: View) -> Unit)? = null,
) : RecyclerView.Adapter<AITagResultAdapter.VH>() {

    private var results: List<AIPredictResult> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newResults: List<AIPredictResult>) {
        results = newResults
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ItemAiTagResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val result = results[position]
        val ctx = holder.binding.root.context

        // Suffix for filtered tags
        val suffix = if (result.isFiltered) ctx.getString(R.string.ai_tag_filtered_suffix) else ""
        holder.binding.tagName.text = result.tagName + suffix

        holder.binding.confidenceText.text = "${(result.score * 100).toInt()}%"

        // Progress bar width
        holder.binding.root.post {
            val parentWidth = holder.binding.root.width
            if (parentWidth > 0) {
                val fillWidth = (parentWidth * result.score).toInt()
                (holder.binding.progressFill.layoutParams as? FrameLayout.LayoutParams)?.apply {
                    width = fillWidth
                }
                holder.binding.progressFill.requestLayout()
            }
        }

        // Visual states: background color = adoption, suffix = filter
        if (result.isAdopted) {
            holder.binding.progressFill.setBackgroundColor(0x33888888.toInt())
            holder.binding.root.alpha = 0.4f
            holder.binding.root.isClickable = false
        } else {
            holder.binding.progressFill.setBackgroundColor(0x3300BFA5.toInt())
            holder.binding.root.alpha = if (result.isFiltered) 0.6f else 1f
            holder.binding.root.isClickable = true
            holder.binding.root.setOnClickListener {
                onAITagClicked(result, holder.binding)
            }
        }

        // Long-click: show filter/unfilter ActionMode (adopted+unfiltered excluded)
        val canLongPress = !result.isAdopted || result.isFiltered
        holder.binding.root.isLongClickable = canLongPress
        if (canLongPress) {
            holder.binding.root.setOnLongClickListener { view ->
                tagLongClickListener?.invoke(result, view)
                true
            }
        } else {
            holder.binding.root.setOnLongClickListener(null)
        }
    }

    override fun getItemCount(): Int = results.size

    private fun onAITagClicked(result: AIPredictResult, itemBinding: ItemAiTagResultBinding) {
        try {
            tagClickedListener.invoke(result)
            itemBinding.progressFill.setBackgroundColor(0x33888888.toInt())
            itemBinding.root.alpha = 0.4f
            itemBinding.root.isClickable = false
        } catch (e: Exception) {
            Timber.e(e, "采纳AI标签失败")
            itemBinding.root.context.toast("采纳失败: ${e.message}")
        }
    }

    class VH(val binding: ItemAiTagResultBinding) : RecyclerView.ViewHolder(binding.root)
}
```

- [x] **Step 2: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/ui/reader/sheet/AITagResultAdapter.kt
git commit -m "feat: add filter visual states (suffix, colors) and long-click listener to AITagResultAdapter"
```

## Task 10: Create ActionMode menu XML

**Files:**
- Create: `app/src/main/res/menu/ai_tag_filter_actions.xml`

- [x] **Step 1: Create the menu file**

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/action_filter"
        android:title="@string/ai_tag_filter_action_filter" />
    <item
        android:id="@+id/action_unfilter"
        android:title="@string/ai_tag_filter_action_unfilter" />
    <item
        android:id="@+id/action_copy_tag_name"
        android:title="@string/_copy_to_clipboard" />
</menu>
```

- [x] **Step 2: Commit**

```bash
git add app/src/main/res/menu/ai_tag_filter_actions.xml
git commit -m "feat: add AI tag filter ActionMode menu (filter, unfilter, copy)"
```

## Task 11: Integrate filter UI into TagSettingsSheet

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/sheet/TagSettingsSheet.kt`

- [x] **Step 1: Update aiTagAdapter creation to pass long-click callback**

In the `aiTagAdapter` lazy property, change:
```kotlin
    private val aiTagAdapter by lazy {
        AITagResultAdapter { result ->
            scope.launchIO {
                viewModel.adoptAITag(result)
            }
        }
    }
```

To:
```kotlin
    private val aiTagAdapter by lazy {
        AITagResultAdapter(
            tagClickedListener = { result ->
                scope.launchIO {
                    viewModel.adoptAITag(result)
                }
            },
            tagLongClickListener = { result, view ->
                showAITagActionMode(result, view)
            },
        )
    }
```

- [x] **Step 2: Update showAIResults() to also recompute filtered list**

Replace:
```kotlin
    private fun showAIResults(results: List<AIPredictResult>) {
        binding.aiLoadingProgress.visibility = View.GONE
        binding.aiTagButton.isEnabled = true
        binding.aiTagResultsRecycler.visibility = View.VISIBLE
        aiTagAdapter.submitList(results.toList())
    }
```

With:
```kotlin
    private fun showAIResults(results: List<AIPredictResult>) {
        binding.aiLoadingProgress.visibility = View.GONE
        binding.aiTagButton.isEnabled = true
        binding.aiTagResultsRecycler.visibility = View.VISIBLE
        aiTagAdapter.submitList(results.toList())
        updateAIStats()
    }
```

- [x] **Step 3: Add stats observation in setupAITagging()**

In `setupAITagging()`, after the existing `aiPredictState` observer (around line 265), add:

```kotlin
        // Observe all results for stats count
        scope.launch {
            viewModel.aiAllPredictResults.collectLatest { all ->
                updateAIStats()
            }
        }

        // Observe showFiltered toggle for button text
        scope.launch {
            viewModel.showFiltered.collectLatest { showing ->
                binding.aiFilterToggle.text = if (showing)
                    "隐藏已过滤" else "显示已过滤"
            }
        }
```

- [x] **Step 4: Add updateAIStats() helper method**

Add this method after `showAIResults()`:

```kotlin
    private fun updateAIStats() {
        val all = viewModel.aiAllPredictResults.value
        if (all.isEmpty()) return
        val total = all.size
        val filtered = all.count { it.isFiltered }
        binding.aiTagStatsText.text = "共识别 $total 个标签，已过滤 $filtered 个"
        binding.aiTagStatsLayout.visibility = View.VISIBLE
    }
```

- [x] **Step 5: Add filter toggle click listener in setupAITagging()**

After the stats observer blocks, add:

```kotlin
        binding.aiFilterToggle.setOnClickListener {
            viewModel.showFiltered.value = !viewModel.showFiltered.value
            viewModel.recomputeAiPredictResults()
            aiTagAdapter.submitList(viewModel.aiPredictResults.value.toList())
        }
```

- [x] **Step 6: Add showAITagActionMode() method and inner callback class**

Add after `finishFloatingActionMode()` method, before the companion object:

```kotlin
    private fun showAITagActionMode(result: AIPredictResult, view: View) {
        finishFloatingActionMode()
        floatingActionMode = view.startActionMode(
            AITagFilterActionModeCallback(result),
            ActionMode.TYPE_FLOATING,
        )
    }

    inner class AITagFilterActionModeCallback(
        private val result: AIPredictResult,
    ) : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.ai_tag_filter_actions, menu)
            // Menu visibility based on state
            menu?.findItem(R.id.action_filter)?.isVisible = !result.isFiltered && !result.isAdopted
            menu?.findItem(R.id.action_unfilter)?.isVisible = result.isFiltered
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.action_filter -> {
                    scope.launchIO {
                        viewModel.addFilter(result.tagName)
                    }
                }
                R.id.action_unfilter -> {
                    scope.launchIO {
                        viewModel.removeFilter(result.tagName)
                    }
                }
                R.id.action_copy_tag_name -> {
                    copyToClipboard(result.tagName, result.tagName, true, binding.root)
                }
            }
            mode?.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {}
    }
```

- [x] **Step 7: Update state observer to hide stats on loading/idle**

In the existing `aiPredictState` observer, modify the `AIPredictState.IDLE` and `AIPredictState.LOADING` branches to hide stats:

For `AIPredictState.IDLE`:
```kotlin
                    AIPredictState.IDLE -> { // no-op
                        binding.aiTagStatsLayout.visibility = View.GONE
                    }
```

For `AIPredictState.LOADING`:
```kotlin
                    AIPredictState.LOADING -> {
                        showAILoading()
                        binding.aiTagButton.isEnabled = false
                        binding.aiTagStatsLayout.visibility = View.GONE
                    }
```

- [x] **Step 8: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/ui/reader/sheet/TagSettingsSheet.kt
git commit -m "feat: integrate AI tag filter UI into TagSettingsSheet (stats, toggle, long-press ActionMode)"
```

## Task 12: Create Dialog layout for AI tag filter management

**Files:**
- Create: `app/src/main/res/layout/dialog_ai_tag_filter.xml`

- [x] **Step 1: Create the dialog layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <EditText
        android:id="@+id/search_edit_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/ai_tag_filter_search_hint"
        android:inputType="text"
        android:drawableStart="@drawable/ic_search_24dp"
        android:drawablePadding="8dp" />

    <TextView
        android:id="@+id/empty_hint"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:gravity="center"
        android:text="@string/ai_tag_filter_empty"
        android:textAppearance="?attr/textAppearanceBodyMedium"
        android:visibility="gone"
        tools:visibility="visible" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/filter_list_recycler"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="8dp"
        tools:listitem="@layout/item_ai_tag_filter" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <EditText
            android:id="@+id/add_edit_text"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="@string/ai_tag_filter_add_hint"
            android:inputType="text" />

        <Button
            android:id="@+id/add_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/_add"
            style="@style/Widget.Material3.Button.TextButton" />
    </LinearLayout>
</LinearLayout>
```

- [x] **Step 2: Create item layout for filter list**

Create `app/src/main/res/layout/item_ai_tag_filter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingVertical="8dp"
    android:paddingHorizontal="4dp">

    <TextView
        android:id="@+id/tag_name"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textAppearance="?attr/textAppearanceBodyMedium"
        tools:text="少女" />

    <ImageButton
        android:id="@+id/delete_button"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="删除"
        android:src="@drawable/ic_close_24dp"
        android:tint="?attr/colorOnSurface" />
</LinearLayout>
```

- [x] **Step 3: Commit**

```bash
git add app/src/main/res/layout/dialog_ai_tag_filter.xml
git add app/src/main/res/layout/item_ai_tag_filter.xml
git commit -m "feat: add AI tag filter dialog layout and list item layout"
```

## Task 13: Create AITagFilterAdapter for settings dialog

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/ui/setting/AITagFilterAdapter.kt`

- [x] **Step 1: Create the adapter**

```kotlin
package eu.kanade.tachiyomi.ui.setting

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.data.orm.models.DBTagFilter
import eu.kanade.tachiyomi.databinding.ItemAiTagFilterBinding

class AITagFilterAdapter(
    private val onDeleteClick: (DBTagFilter) -> Unit,
) : RecyclerView.Adapter<AITagFilterAdapter.VH>() {

    private var items: List<DBTagFilter> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<DBTagFilter>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAiTagFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tagName.text = item.tagName
        holder.binding.deleteButton.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemAiTagFilterBinding) : RecyclerView.ViewHolder(binding.root)
}
```

- [x] **Step 2: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/ui/setting/AITagFilterAdapter.kt
git commit -m "feat: add AITagFilterAdapter for settings dialog RecyclerView"
```

## Task 14: Replace placeholder preference in SettingsGalleryController

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/setting/SettingsGalleryController.kt`

- [x] **Step 1: Add imports**

Add at top of imports:
```kotlin
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.preference.Preference
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.orm.GalleryDatabase
import eu.kanade.tachiyomi.data.orm.models.DBTagFilter
import eu.kanade.tachiyomi.databinding.DialogAiTagFilterBinding
import kotlinx.coroutines.launch
```

- [x] **Step 2: Add field for database access**

After the existing `private val galleryManager: GalleryManager by injectLazy()`:
```kotlin
    private val galleryDatabase: GalleryDatabase by injectLazy()
```

- [x] **Step 3: Replace placeholder preference**

Replace the block starting at:
```kotlin
        preferenceCategory {
            titleRes = R.string.tag_settings

            preference {
                key = "gallery_tag_filter"

            }
        }
```

With:
```kotlin
        preferenceCategory {
            titleRes = R.string.tag_settings

            preference {
                key = "gallery_tag_filter"
                titleRes = R.string.ai_tag_filter_settings
                summaryRes = R.string.ai_tag_filter_summary

                onClick {
                    showAITagFilterDialog()
                }
            }

            // Flow-driven summary update
            viewScope.launch {
                galleryDatabase.getTagFilterDao().getAllAsFlow().collect { list ->
                    findPreference<Preference>("gallery_tag_filter")?.summary =
                        activity?.getString(R.string.ai_tag_filter_summary, list.size)
                }
            }
        }
```

- [x] **Step 4: Add showAITagFilterDialog() method**

Add before the existing `private fun customDirectorySelected()` method:

```kotlin
    private fun showAITagFilterDialog() {
        val context = activity ?: return
        val binding = DialogAiTagFilterBinding.inflate(LayoutInflater.from(context))
        val dao = galleryDatabase.getTagFilterDao()
        val adapter = AITagFilterAdapter { item ->
            viewScope.launch {
                dao.deleteByName(item.tagName)
                refreshFilterList(dao, adapter, binding)
            }
        }

        binding.filterListRecycler.layoutManager = LinearLayoutManager(context)
        binding.filterListRecycler.adapter = adapter

        // Initial load
        viewScope.launch {
            refreshFilterList(dao, adapter, binding)
        }

        // Search
        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            viewScope.launch {
                val query = text?.toString() ?: ""
                if (query.isBlank()) {
                    refreshFilterList(dao, adapter, binding)
                } else {
                    val results = dao.searchByName(query)
                    adapter.submitList(results)
                    binding.emptyHint.visibility =
                        if (results.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // Add button
        binding.addButton.setOnClickListener {
            val name = binding.addEditText.text?.toString()?.trim() ?: ""
            if (name.isNotBlank()) {
                viewScope.launch {
                    dao.insert(DBTagFilter(tagName = name))
                    binding.addEditText.text?.clear()
                    refreshFilterList(dao, adapter, binding)
                }
            }
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.ai_tag_filter_settings)
            .setView(binding.root)
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private suspend fun refreshFilterList(
        dao: eu.kanade.tachiyomi.data.orm.dao.TagFilterDao,
        adapter: AITagFilterAdapter,
        binding: DialogAiTagFilterBinding,
    ) {
        val list = dao.getAll()
        adapter.submitList(list)
        binding.emptyHint.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }
```

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/ui/setting/SettingsGalleryController.kt
git commit -m "feat: implement AI tag filter settings preference with management dialog"
```

## Task 15: Final verification — build and check

**Files:** None (verification only)

- [x] **Step 1: Build the project**

```bash
./gradlew assembleStandardDebug
```
Expected: BUILD SUCCESSFUL with no compilation errors.

- [x] **Step 2: Verify OpenSpec tasks are all marked done**

Update `openspec/changes/ai-tag-filter/tasks.md` to mark all checkboxes as `[x]`.

- [x] **Step 3: Commit final task status**

```bash
git add openspec/changes/ai-tag-filter/tasks.md
git commit -m "chore: mark all ai-tag-filter tasks as complete"
```
