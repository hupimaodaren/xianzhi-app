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
    fun getBytes(index: Int): ByteArray?
}

interface Db {
    fun query(sql: String, vararg args: Any?): List<Row>
    fun close()
}

/** 可写数据库：收藏等用户数据使用，支持 DDL/DML 与查询 */
interface WritableDb : Db {
    fun exec(sql: String, vararg args: Any?)
}

/** 以只读方式打开 SQLite 数据库文件 */
expect fun openReadOnlyDatabase(path: String): Db

/** 以可写方式打开（不存在则创建）SQLite 数据库文件 */
expect fun openWritableDatabase(path: String): WritableDb

/** zlib（deflate）解压，两端 actual 均基于 java.util.zip，零新增依赖 */
expect fun inflateZlib(data: ByteArray): ByteArray
