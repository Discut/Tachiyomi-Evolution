## Context

当前 `TagSettingsSheet` 中的AI打标功能会把WD14模型输出的所有标签（置信度>0.35）全部展示在RecyclerView中，用户只能逐个点击采纳或忽略。`SettingsGalleryController` 中的 `gallery_tag_filter` preference 已是占位状态。需要引入持久化的AI标签过滤机制，让用户可以管理一个过滤黑名单，在任意AI打标结果展示中自动隐藏被过滤的标签。

### 约束
- Room数据库版本从6升级到7
- 过滤基于标签的中文名称（`tagName`），不是tag_id
- 设置页和阅读器底部面板都要能管理过滤列表
- 过虑列表中的标签名与模型输出的 `nameCn` 精确匹配

## Goals / Non-Goals

**Goals:**
- Room持久化存储用户过滤的AI标签名列表
- 设置页提供过滤列表的增删查管理Dialog
- AI打标结果列表自动隐藏已过滤标签，并提供切换显示开关
- 统计显示：总标签数、已过滤数
- 在AI结果列表长按item可快速加入过滤

**Non-Goals:**
- 不支持正则/通配符过滤，仅精确名称匹配
- 不修改 `GalleryManager` 的核心接口签名
- 已采纳且未过滤的标签不可触发"过滤"操作（但已采纳且已过滤的标签可"取消过滤"）

## Decisions

### Decision 1: 使用独立 `ai_tag_filters` 表而非复用 `tags` 表

**选择**：新建 `ai_tag_filters` 表，包含 `id` (INTEGER PK AUTOINCREMENT)、`tag_name` (TEXT UNIQUE NOT NULL)、`created_at` (INTEGER NOT NULL DEFAULT 0)。以 `id` 为主键，`tag_name` 加 UNIQUE 约束防重复。

**理由**：过滤的是模型输出的标签名称，不是数据库中实际创建的 `tags` 记录。模型输出的是中文标签名（如"少女"、"風景"），而 `tags` 表中存的是被采纳后创建的 `TagBo` 记录。两者生命周期不同：过滤在采纳之前。

**替代方案**：在 `tags` 表添加 `is_filtered` 列 — 但过滤是模型输出层面的概念，与数据库标签实体耦合不合适。

### Decision 2: DAO直接注入，不通过GalleryManager委托

**选择**：`SettingsGalleryController` 和 `TagSettingsViewModel` 通过 Injekt 直接注入 `GalleryDatabase` 获取 `TagFilterDao`。

**理由**：过滤操作是简单的CRUD，不需要GalleryManager的业务逻辑层。避免在GalleryManager上增加不必要的接口膨胀。

### Decision 3: 过滤逻辑在ViewModel层做，不在Adapter/UI层

**选择**：在 `TagSettingsViewModel.predictTags()` 中读取过滤列表，将过滤后的结果发给 `aiPredictResults`。额外维护一个 `allAiPredictResults` (未过滤全量) 用于切换时恢复。

**理由**：ViewModel是数据持有者，状态切换应在此层完成。Adapter只负责展示，不应关心过滤逻辑。

### Decision 4: 设置页Dialog使用Compose DialogFragment

**选择**：提取为独立的 `AiTagFilterDialog` 类（`ui/setting/gallery/`），继承 `DialogFragment`，使用 `ComposeView` + `GalleryTheme` 渲染UI，遵循 `GalleryDirSettingsDialog` 的模式。状态通过 Compose 的 `mutableStateOf` + `rememberCoroutineScope` 管理，无需额外 ViewModel。

**理由**：复用项目已有的 Compose DialogFragment 模式（`GalleryDirSettingsDialog`），减少4个文件（adapter + 2个XML布局 + controller内联方法）为1个独立类，状态管理更直观（Compose 自动重组 vs 手动调 adapter.submitList）。

### Decision 5: 长按弹出浮动ActionMode替代右滑手势

**选择**：在AI打标结果item上使用长按（`setOnLongClickListener`），弹出 `ActionMode.TYPE_FLOATING` 操作浮层，提供"过滤/取消过滤"菜单项。

**理由**：避免 ItemTouchHelper 横向滑动与 BottomSheet + 外层 NestedScrollView + RecyclerView 内嵌的三层手势冲突。长按交互与项目中已存在的 `FloatingImageTagActionModeCallback`（TagSettingsSheet.kt:304）模式完全一致，可复用标准 `ActionMode` 模式，零手势冲突，且用户对Chip标签的长按操作已有心智模型。

### Decision 6: 双维度视觉区分：背景色=采纳状态，后缀=过滤状态

**选择**：AI标签item使用两个正交视觉维度区分状态。

| | 未采纳（背景绿色 #3300BFA5） | 已采纳（背景灰色 #33888888） |
|---|---|---|
| **未过滤** | `少女` | `少女` (alpha=0.4) |
| **已过滤** | `少女（已过滤）` | `少女（已过滤）` (alpha=0.4) |

- **背景色** = 采纳状态（决定是否可点击采纳）
- **后缀文案** = 过滤状态（来自 `R.string.ai_tag_filtered_suffix`，支持中英文）
- 已过滤状态下的 item alpha=0.6（非采纳）或 0.4（已采纳）

**理由**：两个状态维度在语义上正交，用不同视觉通道表达避免混淆。后缀文案走 `strings.xml` 保证国际化。

### Decision 7: Sheet 持续观察 aiPredictResults 而非一次性读取

**选择**：在 `setupAITagging()` 中新增 `viewModel.aiPredictResults.collectLatest` 观察器，过滤/采纳状态变化时自动更新 Adapter。

**理由**：原方案只在 `RESULTS` 状态读一次，之后的内存 mutation（addFilter/removeFilter/updateAdoptionState）无法传递到 UI。Flow 持续观察能保证数据一致性。

### Decision 8: 关联/取消关联标签时同步 AI 结果的 isAdopted 状态

**选择**：ViewModel 新增 `updateAdoptionState(tagName, isAdopted)` 方法，`clickTag` 在关联/取消关联后调用。

**理由**：AI 打标列表和 chip 标签组是两个独立系统，需要显式同步。`adoptAITag` 只处理"采纳"方向，缺少反向操作。

### Decision 9: DiffUtil 替代 notifyDataSetChanged

**选择**：`AITagResultAdapter.submitList()` 使用 `DiffUtil.calculateDiff` + `dispatchUpdatesTo`，基于 `tagName` 做 item 比对。

**理由**：`notifyDataSetChanged()` 强制全量重绘，无法产生 item 级别的增删改动画。DiffUtil 支持局部更新，默认 `DefaultItemAnimator` 提供 fade/move 动画。

### Decision 10: REFRESHING 状态避免再次打标时列表清空

**选择**：`AIPredictState` 新增 `REFRESHING` 状态。有旧数据时使用 `REFRESHING`（只显示进度条，不隐藏 RecyclerView），无旧数据时使用 `LOADING`（全屏 loading）。

**理由**：用户体验上，旧数据在新数据到达前保持可见比清空后等待更好。避免了 item 突然消失又出现的闪烁感。

## Risks / Trade-offs

- **[数据一致性]** 过滤列表只存标签名（文本），如果模型更新后标签名变更，旧的过滤项会自动失效（不会误过滤新标签），但用户需重新添加 → 可接受，WD14标签名相对稳定
- **[性能]** 每次打标都查询过滤列表 → 过滤列表预计<100条，查询开销可忽略
- **[UI复杂度]** 在已拥挤的底部面板中添加统计区和切换按钮 → 合理布局，统计信息在"AI智能打标"按钮下方、RecyclerView上方，占用约48dp高度

## Migration Plan

1. GalleryDatabase版本 6 → 7
2. Migration 6→7: `CREATE TABLE IF NOT EXISTS ai_tag_filters (id INTEGER PRIMARY KEY AUTOINCREMENT, tag_name TEXT UNIQUE NOT NULL, created_at INTEGER NOT NULL DEFAULT 0)`
3. 无需数据迁移（新表，无历史数据需处理）
4. 无需回滚策略（新增表，回滚只需降级version number）
