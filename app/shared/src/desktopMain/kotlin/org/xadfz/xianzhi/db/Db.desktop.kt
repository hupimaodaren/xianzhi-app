package org.xadfz.xianzhi.db

import java.sql.DriverManager
import java.util.zip.Inflater

/** Desktop 用 sqlite-jdbc，只读打开（唯一三方依赖）。 */
actual fun openReadOnlyDatabase(path: String): Db {
    Class.forName("org.sqlite.JDBC")
    val conn = DriverManager.getConnection("jdbc:sqlite:$path")
    conn.createStatement().use { it.execute("PRAGMA query_only=ON") }
    return JdbcDb(conn)
}

/** zlib 解压（正文块），java.util.zip 内置实现。 */
actual fun inflateZlib(data: ByteArray): ByteArray {
    val inflater = Inflater()
    try {
        inflater.setInput(data)
        val out = ByteArray(1 shl 18)
        var buf = ByteArray(0)
        while (!inflater.finished()) {
            val n = inflater.inflate(out)
            if (n == 0 && inflater.needsInput()) break
            buf += out.copyOf(n)
        }
        return buf
    } finally {
        inflater.end()
    }
}

/** Desktop 可写打开：收藏库等用户数据。 */
actual fun openWritableDatabase(path: String): WritableDb {
    Class.forName("org.sqlite.JDBC")
    val conn = DriverManager.getConnection("jdbc:sqlite:$path")
    return JdbcDb(conn)
}

private class JdbcRow(private val values: Array<Any?>) : Row {
    override fun getString(index: Int): String? = when (val v = values[index]) {
        null -> null
        is String -> v
        is ByteArray -> String(v, Charsets.UTF_8)
        else -> v.toString()
    }
    override fun getLong(index: Int): Long = when (val v = values[index]) {
        is Number -> v.toLong()
        is String -> v.toLongOrNull() ?: 0L
        else -> 0L
    }
    override fun getInt(index: Int): Int = when (val v = values[index]) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: 0
        else -> 0
    }
    override fun getBytes(index: Int): ByteArray? = when (val v = values[index]) {
        is ByteArray -> v
        is String -> v.toByteArray(Charsets.UTF_8)
        else -> null
    }
}

private class JdbcDb(private val conn: java.sql.Connection) : WritableDb {
    override fun query(sql: String, vararg args: Any?): List<Row> {
        conn.prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, arg -> ps.setObject(i + 1, arg) }
            ps.executeQuery().use { rs ->
                val count = rs.metaData.columnCount
                val rows = ArrayList<Row>()
                while (rs.next()) {
                    rows.add(JdbcRow(Array(count) { i ->
                        val obj = rs.getObject(i + 1)
                        if (obj is java.sql.Blob) obj.getBytes(1, obj.length().toInt()) else obj
                    }))
                }
                return rows
            }
        }
    }

    override fun exec(sql: String, vararg args: Any?) {
        conn.prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, arg -> ps.setObject(i + 1, arg) }
            ps.executeUpdate()
        }
    }

    override fun close() = conn.close()
}
