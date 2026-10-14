# 轻量化专项 · 优化前后体积对比报告

- 日期：2026-10-14
- 基线：v0.3.2（commit 29ead61）　优化版：v0.3.2-lite（本地制品，不升版本号、不发 Release）
- 优化范围：数据库 v2 分块压缩、material-icons-extended 依赖移除、sqlite-jdbc 非 Windows 原生库裁剪、冗余代码清理
- 硬性约束：业务功能 100% 保留、数据无损。功能核验见 [functional-verification.md](functional-verification.md)，数据一致性见 [db-consistency.md](db-consistency.md)
- 计量单位：MiB（1 MiB = 1,048,576 字节）

## 1. 结论速览

| 维度 | 优化前 | 优化后 | 收益 |
|---|---|---|---|
| 数据库（文字版 xianzhi.db） | 166.64 | 60.67 | **-63.6%** |
| 数据库（图文版 xianzhi_img.db） | 167.00 | 60.88 | **-63.5%** |
| Windows 文字版制品（EXE / 便携 zip） | 168.36 / 168.07 | 121.74 / 121.14 | **约 -27.8%** |
| Windows 图文版制品（EXE / 便携 zip） | 663.95 / 666.92 | 617.35 / 620.01 | -7.0% |
| Android 文字版 APK | 79.94 | 74.68 | -6.6% |
| Android 图文版 APK | 594.38 | 589.14 | -0.9% |
| 安装后数据库磁盘占用（每端） | 166.64 / 167.00 | 60.67 / 60.88 | **-105.97 / -106.13 MiB** |

图文版制品百分比收益小，是因为包体主体为配图（约 500 MiB，经抽样确认已无冗余可剥离，见 §7）；其绝对收益与文字版相同（约 -46.6 MiB）。

## 2. 数据库体积（v1 → v2）

| 库 | v1（body 直存） | v2（blocks 分块 zlib-9） | 减幅 |
|---|---|---|---|
| xianzhi.db（文字版） | 166.64 | 60.67（63,620,096 B） | -105.97 MiB / **-63.6%** |
| xianzhi_img.db（图文版） | 167.00 | 60.88（63,832,064 B） | -106.13 MiB / **-63.5%** |

v2 库内压缩正文合计：54.83 MiB（文字版，652 块）/ 55.03 MiB（图文版，652 块），即正文原始 UTF-8（154.21 MiB）压缩至约 35%，库内其余为元数据、索引与 B 树页开销。

## 3. 分发制品体积（实测）

制品统一存放于 `C:\Users\qukes\.local\share\xz\staging\`。

| 制品 | v0.3.2 | v0.3.2-lite | 差值 | 减幅 |
|---|---|---|---|---|
| xianzhi-0.3.2-windows-text.exe | 168.36 | 121.74 | -46.62 | **-27.7%** |
| xianzhi-0.3.2-windows-text-portable.zip | 168.07 | 121.14 | -46.93 | **-27.9%** |
| xianzhi-0.3.2-windows-image.exe | 663.95 | 617.35 | -46.60 | -7.0% |
| xianzhi-0.3.2-windows-image-portable.zip | 666.92 | 620.01 | -46.91 | -7.0% |
| xianzhi-0.3.2-android-text.apk | 79.94 | 74.68 | -5.26 | -6.6% |
| xianzhi-0.3.2-android-image.apk | 594.38 | 589.14 | -5.24 | -0.9% |

## 4. 包内构成变化（Windows 便携 zip 内，压缩后）

| 条目 | 优化前 | 优化后 | 说明 |
|---|---|---|---|
| material-icons-extended-desktop | 34.4 | **0（移除）** | 全项目仅 5 个图标超出 core 集，已本地复刻 |
| sqlite-jdbc jar | 13.60 | **2.37** | 构建期裁剪非 Windows 原生库（原始 20.65 MiB），仅保留 Windows 四架构 |
| xianzhi.db（zip 内） | 57.4 | ≈60.9 | v2 数据已高熵压缩，外层 zip 不再可压（+3.5） |
| classes（Android dex，zip 内） | 9.83 | 5.83 | icons-extended 移除后 dex 缩小 |
| runtime / skiko / jvm.dll / icudtl.dat / 缺字图 | 不变 | 不变 | 14.4 / 5.9 / 4.5 / 4.4 / ≈8 |

净收益（文字版便携 zip）：-34.4（icons）-11.23（sqlite-jdbc）+3.5（db）≈ -42.1，实测 -46.93，差额约 4.8 来自共享模块类文件精简与重打包压缩差异。

**Android 制品净收益说明**：db 环节在 APK(zip) 内反而 +3.4（v1 db 本身 deflate 后已很紧，v2 高熵不可再压），dex 收益 -4.0 与之部分抵消，故 APK 仅净降 5.2。Android 端的主要收益体现在**安装后磁盘占用**（数据库解压落地 -106 MiB）与运行时依赖精简，而非下载体积。

## 5. 安装后磁盘占用

数据库以明文形式部署到本机（桌面 `%APPDATA%\xianzhi\data`、Android `filesDir/data`），该项收益按部署后实际占用计：

| 版本 | 优化前 | 优化后 | 节省 |
|---|---|---|---|
| 文字版 | 166.64 | 60.67 | **-105.97 MiB** |
| 图文版 | 167.00 | 60.88 | **-106.13 MiB** |

另：Windows 端数据可直接从包内 `resources/data` 只读打开，不产生复制占用（v0.2.0 起既有行为，未改变）。

## 6. 本地制品清单与 SHA256

`C:\Users\qukes\.local\share\xz\staging\SHA256SUMS-lite.txt`：

```
63978b49a25494bb014e54b3712a792ea672a6705f662869721a87819cb8e5e6  xianzhi-0.3.2-lite-windows-text.exe
bb97956429962de9a7e166ef9a0f5ad370bc264fc6d777d0bc252a8ee05d043f  xianzhi-0.3.2-lite-windows-text-portable.zip
7eb99e035472bc0b03264b0e39379720d7cd6e275d4c62ed288a597b0af8839b  xianzhi-0.3.2-lite-windows-image.exe
b81fd9be6e1c545b64c06a06825b3d3c336cc84458a934ad9bc44c2676744c22  xianzhi-0.3.2-lite-windows-image-portable.zip
6dcd5b7a1a5afb0ab79143bebee2ef33ee7dc9b4df2c97ece26dbcedd0a19005  xianzhi-0.3.2-lite-android-text.apk
f331b26607b408594ebc917b921c59b7d6cfcf03f828d9242746b1671e05aa44  xianzhi-0.3.2-lite-android-image.apk
```

## 7. 评估后未采纳项（避免无效或负收益改动）

| 项 | 结论 |
|---|---|
| 配图无损优化（元数据剥离） | 抽样 300 张（22.0 MiB）：APPn/COM 元数据合计 0 字节，**收益为 0**，不做 |
| 非正文字典化 | 上限约 3 MiB，需改 schema，收益/风险比低，不做 |
| 全库 lzma / bz2 压缩 | 体积再省 12–19 MiB，但不可随机访问（读一篇需解压全库），或需引入纯 Java XZ 依赖，不做 |
| 逐篇 zlib / 分书 zlib | 压缩率劣于分块方案且大书解压慢（6.7 ms/篇），已由分块 256KB zlib-9 方案取代 |
