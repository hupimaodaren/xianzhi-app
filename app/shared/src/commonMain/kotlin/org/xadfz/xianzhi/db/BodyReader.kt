package org.xadfz.xianzhi.db

/**
 * 正文块读取器（格式版本 2）。
 *
 * 正文按篇对齐拼接成 256KB 块、整块 zlib 压缩存于 blocks 表（见
 * scripts/build_app_db.py）。任意一篇只落在单个块内，解压一篇只需取一块；
 * 相邻篇目常共块，这里保留少量最近解压的块作缓存。
 */
class BodyReader(private val db: Db) {

    /** 已解压块缓存：blk → 原始字节 */
    private val cache = LinkedHashMap<Int, ByteArray>()

    @Synchronized
    fun read(blk: Int, off: Int, nb: Int): ByteArray {
        val data = block(blk)
        return data.copyOfRange(off, off + nb)
    }

    /** 块内 [off, off+nb) 字节区间是否包含 [needle]（UTF-8 字节子串匹配） */
    @Synchronized
    fun contains(blk: Int, off: Int, nb: Int, needle: ByteArray): Boolean {
        val data = block(blk)
        return bytesIndexOf(data, off, off + nb, needle) >= 0
    }

    private fun block(blk: Int): ByteArray {
        cache[blk]?.let { return it }
        val compressed = db.query("SELECT data FROM blocks WHERE blk = ?", blk)
            .firstOrNull()?.getBytes(0) ?: error("正文块缺失：blk=$blk")
        val inflated = inflateZlib(compressed)
        if (cache.size >= MAX_CACHED_BLOCKS) {
            cache.remove(cache.keys.first())
        }
        cache[blk] = inflated
        return inflated
    }

    companion object {
        /** 缓存块数上限：每块解压后约 256KB，4 块约 1MB */
        const val MAX_CACHED_BLOCKS = 4

        /**
         * 简单子串搜索，返回命中下标；未命中 -1。语义与 SQLite instr 一致
         * （字节级子串匹配；UTF-8 自同步编码保证与字符级匹配结果完全等价）。
         * [from]/[end] 限定 haystack 内的搜索区间。
         */
        fun bytesIndexOf(haystack: ByteArray, from: Int, end: Int, needle: ByteArray): Int {
            if (needle.isEmpty()) return from
            val limit = end - needle.size
            val first = needle[0]
            var i = from
            while (i <= limit) {
                if (haystack[i] == first) {
                    var j = 1
                    while (j < needle.size && haystack[i + j] == needle[j]) j++
                    if (j == needle.size) return i
                }
                i++
            }
            return -1
        }
    }
}

/** 库格式版本常量与探测。 */
object DbFormat {
    /** 当前库格式版本（scripts/build_app_db.py 写入 meta 表） */
    const val FORMAT_VERSION = 2

    /**
     * 读取库格式版本。无 meta 表的 v1 旧库查询会抛错，按版本 1 处理，
     * 由部署层据此触发一次性升级（重新部署内置库）。
     */
    fun readVersion(db: Db): Int =
        runCatching {
            db.query("SELECT v FROM meta WHERE k = 'format_version'")
                .firstOrNull()?.getString(0)?.trim()?.toIntOrNull()
        }.getOrNull() ?: 1
}
