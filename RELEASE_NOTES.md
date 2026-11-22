<!--
发布说明：GitHub Actions 发布流程会把本文件内容作为 Release 正文发布。
打 tag 前务必更新：标题版本号、更新内容、下载表中的版本号与体积、Full Changelog 链接。
-->

## 西安志 v0.6.0

81 部书 / 38,978 篇志书文献的**全离线**阅读应用，Windows、Android 与 **macOS** 三平台同步发布。

### 本版更新

本版是一次以界面为主的大改版，并首次提供 macOS 版。

**界面美化（宣纸 / 墨 / 朱砂）**

- 新增统一视觉构件库 `ui/CommonUi.kt`：SealBadge（印章式首字徽标）、CountBadge（胶囊计数）、SectionLabel（朱砂竖条章节标题）、EmptyState（圆形字空态），六页共用
- 书目首页：印章式分类序号（一～六）、统计胶囊、分类卡片细边框 + 悬停反馈，书目列表两位数补零序号
- 篇目目录：全书目录摘要行、分组行层次与叶节点导引线、顶栏「全展开 / 全收起」文字按钮
- 全文检索：圆角搜索框与朱砂检索按钮、命中数说明行、分类头朱砂竖条与展开箭头、命中项层级缩进
- 阅读页：标题层级与朱砂短横导语、段首两字缩进、正文版心与配图圆角统一，检索关键词高亮改为暖黄底衬
- 收藏 / 最近阅读：合集摘要行、胶囊式筛选、印章首字条目、统一空态；修复副标题重复拼接书名的问题
- 设置页：全部卡片改为朱砂竖条标题，关于卡加入印章徽标与版本信息
- 导航：侧栏与底部导航选中态改为朱砂淡底胶囊 + 按压缩放动画，宽屏侧栏与窄屏底栏共用同一套图标节奏

**macOS 支持**

- 新增 Apple Silicon（M 系列）原生 DMG；Intel Mac 可经 Rosetta 2 运行
- 桌面端平台适配：平台名与打开链接按 `os.name` 分支（`open` / `xdg-open` / `rundll32`）；设置与数据目录在 macOS 上落到 `~/Library/Application Support/xianzhi`
- sqlite-jdbc 裁剪策略由「仅 Windows」调整为「Windows + macOS」，三端共用同一份构建配置

**工程改进**

- `:shared:desktopTestRunner` 补挂完整 sqlite-jdbc，测试不再只能在 Windows 上运行
- `:desktopApp:screenshot` 离屏截图任务支持 `-Pshots.*` 参数透传（主题 / 尺寸 / 单页），用于全站视觉核对
- 阅读历史时间戳改为严格单调递增，修复同一毫秒内多条记录排序不稳定的问题

### 下载选择

| 平台 | 纯文字版 | 完整图文版 |
|---|---|---|
| Android | `xianzhi-0.6.0-android-text.apk`（约 75 MB） | `xianzhi-0.6.0-android-image.apk`（约 589 MB） |
| Windows 安装程序 | `xianzhi-0.6.0-windows-text.exe`（约 122 MB） | `xianzhi-0.6.0-windows-image.exe`（约 617 MB） |
| Windows 便携免安装 | `xianzhi-0.6.0-windows-text-portable.zip`（约 121 MB） | `xianzhi-0.6.0-windows-image-portable.zip`（约 620 MB） |
| macOS（Apple Silicon） | `xianzhi-0.6.0-macos-arm64.dmg`（约 130 MB） | — |

- **纯文字版**：含全部 38,978 篇正文与 4,794 个缺字图；检索、阅读、收藏、最近阅读、设置功能完整。
- **完整图文版**：在文字版基础上内置全部 7,975 张配图，体积较大但无需联网。
- 两个 Android APK 包名不同（`.image` 后缀），可同时安装；两个 Windows 版本也是独立产品，可并存安装。
- macOS 版目前只提供纯文字版 DMG（图文版体积过大，需要完整配图可用 Windows 版数据）。

### 安装与升级

- **Android**：要求 Android 7.0（API 24）及以上；签名与历史版本相同，可直接覆盖升级。
- **Windows 安装程序**：运行 .exe 按向导安装（可自定义路径），收藏与检索阈值等设置保存在 `%APPDATA%\xianzhi` 下，覆盖安装不丢失。
- **Windows 便携版**：解压 zip 后运行其中的 `xianzhi.exe` / `xianzhi-img.exe`，不安装、不写注册表。
- **macOS**：打开 DMG 把「西安志」拖入「应用程序」。未做代码签名与公证，首次启动需在「系统设置 → 隐私与安全性」底部点击「仍要打开」（或对 App 右键选择「打开」）。要求 macOS 11 及以上。
- 未做代码签名，Windows 首次运行可能出现 SmartScreen 提示，点击「仍要运行」即可。要求 64 位 Windows 10/11。

### 完整性校验

下载后可用 `SHA256SUMS.txt` 核对（Windows 用 `certutil -hashfile <文件> SHA256`，macOS/Linux 用 `shasum -a 256 <文件>` 或 `sha256sum -c`）。

### 数据与版权

数据来源：西安市地方志办公室官网（xadfz.xa.gov.cn）。正文及配图著作权归西安市地方志办公室及各编纂单位所有，仅供个人学习研究使用，转载请注明出处。软件代码以 MIT 协议开源：https://github.com/hupimaodaren/xianzhi-app

**Full Changelog**: https://github.com/hupimaodaren/xianzhi-app/compare/v0.5.1...v0.6.0
