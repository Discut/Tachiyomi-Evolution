## ADDED Requirements

### Requirement: AI打标结果自动过滤

系统SHALL在AI打标结果展示中自动隐藏用户已加入过滤列表的标签。

#### Scenario: 打标完成后自动过滤
- **WHEN** AI打标推理完成并返回标签列表
- **THEN** 系统将结果中标签名在过滤列表中的条目标记为已过滤，默认不显示在RecyclerView中

#### Scenario: 过滤不影响已采纳标签
- **WHEN** 标签已存在于数据库的tags表中（即已被采纳）
- **THEN** 该标签始终显示在结果列表中（灰显+不可点击），若同时被过滤则标签名后追加"（已过滤）"后缀

### Requirement: AI打标统计信息

系统SHALL在"AI智能打标"按钮下方显示打标结果统计信息。

#### Scenario: 显示统计信息
- **WHEN** AI打标结果就绪
- **THEN** 在AI按钮和RecyclerView之间显示文本"共识别 X 个标签，已过滤 Y 个"

#### Scenario: 未打标时隐藏统计
- **WHEN** AI打标状态为IDLE或LOADING
- **THEN** 统计信息区域不可见

### Requirement: 过滤标签显示切换

系统SHALL提供一个切换开关，允许用户选择是否在结果列表中展示被过滤的标签。

#### Scenario: 默认隐藏过滤标签
- **WHEN** AI打标结果首次展示
- **THEN** 被过滤的标签不在RecyclerView中显示，切换按钮显示为"显示已过滤"

#### Scenario: 切换显示过滤标签
- **WHEN** 用户点击"显示已过滤"按钮
- **THEN** RecyclerView展示全部标签，已过滤标签的标签名后追加"（已过滤）"后缀，已采纳标签保持灰色背景，切换按钮变为"隐藏已过滤"

#### Scenario: 切换隐藏过滤标签
- **WHEN** 用户点击"隐藏已过滤"按钮
- **THEN** RecyclerView隐藏已过滤标签，只展示未过滤的标签

### Requirement: 长按item快捷过滤

系统SHALL支持在AI打标结果RecyclerView中通过长按item弹出操作浮层，将标签加入过滤列表。

#### Scenario: 长按未采纳item弹出操作浮层
- **WHEN** 用户在未采纳（isAdopted=false）的AI结果item上长按
- **THEN** 系统弹出浮动ActionMode，菜单中包含"过滤"选项

#### Scenario: 点击过滤选项
- **WHEN** 用户在长按弹出的操作浮层中点击"过滤"
- **THEN** 该标签名被持久化保存到过滤列表，该item从当前可见列表中移除（或标记为已过滤样式），操作浮层关闭

#### Scenario: 长按已过滤item弹出操作浮层
- **WHEN** 用户在已过滤（isFiltered=true）的AI结果item上长按（切换显示后可见）
- **THEN** 系统弹出浮动ActionMode，菜单中包含"取消过滤"选项

#### Scenario: 点击取消过滤选项
- **WHEN** 用户在操作浮层中点击"取消过滤"
- **THEN** 该标签名从过滤列表移除，item恢复为正常显示样式，操作浮层关闭

#### Scenario: 已采纳且未过滤的标签无操作浮层
- **WHEN** 用户在已采纳且未被过滤（isAdopted=true, isFiltered=false）的item上长按
- **THEN** 系统不弹出操作浮层

#### Scenario: 已采纳且已过滤的标签仅可取消过滤
- **WHEN** 用户在已采纳且已被过滤（isAdopted=true, isFiltered=true）的item上长按
- **THEN** 系统弹出浮动ActionMode，菜单中仅包含"取消过滤"选项（不含"过滤"）
