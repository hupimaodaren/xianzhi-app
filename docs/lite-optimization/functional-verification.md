# 轻量化专项 · 功能完整性验证报告

- 日期：2026-10-14
- 基线：v0.3.2（commit 29ead61）　优化版：v0.3.2-lite
- 验证目标：确认所有现有业务功能 100% 完整保留，无功能降级、无交互变化
- 关联报告：[size-comparison.md](size-comparison.md)、[db-consistency.md](db-consistency.md)

## 1. 总体结论

**全部通过。** 优化改动严格限定在数据读取层与构建层，UI 组件、交互流程、业务逻辑零改动；53 项单元回归测试通过，38,978 篇 × 2 库逐篇字节级校验零差异，桌面端旧库升级冒烟通过，MCP 端到端双格式验证一致。

## 2. 改动面声明

**有改动的文件（全部为数据层/构建层）：**

| 文件 | 改动 | 对功能的影响 |
|---|---|---|
| `shared/.../db/BodyReader.kt`（新增） | v2 块解压读取、字节级子串搜索、4 块 LRU 缓存 | 数据层实现，公共 API 不变 |
| `shared/.../db/Queries.kt` | articleDetail 改走 BodyReader；检索改为"SQL 取元数据 + 内存子串匹配" | 公共 API 签名不变（search/searchPage/countHits） |
| `shared/.../db/Db.kt` / Db.desktop / Db.android | expect 增加 inflateZlib、Row.getBytes、Blob 处理 | 数据层 |
| `shared/.../data/DesktopDataDeployer.kt` / AndroidDataDeployer.kt | 格式守卫：旧库（format_version≠2）一次性自动重部署 | 仅升级路径，见 §8 |
| `shared/build.gradle.kts` | 移除 material-icons-extended；sqlite-jdbc 构建期裁剪 | 构建层 |
| `androidApp/build.gradle.kts` | 数据目录引用 data/（路径 ASCII 化） | 构建层 |
| `shared/.../ui/LocalIcons.kt`（新增）+ 4 个 UI 文件图标 import | 5 个图标本地复刻，几何数据逐点一致 | 仅 import 来源变化，视觉零变化（见 §7） |

**未触碰：**全部页面组件（Books/Search/Reader/Favorites/Settings）、导航与返回逻辑（AppNavigator）、检索分组与粘性头（v0.3.2）、收藏体系（v0.3.0）、搜索返回状态保持与目录滚动恢复（v0.3.0/0.3.1）、阈值分级检索与页码跳转（v0.3.1 逻辑，分组展示沿用 v0.3.2 形态）、RichText 图文混排渲染。

## 3. 功能逐项核验

| 功能面 | 核验方式 | 结果 |
|---|---|---|
| 书目分类 / 书列表 / 篇目目录（含滚动位置恢复） | 桌面冒烟 + 回归测试 | 通过（逻辑未触碰） |
| 阅读器：图文混排、缺字图内嵌、配图放大、上一篇/下一篇、文内互见 | 逐篇正文与标记校验（mark_diff=0）+ 冒烟 | 通过 |
| 全文检索：大小写敏感子串、标题命中优先排序 | 20 关键词 × 2 库命中数与顺序对照 | 通过（见 §5） |
| 检索阈值分级（500 询问 / 2000 强制终止，可配置） | 逻辑未触碰；countHits/search 公共 API 不变 | 通过 |
| 检索分组展示 + 粘性分类头（v0.3.2） | 逻辑未触碰，仅数据来源改为 v2 | 通过 |
| 收藏体系：合集分组、收藏对话框、新建合集、收藏管理 | 逻辑未触碰；图标几何一致 | 通过 |
| 设置页：阈值自定义、即时生效、持久化 | 逻辑未触碰 | 通过 |
| 三入口常驻导航（窄屏底栏 / 宽屏 Rail） | 逻辑未触碰；桌面冒烟窗口正常 | 通过 |
| xianzhi-mcp（独立组件）7 工具 | v2 适配 + stdio 端到端 + 双格式对比 | 通过（见 §9） |

## 4. 回归测试

- `gradlew :shared:desktopTestRunner`：**OK (53 tests)**，全部通过。
- 其中 `QueriesSearchPageTest` 针对 v2 读取层重写（7 项）：标题命中排序优先、分页结果与全量拼接一致、offset 越界、非法参数、countHits、snippet 摘要、articleDetail 正文解码。
- 测试夹具自建 v2 schema（meta/books/arts+blk,off,nb/blocks），单块 Deflater 压缩，与生产格式同构。

## 5. 检索语义保持（重点核验）

| 语义点 | v1 | v2 | 等价性 |
|---|---|---|---|
| 匹配规则 | SQL `instr(body,kw)>0 OR instr(title,kw)>0`（大小写敏感子串） | 解压后 UTF-8 字节级子串搜索（bytesIndexOf） | UTF-8 字节级子串与字符级子串等价（UTF-8 无字节歧义） |
| 排序 | 标题命中优先，再按 id ASC | 同 | 20 关键词 order_eq=true |
| 上限 | SEARCH_MAX_HITS=500、软阈值 2000（设置页可调） | 同 | 阈值常量与设置项未改动 |
| 缓存 | 无 | 会话内 scanCache（countHits→search→searchPage 只扫一次） | 行为增强，不影响结果 |

命中数对照（20 关键词 × 2 库，v1 与 v2 完全一致）见 [db-consistency.md §3.2](db-consistency.md)。

## 6. 性能与内存基准（JVM OptBench，文字版 v2）

| 指标 | v1（优化前） | v2（优化后） | 说明 |
|---|---|---|---|
| 打开数据库 | — | 183 ms | 含格式探测、81 书 / 38,978 篇元数据加载 |
| 阅读单篇（含解压+切片+解码） | <1 ms | 1.235 ms/篇 | 连续翻篇 1.134 ms；人眼无感 |
| 全库检索计数 | 1–3 s（SQL instr 全表扫描） | **0.77–1.03 s** | 磁盘 IO 154→55 MiB；检索反而更快 |
| 会话内翻页（page 50 条） | — | 16–57 ms | scanCache 命中 |
| 常驻额外内存 | — | ≈1 MiB | 4 块 × 256KB LRU；全量扫描瞬时堆峰值最高约 203 MiB，扫描结束即回落 |

计算开销略增（解压）为方案已知代价，实际全库检索快于 v1 的 SQL instr，用户已确认接受。无结果词（xyz不存在）扫描 834 ms 正常返回 0 命中。

## 7. 图标视觉一致性

移除 material-icons-extended（36 MiB 原始 / 34.4 MiB 压缩）后，5 个不在 core 集的图标以本地 `ImageVector` 复刻：`Bookmark`、`BookmarkBorder`、`ExpandLess`、`ExpandMore`、`DriveFileRenameOutline`。

- 几何数据由 IconDump.java 从 material-icons-extended 1.7.3 **运行时反射提取**（pathData 逐点、viewport 24、NonZero 填充规则、默认黑填充），非手工临摹。
- 引用处仅替换 import 来源，调用参数（tint、尺寸）不变。

## 8. 旧库升级路径（兼容性）

| 端 | 机制 | 验证 |
|---|---|---|
| Windows | 启动时探测已部署库 `meta.format_version`，≠2 时用包内 v2 库覆盖（一次性） | 冒烟：run-data 残留 v1 旧库（175 MiB）启动 → 自动替换为 60.88 MiB v2 库 → 窗口正常打开、书目/阅读/检索正常、无异常日志 |
| Android | `filesDir` 中 db 存在但 `db_format.txt` 标记≠"2" → 显示「正在升级数据库（一次性，约需 1 分钟）」并重拷，完成后写标记 | 代码路径与桌面同构；APK 内置库已确认为 v2（652 块、format=2） |

旧版 App（≤v0.3.2）无法读取 v2 库：v2 库只随 lite 制品分发，v0.3.2 及更早安装包内置 v1 库，互不影响。

## 9. MCP 双格式端到端（独立组件同步适配）

- `xianzhi-mcp` 已适配 v2（独立仓库 commit 16ef7d0 已推送），**自动探测** v1/v2，7 个工具接口不变。
- `test_mcp_client.py`（MCP stdio 协议，PyInstaller exe 子进程）：7 工具发现、stats（38,978 篇）、search、chunked 全部通过。
- `verify_dual_format.py`：同一批查询 v1 库 vs v2 库全接口输出对比一致，总字数 66,343,013 逐字对齐。

## 10. 编译验证

| 目标 | 结果 |
|---|---|
| `:shared:compileKotlinDesktop` | 通过 |
| `:desktopApp:run`（真实数据） | 通过（v1→v2 自动升级 + 正常运行） |
| `:androidApp:assembleTextRelease` / `assembleImageRelease` | 通过（APK 内 db 已确认为 v2） |
| `:shared:desktopTestRunner` | OK (53 tests) |
