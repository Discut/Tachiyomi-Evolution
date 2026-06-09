## Why

AI自动打标功能已经能让用户通过WD14模型分析图片并获取标签列表，但每次打标都会显示所有模型输出的标签（包括用户不关心的、不希望看到的标签），用户无法持久化过滤这些标签。这导致每次AI打标后用户都需要手动忽略大量无关标签，降低了打标效率和使用体验。

## What Changes

- **新增AI标签过滤持久化存储**：通过Room数据库新增 `ai_tag_filter` 表存储用户需要过滤的AI标签名称列表
- **设置页面新增AI标签过滤管理**：在 `SettingsGalleryController` 中新增"AI标签过滤"设置项，点击弹出Dialog，支持对过滤列表进行增删查操作
- **AI打标结果列表集成过滤**：在 `TagSettingsSheet` 的AI打标结果展示中，自动隐藏已加入过滤列表的标签；新增统计信息显示（总标签数、已过滤数）和过滤切换开关
- **快捷过滤交互**：在AI打标结果列表中，通过向右滑动item露出操作按钮，可将标签快速加入过滤列表

## Capabilities

### New Capabilities

- `ai-tag-filter-storage`: AI标签过滤列表的Room持久化存储（实体、DAO、数据库迁移）
- `ai-tag-filter-settings`: SettingsGalleryController中AI标签过滤管理入口及Dialog（增删查）
- `ai-tag-filter-ui`: TagSettingsSheet中过滤集成（过滤逻辑、统计显示、快捷加入过滤、切换开关）

### Modified Capabilities

<!-- No existing specs to modify -->

## Impact

- **Affected code**:
  - `GalleryDatabase.kt` — 新增实体和DAO注册，版本升级到7
  - `DatabaseMigration.kt` — 新增v6→v7迁移
  - `data/orm/models/` — 新增 `AITagFilter` 实体
  - `data/orm/dao/` — 新增 `AITagFilterDao`
  - `SettingsGalleryController.kt` — 替换占位preference为AI标签过滤管理入口
  - `TagSettingsSheet.kt` — AI结果列表集成过滤逻辑、统计信息、切换开关、滑动删除
  - `AITagResultAdapter.kt` — 支持滑动露出操作按钮
  - `AIPredictResult.kt` — 可能新增 `isFiltered` 字段
  - `TagSettingsViewModel.kt` — 注入过滤数据源，过滤逻辑
  - `layout/tag_settings_sheet.xml` — 新增统计文本和过滤切换按钮
  - `AppModule.kt` — 可能需要注册新服务
