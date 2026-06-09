## 1. 数据库层：新建实体、DAO和迁移

- [x] 1.1 创建 `DBTagFilter` 实体类 (`data/orm/models/DBTagFilter.kt`)：`id` (INTEGER PK AUTOINCREMENT)、`tag_name` (TEXT UNIQUE NOT NULL)、`created_at` (INTEGER NOT NULL DEFAULT 0)
- [x] 1.2 创建表常量 `AITagFilterTable` (`data/database/tables/AITagFilterTable.kt`)
- [x] 1.3 创建 `TagFilterDao` 接口 (`data/orm/dao/TagFilterDao.kt`)，继承 `BaseDao<DBTagFilter>`，添加 `getAll()`、`getAllAsFlow()`、`deleteByName(name)`、`searchByName(name)` 查询方法
- [x] 1.4 在 `GalleryDatabase` 注册 `DBTagFilter` 实体，版本升级到 7，添加 `getTagFilterDao()` 抽象方法
- [x] 1.5 在 `DatabaseMigration.kt` 添加 v6→v7 迁移：创建 `ai_tag_filters` 表

## 2. 设置页面：AI标签过滤管理入口和Dialog

- [x] 2.1 替换 `SettingsGalleryController` 中 `gallery_tag_filter` 占位preference为完整实现：添加 title、summary，点击弹出过滤管理Dialog
- [x] 2.2 创建过滤管理Dialog布局 (`dialog_ai_tag_filter.xml`)：顶部搜索EditText、中间RecyclerView、底部输入区（EditText + 添加按钮）
- [x] 2.3 创建过滤管理Dialog适配器 (`AITagFilterAdapter.kt`)：展示标签名及删除按钮，支持列表更新
- [x] 2.4 实现Dialog逻辑（在 `SettingsGalleryController` 中或新建Dialog类）：增删查过滤标签，搜索过滤，空列表提示，实时更新summary

## 3. AI打标UI：过滤集成、统计信息和快捷操作

- [x] 3.1 在 `reader_tag_settings_sheet.xml` 中：在AI按钮和RecyclerView之间添加统计文本 (`ai_tag_stats`) 和过滤切换按钮 (`ai_filter_toggle`)
- [x] 3.2 修改 `TagSettingsViewModel`：注入 `TagFilterDao`，`predictTags()` 完成后应用过滤逻辑，新增 `aiAllPredictResults`（全量）和 `showFiltered` 状态流
- [x] 3.3 修改 `TagSettingsSheet.setupAITagging()`：观察并展示统计信息（总标签数/已过滤数），切换按钮控制过滤显示
- [x] 3.4 修改 `AITagResultAdapter`：添加 `isFiltered` 显示状态，为item设置长按监听弹出浮动ActionMode（过滤/取消过滤）
- [x] 3.5 在 `TagSettingsSheet` 中创建 AI 标签的 FloatingActionModeCallback（参考现有 `FloatingImageTagActionModeCallback`），处理"过滤"和"取消过滤"操作
- [x] 3.6 更新 `AIPredictResult` 数据类，添加 `isFiltered: Boolean = false` 字段

## 4. 字符串和最终验证

- [x] 4.1 在 `strings.xml` 中添加所需的字符串资源（过滤按钮文本、统计文本模板、提示信息等）
- [x] 4.2 手动验证：打标后过滤标签不显示、切换开关正常、设置页Dialog增删查正常、重启后过滤数据持久化

## 5. Compose Dialog 重构

- [x] 5.1 创建 `AiTagFilterDialog` Compose DialogFragment (`ui/setting/gallery/AiTagFilterDialog.kt`)，替代 XML 布局 + RecyclerView + AITagFilterAdapter
- [x] 5.2 更新 `SettingsGalleryController.showAITagFilterDialog()` 使用新的 Compose Dialog
- [x] 5.3 删除 `AITagFilterAdapter.kt`、`dialog_ai_tag_filter.xml`、`item_ai_tag_filter.xml`

## 6. 实现后修复

- [x] 6.1 修复过滤/取消过滤后 AI 列表不更新：Sheet 新增 `aiPredictResults.collectLatest` 持续观察器
- [x] 6.2 修复关联/取消关联标签不同步到 AI 列表：ViewModel 新增 `updateAdoptionState()`，`clickTag` 中调用同步
- [x] 6.3 DiffUtil 替代 `notifyDataSetChanged`：`AITagResultAdapter.submitList()` 使用 DiffUtil + dispatchUpdatesTo
- [x] 6.4 REFRESHING 状态：再次打标时保留旧数据，只显示进度条，新数据到达后覆盖
