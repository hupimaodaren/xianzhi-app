# -*- coding: utf-8 -*-
"""从 books 分片 JSON 生成两版 App 库（文字版 + 带图版）。

文字版 xianzhi.db     ：body 仅保留缺字图标记 ⟦img:小图⟧（字库缺字，不显示则阅读中断），
                        内容配图标记移除。配套 assets：缺字图 1.7MB。
带图版 xianzhi_img.db ：body 保留全部图片标记 ⟦img:文件⟧，App 从图片目录加载。
                        配套资源：配图 522MB（分期下载或桌面版内置）。

表结构（两版一致）：
  books(id TEXT PK, title, cat, n)
  arts(id INTEGER PK, book, title, vol, date, body)
检索：instr(body, ?) 全表扫描，无分词缺陷。
"""
import json
import os
import re
import sqlite3
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.environ.get("XADFZ_APP_DATA", "D:/西安地方志App开发/数据")
IMG_MARK = re.compile(r"⟦img:([^⟧]+)⟧")


def strip_big_images(body, missing_set):
    """文字版：删除内容配图标记，保留缺字图标记。"""
    def rep(m):
        return m.group(0) if m.group(1) in missing_set else ""
    return IMG_MARK.sub(rep, body)


def generate(db_path, mode):
    t0 = time.time()
    if os.path.exists(db_path):
        os.remove(db_path)
    con = sqlite3.connect(db_path)
    con.execute("PRAGMA journal_mode=OFF")
    con.execute("CREATE TABLE books(id TEXT PRIMARY KEY, title TEXT, cat TEXT, n INT)")
    con.execute("CREATE TABLE arts(id INTEGER PRIMARY KEY, book TEXT, title TEXT,"
                " vol TEXT, date TEXT, body TEXT)")
    con.execute("CREATE INDEX idx_arts_book ON arts(book)")
    con.execute("CREATE INDEX idx_arts_title ON arts(title)")
    idx = json.load(open(os.path.join(DATA, "index.json"), encoding="utf-8"))
    missing = set()
    mp = os.path.join(DATA, "missing_char_imgs.json")
    if mode == "text" and os.path.exists(mp):
        missing = set(json.load(open(mp, encoding="utf-8")))
    n = 0
    for cat in idx["categories"]:
        for book in cat["books"]:
            data = json.load(open(os.path.join(DATA, "books", book["file"]), encoding="utf-8"))
            con.execute("INSERT INTO books VALUES (?,?,?,?)",
                        (data["id"], data["title"], cat["name"], len(data["arts"])))
            rows = []
            for a in data["arts"]:
                vol = a["p"][0] if a["p"] else ""
                date = ""
                if a.get("s"):
                    date = a["s"].rstrip("·").rsplit("·", 1)[-1].strip()
                body = strip_big_images(a["b"], missing) if mode == "text" else a["b"]
                rows.append((n, data["id"], a["t"], vol, date, body))
                n += 1
            con.executemany("INSERT INTO arts VALUES (?,?,?,?,?,?)", rows)
        con.commit()
    con.execute("VACUUM")
    con.close()
    print("%s：%d 篇，%.1f MB，用时 %.0fs" % (os.path.basename(db_path), n,
          os.path.getsize(db_path) / 1048576, time.time() - t0))


def main():
    generate(os.path.join(DATA, "xianzhi.db"), "text")
    generate(os.path.join(DATA, "xianzhi_img.db"), "img")


if __name__ == "__main__":
    sys.exit(main())
