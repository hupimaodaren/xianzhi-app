package org.xadfz.xianzhi.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.util.zip.Inflater

/** Android 用系统自带 SQLite（无三方依赖），只读打开。 */
actual fun openReadOnlyDatabase(path: String): Db =
    AndroidDb(SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY))

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

/** Android 可写打开：收藏库等用户数据。 */
actual fun openWritableDatabase(path: String): WritableDb =
    AndroidDb(SQLiteDatabase.openOrCreateDatabase(path, null))

private class AndroidRow(private val values: Array<Any?>) : Row {
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

private class AndroidDb(private val db: SQLiteDatabase) : WritableDb {
    override fun query(sql: String, vararg args: Any?): List<Row> {
        db.rawQuery(sql, Array(args.size) { args[it]?.toString() }).use { cursor ->
            val rows = ArrayList<Row>(cursor.count)
            while (cursor.moveToNext()) {
                rows.add(AndroidRow(Array(cursor.columnCount) { i -> cell(cursor, i) }))
            }
            return rows
        }
    }

    /** 按存储类型取值：BLOB 取字节（正文块），其余取字符串 */
    private fun cell(cursor: Cursor, i: Int): Any? =
        if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB) cursor.getBlob(i) else cursor.getString(i)

    override fun exec(sql: String, vararg args: Any?) {
        db.execSQL(sql, Array(args.size) { args[it]?.toString() })
    }

    override fun close() = db.close()
}
