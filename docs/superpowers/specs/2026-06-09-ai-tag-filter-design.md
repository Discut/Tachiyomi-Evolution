---
change: ai-tag-filter
role: technical-design
canonical_spec: openspec
---

# AI Tag Filter — Technical Design

## 1. Overview

在 `TagSettingsSheet` 的 AI 打标结果中引入持久化标签过滤机制。用户可管理一个过滤黑名单（持久化到 Room `ai_tag_filters` 表），打标结果自动隐藏已过滤标签，并通过长按浮层、设置页 Dialog 等多种入口管理过滤列表。

## 2. Architecture

### 2.1 Component Diagram

```
SettingsGalleryController          TagSettingsSheet
       │                                 │
       │  Flow<List<DBTagFilter>>        │  getAll() (one-shot)
       ▼                                 ▼
  AiTagFilterDialog ◄── TagFilterDao ◄── TagSettingsViewModel
  (Compose DialogFragment)       │        │
       │                         │        │  aiAllPredictResults (全量)
       │  rememberCoroutineScope │        │  aiPredictResults (裁剪后)
       │                         │        ▼
       │  dao.insert/delete      │   AITagResultAdapter
       │                         │        │
       ▼                         ▼        ▼
  ai_tag_filters (Room v7)    RecyclerView
  DatabaseMigration           (背景色 + 后缀 + 长按浮层)
  (v6→v7: CREATE TABLE)
```

### 2.2 Data Flow

```
1. 打标: ViewModel.predictTags()
   → WDTagger.predictAsync() → List<Pair<String, Float>>
   → TagFilterDao.getAll() → Set<String> (filter set)
   → 生成 aiAllPredictResults (全量, isFiltered 已标记)
   → recomputeAiPredictResults() 裁剪 → aiPredictResults
   → Sheet 观察两个流分别驱动 RecyclerView 和统计文本

2. 长按过滤: ActionMode → addFilter(tagName)
   → 内存: aiAllPredictResults 更新标记, recompute
   → DB: 异步 insert DBTagFilter

3. 设置页增删: TagFilterDao.insert/deleteByName
   → Flow 自动更新 preference summary
   → 下次打标生效 (ViewModel 使用一次性 getAll)
```

## 3. Database Layer

### 3.1 Entity

```kotlin
@Entity(tableName = "ai_tag_filters")
data class DBTagFilter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "tag_name")
    val tagName: String,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis() / 1000,
)
```

| Field | Type | Constraint |
|-------|------|------------|
| `id` | INTEGER | PK, AUTOINCREMENT |
| `tag_name` | TEXT | UNIQUE, NOT NULL |
| `created_at` | INTEGER | NOT NULL, DEFAULT 0 |

### 3.2 Table Constant

```kotlin
object AITagFilterTable {
    const val TABLE = "ai_tag_filters"
    const val TAG_NAME = "tag_name"
    const val CREATED_AT = "created_at"
}
```

### 3.3 DAO

```kotlin
@Dao
interface TagFilterDao : BaseDao<DBTagFilter> {
    @Query("SELECT * FROM $TABLE")
    suspend fun getAll(): List<DBTagFilter>

    @Query("SELECT * FROM $TABLE")
    fun getAllAsFlow(): Flow<List<DBTagFilter>>

    @Query("DELETE FROM $TABLE WHERE $TAG_NAME = :name")
    suspend fun deleteByName(name: String): Int

    @Query("SELECT * FROM $TABLE WHERE $TAG_NAME LIKE '%' || :query || '%'")
    suspend fun searchByName(query: String): List<DBTagFilter>
}
```

Inherits `insert/delete/update/insertAll` from `BaseDao<DBTagFilter>`.

### 3.4 Migration (v6 → v7)

```sql
CREATE TABLE IF NOT EXISTS ai_tag_filters (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    tag_name TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL DEFAULT 0
);
```

### 3.5 GalleryDatabase Changes

- `version = 7`
- entities array: append `DBTagFilter::class`
- new method: `abstract fun getTagFilterDao(): TagFilterDao`
- No AppModule changes needed — consummers inject `GalleryDatabase` directly.

## 4. Settings Page

### 4.1 SettingsGalleryController

Replace the placeholder `gallery_tag_filter` preference with full implementation.

```kotlin
preference {
    key = "gallery_tag_filter"
    titleRes = R.string.ai_tag_filter_settings
    summary = "..." // initial placeholder
    onClick { showFilterDialog() }
}

// Flow-driven summary
viewScope.launch {
    db.getTagFilterDao().getAllAsFlow().collect { list ->
        findPreference<Preference>("gallery_tag_filter")?.summary =
            context.getString(R.string.ai_tag_filter_summary, list.size)
    }
}
```

### 4.2 Filter Management Dialog

Implemented as `AiTagFilterDialog` — a Compose `DialogFragment` following the `GalleryDirSettingsDialog` pattern.

**File**: `ui/setting/gallery/AiTagFilterDialog.kt`

```
class AiTagFilterDialog(private val dao: TagFilterDao) : DialogFragment()

override fun onCreateDialog(): Dialog =
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.ai_tag_filter_settings)
        .setView(ComposeView { GalleryTheme { Content(dao) } })
        .setNegativeButton(R.string.close)
        .create()
```

**`@Composable Content(dao)` — state management:**

| State | Type | Purpose |
|-------|------|---------|
| `searchQuery` | `mutableStateOf("")` | Search input text |
| `addText` | `mutableStateOf("")` | Add input text |
| `filterList` | `mutableStateOf(emptyList<DBTagFilter>())` | Current list |
| `scope` | `rememberCoroutineScope()` | DB write operations |

| Action | Implementation |
|--------|----------------|
| Load | `LaunchedEffect(Unit)` → `dao.getAll()` |
| Search | `LaunchedEffect(searchQuery)` → `dao.searchByName()` or `dao.getAll()` |
| Add | `OutlinedIconButton.onClick` → `dao.insert(DBTagFilter)` → refresh |
| Delete | `IconButton.onClick` → `dao.deleteByName()` → refresh |

**UI structure**: `Column` → search `OutlinedTextField` + `LazyColumn` (320dp height) / empty hint + `HorizontalDivider` + add row (`OutlinedTextField` + `OutlinedIconButton`)

## 5. AI Tagging Sheet UI

### 5.1 Layout Changes

New section inserted between AI button and RecyclerView in `reader_tag_settings_sheet.xml`:

```xml
<LinearLayout android:id="@+id/ai_tag_stats_layout" android:visibility="gone">
    <TextView android:id="@+id/ai_tag_stats_text"
        android:layout_width="0dp" android:layout_weight="1" />
    <Button android:id="@+id/ai_filter_toggle"
        android:layout_width="wrap_content"
        style="@style/Widget.Material3.Button.TextButton" />
</LinearLayout>
```

Summary text left-aligned, toggle button right-aligned. Hidden when not in RESULTS state.

### 5.2 ViewModel: State Management

```kotlin
// New state flows
val aiAllPredictResults = MutableStateFlow<List<AIPredictResult>>(emptyList())
val showFiltered = MutableStateFlow(false)

// In predictTags():
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

// recompute: filter out filtered items unless showFiltered=true
// Exception: adopted items always shown regardless of filter status
private fun recomputeAiPredictResults() {
    aiPredictResults.value = if (showFiltered.value)
        aiAllPredictResults.value
    else
        aiAllPredictResults.value.filter { !it.isFiltered || it.isAdopted }
}

// Mutation: memory instant, DB async
fun addFilter(tagName: String) { ... }
fun removeFilter(tagName: String) { ... }
```

### 5.3 Visual States

Two orthogonal visual dimensions:

| | Not Adopted (green bg `#3300BFA5`) | Adopted (gray bg `#33888888`) |
|---|---|---|
| **Not filtered** | `tagName`, α=1 | `tagName`, α=0.4 |
| **Filtered** | `tagName（已过滤）`, α=0.6 | `tagName（已过滤）`, α=0.4 |

- Background color = adoption state (determines clickability)
- Suffix text = filter state (from `R.string.ai_tag_filtered_suffix`)

### 5.4 Long-Press Action Mode

```kotlin
inner class AITagFilterActionModeCallback(
    private val result: AIPredictResult,
) : ActionMode.Callback {
    // Inflate R.menu.ai_tag_filter_actions
    // Menu item visibility:
    //   action_filter: visible when !isFiltered && !isAdopted
    //   action_unfilter: visible when isFiltered
    //   action_copy_tag_name: always visible
}
```

Menu file `res/menu/ai_tag_filter_actions.xml`:
```xml
<menu>
    <item android:id="@+id/action_filter" />
    <item android:id="@+id/action_unfilter" />
    <item android:id="@+id/action_copy_tag_name" />
</menu>
```

## 6. String Resources

| Key | English | Chinese |
|-----|---------|---------|
| `ai_tag_filter_settings` | AI Tag Filter | AI标签过滤 |
| `ai_tag_filter_summary` | Filtered %d tags | 已过滤 %d 个标签 |
| `ai_tag_filtered_suffix` | (filtered) | （已过滤） |
| `ai_tag_filter_add_hint` | Enter tag name to filter | 输入要过滤的标签名 |
| `ai_tag_filter_search_hint` | Search filtered tags | 搜索过滤标签 |
| `ai_tag_filter_empty` | No filtered tags | 暂无过滤标签 |
| `ai_tag_filter_action_filter` | Filter | 过滤 |
| `ai_tag_filter_action_unfilter` | Remove filter | 取消过滤 |

## 7. Files Changed

| File | Action | Description |
|------|--------|-------------|
| `data/orm/models/DBTagFilter.kt` | **New** | Room entity |
| `data/database/tables/AITagFilterTable.kt` | **New** | Table constants |
| `data/orm/dao/TagFilterDao.kt` | **New** | DAO with CRUD + search + Flow |
| `data/orm/GalleryDatabase.kt` | **Modify** | v7, entity + DAO registration |
| `data/orm/migrations/DatabaseMigration.kt` | **Modify** | v6→v7 migration |
| `SettingsGalleryController.kt` | **Modify** | Replace placeholder, Flow summary, show AiTagFilterDialog |
| `ui/setting/gallery/AiTagFilterDialog.kt` | **New** | Compose DialogFragment (replaces XML + Adapter) |
| `res/layout/reader_tag_settings_sheet.xml` | **Modify** | Stats area + toggle button |
| `TagSettingsViewModel.kt` | **Modify** | Dual flows, filter logic, mutation methods |
| `AIPredictResult.kt` | **Modify** | Add `isFiltered` field |
| `AITagResultAdapter.kt` | **Modify** | Suffix, colors, long-click listener |
| `TagSettingsSheet.kt` | **Modify** | Stats observation, toggle, action mode |
| `res/menu/ai_tag_filter_actions.xml` | **New** | Action mode menu |
| `values/strings.xml` | **Modify** | 8 new strings |
| `values-zh-rCN/strings.xml` | **Modify** | 8 Chinese translations |

## 8. Key Design Decisions

1. **Independent `ai_tag_filters` table** — filter names are model-output concepts, distinct from adopted `tags` records.
2. **Auto-increment `id` PK** — matches project entity conventions, enables future column additions without PK migration.
3. **`tag_name` UNIQUE constraint** — prevents duplicate entries at DB level.
4. **DAO direct injection** — simple CRUD doesn't warrant a GalleryManager delegate interface.
5. **Filter logic in ViewModel** — dual StateFlows, Adapter is passive.
6. **Long-press ActionMode** — avoids gesture conflicts with BottomSheet + NestedScrollView.
7. **Two orthogonal visual dimensions** — background color for adoption, suffix text for filter status.
8. **Flow for settings, one-shot for sheet** — settings page needs live updates; sheet reads once per prediction and mutates in memory.

## 9. Post-Implementation Fixes

### 9.1 Filter/Unfilter Not Updating AI Tag List UI

**Problem**: Tapping "过滤/取消过滤" in the floating ActionMode updated the DB and ViewModel's `aiAllPredictResults`, but the RecyclerView didn't reflect the change.

**Root cause**: The Sheet only read `aiPredictResults` once when state transitioned to `RESULTS`. No continuous observation existed for in-memory mutations.

**Fix**: Added a `collectLatest` observer on `viewModel.aiPredictResults` in `setupAITagging()`:
```kotlin
scope.launch {
    viewModel.aiPredictResults.collectLatest { results ->
        if (viewModel.aiPredictState.value == AIPredictState.RESULTS ||
            viewModel.aiPredictState.value == AIPredictState.REFRESHING) {
            aiTagAdapter.submitList(results)
            updateAIStats()
        }
    }
}
```

### 9.2 Tag Association/Disassociation Not Syncing to AI Tag List

**Problem**: Adopting an AI tag (click → `relatedImageAndTag`) updated `isAdopted = true` in the AI list. But disassociating from the chip group didn't revert `isAdopted` to `false`.

**Root cause**: `clickTag` called `unrelatedImageAndTag` but had no code to update the AI results' `isAdopted` state.

**Fix**: Added `TagSettingsViewModel.updateAdoptionState(tagName, isAdopted)` and called it from `clickTag`:
```kotlin
// In clickTag:
scope.launchIO {
    viewModel.unrelatedImageAndTag(imageBO, tagVo)
    viewModel.updateAdoptionState(tagVo.tagValue, false)  // sync AI list
}
```

### 9.3 DiffUtil for Smooth List Animations

**Problem**: AI tag list items popped in abruptly when the list was loaded or updated.

**Root cause**: `AITagResultAdapter.submitList()` used `notifyDataSetChanged()`, which forces a full re-render with no item animation.

**Fix**: Replaced with `DiffUtil.calculateDiff` + `dispatchUpdatesTo`:
```kotlin
fun submitList(newResults: List<AIPredictResult>) {
    val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun getOldListSize() = results.size
        override fun getNewListSize() = newResults.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            results[oldPos].tagName == newResults[newPos].tagName
        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            results[oldPos] == newResults[newPos]
    })
    results = newResults
    diff.dispatchUpdatesTo(this)
}
```

### 9.4 Overlay Old Results on Re-Prediction

**Problem**: Tapping the predict button again cleared the list immediately, causing a jarring empty → loading → results transition.

**Root cause**: `predictTags()` always set state to `LOADING`, and `showAILoading()` hid the RecyclerView.

**Fix**: Added `REFRESHING` state to `AIPredictState`. When existing results are present, use `REFRESHING` instead of `LOADING`:
```kotlin
// ViewModel:
val hasExistingResults = aiAllPredictResults.value.isNotEmpty()
aiPredictState.value = if (hasExistingResults) AIPredictState.REFRESHING else AIPredictState.LOADING

// Sheet: REFRESHING keeps RecyclerView visible, only shows progress bar
AIPredictState.REFRESHING -> {
    binding.aiLoadingProgress.visibility = View.VISIBLE
    binding.aiTagButton.isEnabled = false
}
```
