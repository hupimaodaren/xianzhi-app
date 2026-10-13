package org.xadfz.xianzhi.db

import android.database.sqlite.SQLiteDatabase

/** Android 用系统自带 SQLite（无三方依赖），只读打开。 */
actual fun openReadOnlyDatabase(path: String): Db =
    AndroidDb(SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY))

/** Android 可写打开：收藏库等用户数据。 */
actual fun openWritableDatabase(path: String): WritableDb =
    AndroidDb(SQLiteDatabase.openOrCreateDatabase(path, null))

private class AndroidRow(private val values: Array<String?>) : Row {
    override fun getString(index: Int): String? = values[index]
    override fun getLong(index: Int): Long = values[index]?.toLongOrNull() ?: 0L
    override fun getInt(index: Int): Int = values[index]?.toIntOrNull() ?: 0
}

private class AndroidDb(private val db: SQLiteDatabase) : WritableDb {
    override fun query(sql: String, vararg args: Any?): List<Row> {
        db.rawQuery(sql, Array(args.size) { args[it]?.toString() }).use { cursor ->
            val rows = ArrayList<Row>(cursor.count)
            while (cursor.moveToNext()) {
                rows.add(AndroidRow(Array(cursor.columnCount) { i -> cursor.getString(i) }))
            }
            return rows
        }
    }

    override fun exec(sql: String, vararg args: Any?) {
        db.execSQL(sql, Array(args.size) { args[it]?.toString() })
    }

    override fun close() = db.close()
}
