## ADDED Requirements

### Requirement: AI标签过滤数据持久化

系统SHALL在Room数据库中维护一个 `ai_tag_filters` 表，用于持久化存储用户需要过滤的AI标签名称。

#### Scenario: 数据库表创建
- **WHEN** 数据库从版本6迁移到版本7
- **THEN** 系统创建 `ai_tag_filters` 表，包含 `id` (INTEGER PRIMARY KEY AUTOINCREMENT)、`tag_name` (TEXT UNIQUE NOT NULL) 和 `created_at` (INTEGER NOT NULL DEFAULT 0) 字段

#### Scenario: 新安装建表
- **WHEN** GalleryDatabase首次创建
- **THEN** `ai_tag_filters` 表与其它表一同被Room创建

### Requirement: 过滤标签CRUD操作

系统SHALL通过 `TagFilterDao` 提供对过滤标签列表的增删查操作。

#### Scenario: 添加过滤标签
- **WHEN** 用户将一个AI标签名加入过滤列表
- **THEN** 系统将记录插入 `ai_tag_filters` 表，若 `tag_name` 已存在则忽略（UNIQUE约束）

#### Scenario: 移除过滤标签
- **WHEN** 用户从过滤列表中删除一个标签
- **THEN** 系统从 `ai_tag_filters` 表中删除对应 `tag_name` 的记录

#### Scenario: 查询所有过滤标签
- **WHEN** AI打标完成后需要过滤结果
- **THEN** 系统查询 `ai_tag_filters` 表全量数据，返回 `List<DBTagFilter>` 或 `Flow<List<DBTagFilter>>`

#### Scenario: 搜索过滤标签
- **WHEN** 用户在过滤管理Dialog中搜索标签名
- **THEN** 系统支持按 `tag_name` 模糊匹配查询过滤标签
