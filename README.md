# 西安地方志 App

把西安市地方志办公室官网「志鉴浏览」已归档的地方志全文，做成离线可读、可全文检索的原生 App。

- **数据来源**：[xadfz.xa.gov.cn](http://xadfz.xa.gov.cn)（西安市地方志办公室官网，政府公开信息）
- **建设单位**：西安地名协会
- **技术栈**：Kotlin Multiplatform + Compose Multiplatform（Material 3）
- **目标平台**：Android + Windows（iOS 预留）
- **发布形态**：文字版（主力）+ 带图版（研究）

## 数据规模

全库共 **81 部书、38978 篇、约 6670 万字**正文，已全部内置在安装包内。

| 版本 | 主库 | 图片资源 | 说明 |
|---|---|---|---|
| 文字版 | `xianzhi.db`（174 MB） | 缺字图 4794 个（10.79 MB） | 主力版，体积小速度快 |
| 带图版 | `xianzhi_img.db`（175 MB） | 缺字图 + 配图 7975 张（514 MB） | 研究版，完整呈现原书面貌 |

## 功能

- 浏览：六大栏目 → 书目 → 篇目目录树 → 阅读
- 阅读：字号无级调节（14–28sp）、行距、浅色/深色主题、阅读位置记忆、上一篇/下一篇
- 检索：全库全文检索（`instr`），跳转正文并高亮关键词，搜索历史
- 图片：`⟦img:文件名⟧` 标记解析，文字版缺字图内嵌、带图版配图撑满可放大

## 下载 v0.1.0

最新版本：<https://github.com/hupimaodaren/xianzhi-app/releases/tag/v0.1.0>

数据已全部内置，无需单独下载数据文件。

| 平台 | 纯文字版 | 完整图文版 |
|---|---|---|
| Android | `xianzhi-0.1.0-android-text.apk`（75.22 MB） | `xianzhi-0.1.0-android-image.apk`（589.67 MB） |
| Windows 安装程序 | `xianzhi-0.1.0-windows-text.exe`（133.32 MB） | `xianzhi-0.1.0-windows-image.exe`（628.92 MB） |
| Windows 便携免安装 | `xianzhi-0.1.0-windows-text-portable.zip`（133.08 MB） | `xianzhi-0.1.0-windows-image-portable.zip`（631.93 MB） |

同一 Release 内提供 `SHA256SUMS.txt`，下载后可校验完整性：

```bash
# Windows PowerShell
Get-FileHash xianzhi-0.1.0-windows-text.exe -Algorithm SHA256
```

> 说明：早期 `data v1` Release 中的数据资产（数据库、配图压缩包等）已全部清空删除，数据改由 v0.1.0 安装包内置分发，该 tag 仅作历史标记保留。

## 安装与使用

### Android

1. 下载对应 APK（文字版体积小，推荐日常阅读；图文版适合研究原书面貌）
2. 安装时需允许「未知来源」应用
3. 首次启动会释放内置数据库到应用私有目录，约 1–2 分钟（体积约 700 MB），期间有进度提示，请勿杀后台
4. 系统要求：Android 7.0（API 24）及以上

### Windows

**安装程序版（.exe）**：

1. 双击运行安装程序，可选择安装路径（默认 `%LOCALAPPDATA%\Programs\xianzhi`）
2. 因未购买代码签名证书，SmartScreen 可能提示「Windows 已保护你的电脑」，点击「更多信息」→「仍要运行」即可
3. 安装完成后从开始菜单启动

**便携免安装版（.zip）**：

1. 解压到任意目录（建议英文路径）
2. 运行 `xianzhi.exe`（文字版）或 `xianzhi-img.exe`（图文版）
3. 不写注册表、不生成开始菜单快捷方式，删除目录即完全卸载
4. 设置与数据存放在 `%APPDATA%\xianzhi`

首次启动会释放内置数据库到用户数据目录，约 1–2 分钟，之后即可离线使用。

系统要求：Windows 10/11 64 位。

## 版权声明

**地方志正文及配图著作权归西安市地方志办公室及各编纂单位所有**，仅供个人学习研究使用，转载请注明出处。

本仓库「软件代码」采用 [MIT](LICENSE) 协议开源；数据文件以「内置分发」形式随安装包发布，不适用 MIT 协议。
