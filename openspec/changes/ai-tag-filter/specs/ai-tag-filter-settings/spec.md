## ADDED Requirements

### Requirement: 设置页AI标签过滤入口

系统SHALL在 `SettingsGalleryController` 的"Tag Settings"分类中提供一个可点击的AI标签过滤管理入口。

#### Scenario: 显示过滤管理入口
- **WHEN** 用户进入 Gallery 设置页面
- **THEN** 在"Tag Settings"分类下显示"AI标签过滤"preference项，展示当前过滤标签数量作为summary

#### Scenario: 点击打开过滤管理Dialog
- **WHEN** 用户点击"AI标签过滤"preference
- **THEN** 系统弹出一个Dialog，展示当前已保存的过滤标签列表

### Requirement: AI标签过滤管理Dialog

系统SHALL在设置页的过滤管理Dialog中支持对过滤标签列表的增删查操作。

#### Scenario: 查看过滤标签列表
- **WHEN** 过滤管理Dialog打开
- **THEN** Dialog内RecyclerView展示所有已保存的过滤标签，每个item显示标签名称

#### Scenario: 添加过滤标签
- **WHEN** 用户在Dialog底部输入标签名并点击添加按钮
- **THEN** 该标签名被保存到过滤列表，RecyclerView即时刷新显示新标签

#### Scenario: 删除过滤标签
- **WHEN** 用户在Dialog中的标签item上执行删除操作（如点击删除图标或右滑）
- **THEN** 该标签从过滤列表中移除，RecyclerView即时刷新

#### Scenario: 搜索过滤标签
- **WHEN** 用户在Dialog的搜索框中输入文字
- **THEN** RecyclerView过滤显示包含搜索关键词的标签

#### Scenario: 空列表提示
- **WHEN** 过滤列表为空
- **THEN** Dialog显示"暂无过滤标签"的提示信息

#### Scenario: 设置页数量汇总
- **WHEN** 过滤标签数量发生变化（新增或删除）
- **THEN** 设置页preference的summary更新为"已过滤 X 个标签"
