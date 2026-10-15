<!--
发布说明：GitHub Actions 发布流程会把本文件内容作为 Release 正文发布。
打 tag 前务必更新：标题版本号、更新内容、下载表中的版本号与体积、Full Changelog 链接。
-->

## 西安志 v0.5.0

81 部书 / 38,978 篇志书文献的**全离线**阅读应用，Windows 与 Android 双平台、双版本同步发布。

### 本版更新

- 新增「最近阅读」功能：设置页可查看阅读历史，点击条目直达正文并自动恢复上次阅读位置；支持单条删除与一键清空
- 阅读页正文支持文本长按或鼠标拖拽选择复制
- 修正关于页版本号显示为 0.3.2 的问题，当前版本正确显示为 0.5.0

### 下载选择

| 平台 | 纯文字版 | 完整图文版 |
|---|---|---|
| Android | `xianzhi-0.5.0-android-text.apk`（约 75 MB） | `xianzhi-0.5.0-android-image.apk`（约 589 MB） |
| Windows 安装程序 | `xianzhi-0.5.0-windows-text.exe`（约 122 MB） | `xianzhi-0.5.0-windows-image.exe`（约 617 MB） |
| Windows 便携免安装 | `xianzhi-0.5.0-windows-text-portable.zip`（约 121 MB） | `xianzhi-0.5.0-windows-image-portable.zip`（约 620 MB） |

- **纯文字版**：含全部 38,978 篇正文与 4,794 个缺字图；检索、阅读、收藏、设置功能完整。
- **完整图文版**：在文字版基础上内置全部 7,975 张配图，体积较大但无需联网。
- 两个 Android APK 包名不同（`.image` 后缀），可同时安装；两个 Windows 版本也是独立产品，可并存安装。

### 安装与升级

- **Android**：要求 Android 7.0（API 24）及以上；签名与历史版本相同，可直接覆盖升级。
- **Windows 安装程序**：运行 .exe 按向导安装（可自定义路径），收藏与检索阈值等设置保存在 `%APPDATA%\xianzhi` 下，覆盖安装不丢失。
- **Windows 便携版**：解压 zip 后运行其中的 `xianzhi.exe` / `xianzhi-img.exe`，不安装、不写注册表。
- 未做代码签名，首次运行可能出现 SmartScreen 提示，点击「仍要运行」即可。要求 64 位 Windows 10/11。

### 完整性校验

下载后可用 `SHA256SUMS.txt` 核对（`certutil -hashfile <文件> SHA256` 或 `sha256sum -c`）。

### 数据与版权

数据来源：西安市地方志办公室官网（xadfz.xa.gov.cn）。正文及配图著作权归西安市地方志办公室及各编纂单位所有，仅供个人学习研究使用，转载请注明出处。软件代码以 MIT 协议开源：https://github.com/hupimaodaren/xianzhi-app

**Full Changelog**: https://github.com/hupimaodaren/xianzhi-app/compare/v0.4.0...v0.5.0
