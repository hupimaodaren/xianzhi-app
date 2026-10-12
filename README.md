# 西安地方志 App

把西安市地方志办公室官网「志鉴浏览」已归档的地方志全文，做成离线可读、可全文检索的原生 App。

- **数据来源**：[xadfz.xa.gov.cn](http://xadfz.xa.gov.cn)（西安市地方志办公室官网，政府公开信息）
- **建设单位**：西安地名协会
- **技术栈**：Kotlin Multiplatform + Compose Multiplatform（Material 3）
- **目标平台**：Android + Windows（iOS 预留）
- **发布形态**：文字版（主力）+ 带图版（研究）

## 数据规模

| 版本 | 主库 | 图片资源 | 说明 |
|---|---|---|---|
| 文字版 | `xianzhi.db`（174 MB） | 缺字图 4794 个（10.79 MB） | 主力版，体积小速度快 |
| 带图版 | `xianzhi_img.db`（175 MB） | 缺字图 + 配图 7975 张（514 MB） | 研究版，完整呈现原书面貌 |

全库共 **81 部书、38978 篇、约 6670 万字**正文。

## 功能

- 浏览：六大栏目 → 书目 → 篇目目录树 → 阅读
- 阅读：字号无级调节（14–28sp）、行距、浅色/深色主题、阅读位置记忆、上一篇/下一篇
- 检索：全库全文检索（`instr`），跳转正文并高亮关键词，搜索历史
- 图片：`⟦img:文件名⟧` 标记解析，文字版缺字图内嵌、带图版配图撑满可放大

## 数据获取

大文件（数据库、配图、缺字图）托管在 [Releases](https://github.com/hupimaodaren/xianzhi-app/releases)，不进 git 仓库。

```bash
# 克隆代码
git clone https://github.com/hupimaodaren/xianzhi-app.git
cd xianzhi-app

# 下载数据（从最新 Release 下载 xianzhi.db 与缺字图，放入 数据/ 目录）
# 文字版：xianzhi.db + 缺字图/
# 带图版：xianzhi_img.db + 缺字图/ + 配图/
```

## 目录结构

```
xianzhi-app/
├── 需求与移交文档.md     # 完整需求 + KMP 技术方案 + 表结构 + SQL + 实施步骤
├── 开发使用清单.md       # 开发所需全部内容清单
├── 数据说明.md           # 数据文件清单与用法
├── LICENSE               # MIT（代码部分）
├── 数据/
│   ├── index.json        # 栏目与书目清单（9 KB）
│   ├── missing_char_imgs.json  # 缺字图名单
│   ├── xianzhi.db        # ⚠️ 从 Release 下载（不进 git）
│   ├── xianzhi_img.db    # ⚠️ 从 Release 下载（不进 git）
│   ├── 缺字图/            # ⚠️ 从 Release 下载（不进 git）
│   └── 配图/             # ⚠️ 从 Release 下载（不进 git）
└── 脚本/
    ├── build_data.py     # 成品库 md → books JSON
    └── build_app_db.py   # JSON → 两版 db
```

## 数据更新

源站新增篇目后：

```bash
python 脚本/build_data.py
python 脚本/build_app_db.py
```

## 版权声明

**地方志正文及配图著作权归西安市地方志办公室及各编纂单位所有**，仅供个人学习研究使用，转载请注明出处。

本仓库「软件代码」采用 [MIT](LICENSE) 协议开源；数据文件以「数据归档」形式随 Release 分发，不适用 MIT 协议。
