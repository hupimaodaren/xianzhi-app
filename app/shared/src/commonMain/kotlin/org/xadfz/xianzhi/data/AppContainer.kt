package org.xadfz.xianzhi.data

import org.xadfz.xianzhi.db.Queries
import org.xadfz.xianzhi.db.openReadOnlyDatabase
import org.xadfz.xianzhi.favorites.FavoritesRepository
import org.xadfz.xianzhi.search.SearchSession
import org.xadfz.xianzhi.settings.SearchHistory
import org.xadfz.xianzhi.settings.SettingsStore

/**
 * 应用依赖容器。两端入口先把 db 与图片目录部署到本地文件系统，再构造本对象。
 *
 * @param missingImgDir 缺字图目录（文字版/带图版都必需）
 * @param contentImgDir 内容配图目录；null 表示文字版
 */
class AppContainer(
    val dbPath: String,
    val missingImgDir: String,
    val contentImgDir: String?,
    val settings: SettingsStore,
) {
    private val db = openReadOnlyDatabase(dbPath)
    val queries = Queries(db)
    val searchHistory = SearchHistory(settings)
    val favorites = FavoritesRepository()
    val searchSession = SearchSession()

    val isImageEdition: Boolean get() = contentImgDir != null

    fun missingImagePath(file: String): String = "$missingImgDir/$file"

    fun contentImagePath(file: String): String? = contentImgDir?.let { "$it/$file" }

    fun close() {
        db.close()
        favorites.close()
    }
}
