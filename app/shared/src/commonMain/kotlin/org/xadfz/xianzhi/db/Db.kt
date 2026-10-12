package org.xadfz.xianzhi.db

/**
 * 只读数据库抽象，两端 actual：
 * - Android：系统自带 android.database.sqlite.SQLiteDatabase（无三方依赖）
 * - Desktop：sqlite-jdbc（org.sqlite.JDBC）
 */
interface Row {
    fun getString(index: Int): String?
    fun getLong(index: Int): Long
    fun getInt(index: Int): Int
}

interface Db {
    fun query(sql: String, vararg args: Any?): List<Row>
    fun close()
}

/** 以只读方式打开 SQLite 数据库文件 */
expect fun openReadOnlyDatabase(path: String): Db
