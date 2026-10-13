package org.xadfz.xianzhi.db

import java.sql.DriverManager

/** Desktop 用 sqlite-jdbc，只读打开（唯一三方依赖）。 */
actual fun openReadOnlyDatabase(path: String): Db {
    Class.forName("org.sqlite.JDBC")
    val conn = DriverManager.getConnection("jdbc:sqlite:$path")
    conn.createStatement().use { it.execute("PRAGMA query_only=ON") }
    return JdbcDb(conn)
}

/** Desktop 可写打开：收藏库等用户数据。 */
actual fun openWritableDatabase(path: String): WritableDb {
    Class.forName("org.sqlite.JDBC")
    val conn = DriverManager.getConnection("jdbc:sqlite:$path")
    return JdbcDb(conn)
}

private class JdbcRow(private val values: Array<String?>) : Row {
    override fun getString(index: Int): String? = values[index]
    override fun getLong(index: Int): Long = values[index]?.toLongOrNull() ?: 0L
    override fun getInt(index: Int): Int = values[index]?.toIntOrNull() ?: 0
}

private class JdbcDb(private val conn: java.sql.Connection) : WritableDb {
    override fun query(sql: String, vararg args: Any?): List<Row> {
        conn.prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, arg -> ps.setObject(i + 1, arg) }
            ps.executeQuery().use { rs ->
                val count = rs.metaData.columnCount
                val rows = ArrayList<Row>()
                while (rs.next()) {
                    rows.add(JdbcRow(Array(count) { i -> rs.getString(i + 1) }))
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
