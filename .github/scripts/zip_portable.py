# -*- coding: utf-8 -*-
"""把 AppImage 目录打成便携 zip（条目用正斜杠，顶层带版本目录）。
用法: python zip_portable.py <源目录> <输出zip> <顶层目录名>"""
import os, sys, zipfile


def main():
    src, out, top = sys.argv[1], sys.argv[2], sys.argv[3]
    n = 0
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as z:
        for root, dirs, files in os.walk(src):
            dirs.sort()
            for f in sorted(files):
                p = os.path.join(root, f)
                arc = top + "/" + os.path.relpath(p, src).replace(os.sep, "/")
                z.write(p, arc)
                n += 1
    print("zip entries:", n, "size MB:", round(os.path.getsize(out) / 1048576, 1), flush=True)


if __name__ == "__main__":
    main()
