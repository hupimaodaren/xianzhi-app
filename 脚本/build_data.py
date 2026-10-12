# -*- coding: utf-8 -*-
"""把成品 Markdown 资料库转换为网页阅读器用的分片 JSON。

输入：西安地方志全文资料库/（六大栏目 + 每部书的 目录.md 与篇目 .md）
输出：webapp/data/
  index.json    栏目与书目清单
  titles.json   全库篇目标题索引（全局搜索用）
  books/NN-书号.json  每部书一篇：篇目树 + 正文
正文保留 Markdown 原文（去掉图片引用），客户端渲染。
"""
import json
import os
import re
import sys

SRC = os.environ.get("XADFZ_LIB", "D:/西安地方志全文资料库/西安地方志全文资料库")
OUT = os.environ.get("XADFZ_APP_DATA", "D:/西安地方志App开发/数据")
CATS = ["01-西安市志", "02-区县志鉴", "03-专业志·部门志", "04-西安年鉴", "05-地情丛书", "06-旧志整理"]

IMG_MD = re.compile(r"!\[[^\]]*\]\(([^)]*)\)")
IMG_TAG = re.compile(r'<img[^>]+src=["\']([^"\']+)["\'][^>]*>', re.I)
SRC_LINE = re.compile(r"^来源：(.+)$", re.M)
PIC_DIR = "D:/西安地方志全文资料库/西安地方志全文资料库/配图"
# 缺字图名单（源站字库缺字用图显示的字）：由上下文+像素尺寸扫描生成（正文引用+高度≤64px），
# 存于 D:/西安地方志App开发/数据/missing_char_imgs.json；缺失时退化为 <2KB 兜底。
try:
    MISSING_SET = set(json.load(open("D:/西安地方志App开发/数据/missing_char_imgs.json", encoding="utf-8")))
except Exception:
    MISSING_SET = None


def is_missing_char_img(fname):
    """缺字图判定：读名单（高度<=64px 的单字图）；名单缺失时按 <2KB 兜底。"""
    if MISSING_SET is not None:
        return fname in MISSING_SET
    p = os.path.join(PIC_DIR, fname)
    return os.path.exists(p) and os.path.getsize(p) < 2048


def img_marker(m):
    """图片引用 -> ⟦img:文件名⟧ 占位标记（App 端解析渲染）。"""
    base = os.path.basename(m.group(1).strip().split("?")[0])
    return "⟦img:%s⟧" % base if base else ""


def parse_article(path):
    """解析一篇 md -> (title, body, source)。图片统一转为 ⟦img:文件名⟧ 标记。"""
    t = open(path, encoding="utf-8").read()
    lines = t.split("\n")
    title = ""
    body_start = 0
    if lines and lines[0].startswith("# "):
        title = lines[0][2:].strip()
        body_start = 1
    # 来源行（--- 之后）
    src = ""
    for i, ln in enumerate(lines):
        m = SRC_LINE.match(ln)
        if m:
            src = m.group(1).strip()
            lines = lines[:i]
            break
    body = "\n".join(lines[body_start:]).strip()
    body = IMG_TAG.sub(img_marker, IMG_MD.sub(img_marker, body))
    body = re.sub(r"\n{3,}", "\n\n", body).strip()
    return title, body, src


def parse_toc(book_dir):
    """解析 目录.md -> [(title, rel, parts)]"""
    entries = []
    tp = os.path.join(book_dir, "目录.md")
    if not os.path.exists(tp):
        return entries
    for ln in open(tp, encoding="utf-8"):
        m = re.match(r"- \[(.*?)\]\(([^)]+)\)\s*｜\s*(.*)", ln.strip())
        if not m:
            continue
        title, rel, full = m.group(1), m.group(2), m.group(3).strip()
        parts = [p.strip() for p in full.split("/") if p.strip()]
        entries.append((title, rel, parts))
    return entries


def emit_book(out, index, titles, cat, cat_name, bid, btitle, sub, bpath, stats):
    """把一部书（或一卷）写为 books/NN-书名.json，并登记索引。"""
    entries = parse_toc(bpath)
    # 补扫：磁盘上存在但目录未登记的篇目，按所在目录路径补入（保证不丢篇）
    listed = {os.path.normpath(rel) for _, rel, _ in entries}
    extra = []
    for dp, _, fs in os.walk(bpath):
        for f in fs:
            if not f.endswith(".md") or f == "目录.md":
                continue
            rp = os.path.relpath(os.path.join(dp, f), bpath)
            if os.path.normpath(rp) not in listed:
                parts = [re.sub(r"^\d+-", "", p) for p in os.path.dirname(rp).split(os.sep) if p]
                extra.append((f[:-3], rp.replace(os.sep, "/"), parts + [f[:-3]]))
    if extra:
        print("    [补扫] %s：目录外篇目 %d 篇" % (btitle + ("·" + sub if sub else ""), len(extra)), flush=True)
    entries += extra
    if sub:
        vol_name = re.sub(r"^\d+-", "", sub)
        title_full = btitle + " · " + vol_name
    else:
        title_full = btitle
    arts = []
    for title, rel, parts in entries:
        ap = os.path.normpath(os.path.join(bpath, rel))
        if not os.path.exists(ap):
            continue
        atitle, body, asrc = parse_article(ap)
        arts.append({
            "t": atitle or title,
            "p": parts[:-1] if len(parts) > 1 else [],
            "b": body,
            "s": asrc,
        })
    if not arts:
        return
    cat_no = cat.split("-")[0]
    if sub:
        book_id = "%s-%s-%s" % (cat_no, bid, re.sub(r"^\d+-", "", sub))
    else:
        book_id = "%s-%s" % (cat_no, bid)
    fname = "%s-%s.json" % (book_id, re.sub(r"[\\/:*?\"<>|]", "_", title_full))
    data = {"id": book_id, "title": title_full, "cat": cat_name, "arts": arts}
    json.dump(data, open(os.path.join(out, "books", fname), "w", encoding="utf-8"),
              ensure_ascii=False, separators=(",", ":"))
    stats["arts"] += len(arts)
    for i, a in enumerate(arts):
        titles.append({"b": fname[:-5], "i": i, "t": a["t"]})
    for c in index["categories"]:
        if c["name"] == cat_name:
            c["books"].append({"id": book_id, "title": title_full, "n": len(arts), "file": fname})
            break
    print("  %s / %s：%d 篇" % (cat_name, title_full, len(arts)), flush=True)


def main():
    src = os.path.abspath(SRC)
    out = os.path.abspath(OUT)
    os.makedirs(os.path.join(out, "books"), exist_ok=True)
    index = {"categories": []}
    titles = []
    stats = {"arts": 0}

    for cat in CATS:
        cdir = os.path.join(src, cat)
        if not os.path.isdir(cdir):
            continue
        cat_name = cat.split("-", 1)[1]
        books = sorted(d for d in os.listdir(cdir) if os.path.isdir(os.path.join(cdir, d)))
        index["categories"].append({"name": cat_name, "dir": cat, "books": []})
        for bdir in books:
            bpath = os.path.join(cdir, bdir)
            m = re.match(r"(\d+)-(.+)", bdir)
            bid, btitle = (m.group(1), m.group(2)) if m else (bdir, bdir)
            # 书下带卷的（如第二轮市志八卷本）：每个卷目录拆为独立书目
            if not os.path.exists(os.path.join(bpath, "目录.md")):
                for sub in sorted(os.listdir(bpath)):
                    sp = os.path.join(bpath, sub)
                    if os.path.isdir(sp) and os.path.exists(os.path.join(sp, "目录.md")):
                        emit_book(out, index, titles, cat, cat_name, bid, btitle,
                                  sub, sp, stats)
                continue
            emit_book(out, index, titles, cat, cat_name, bid, btitle, None, bpath, stats)

    json.dump(index, open(os.path.join(out, "index.json"), "w", encoding="utf-8"),
              ensure_ascii=False, separators=(",", ":"))
    json.dump(titles, open(os.path.join(out, "titles.json"), "w", encoding="utf-8"),
              ensure_ascii=False, separators=(",", ":"))
    # 缺字图名单：正文标记中引用的缺字图（源站字库缺字，两版都须保留显示）。
    # 名单由上下文+像素尺寸扫描产出（高度<=64px 的单字图），存于 App开发/；
    # 此处仅做校验透传，保证 books 标记、db 过滤、 assets 三者一致。
    import glob as _glob
    mc = set()
    for fp in _glob.glob(os.path.join(out, "books", "*.json")):
        for mm in re.findall(r"⟦img:([^⟧]+)⟧", open(fp, encoding="utf-8").read()):
            if is_missing_char_img(mm):
                mc.add(mm)
    if MISSING_SET:
        mc |= {x for x in MISSING_SET if os.path.exists(os.path.join(PIC_DIR, x))}
    json.dump(sorted(mc), open(os.path.join(out, "missing_char_imgs.json"), "w",
              encoding="utf-8"), ensure_ascii=False)
    print("缺字图（名单+正文实际引用）：%d 个" % len(mc), flush=True)
    nbooks = sum(len(c["books"]) for c in index["categories"])
    total = sum(os.path.getsize(os.path.join(out, f)) for f in os.listdir(out))
    total += sum(os.path.getsize(os.path.join(out, "books", f)) for f in os.listdir(os.path.join(out, "books")))
    print("-" * 50)
    print("完成：%d 部书，%d 篇，data 合计 %.1f MB" % (nbooks, stats["arts"], total / 1048576))


if __name__ == "__main__":
    sys.exit(main())
