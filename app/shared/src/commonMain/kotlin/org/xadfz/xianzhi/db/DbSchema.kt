package org.xadfz.xianzhi.db

/** 表名、字段与栏目顺序常量，与数据构建期生成的库一致。 */
object DbSchema {
    const val TABLE_BOOKS = "books"
    const val TABLE_ARTS = "arts"

    const val CAT_XIAN = "西安市志"
    const val CAT_DISTRICT = "区县志鉴"
    const val CAT_PROFESSIONAL = "专业志·部门志"
    const val CAT_YEARBOOK = "西安年鉴"
    const val CAT_LOCAL = "地情丛书"
    const val CAT_OLD = "旧志整理"

    /** 首页栏目排列顺序 */
    val CATEGORY_ORDER = listOf(
        CAT_XIAN, CAT_DISTRICT, CAT_PROFESSIONAL, CAT_YEARBOOK, CAT_LOCAL, CAT_OLD,
    )
}
