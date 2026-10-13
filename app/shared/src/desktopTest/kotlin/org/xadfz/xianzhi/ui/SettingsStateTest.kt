package org.xadfz.xianzhi.ui

import org.xadfz.xianzhi.settings.SettingsKeys
import org.xadfz.xianzhi.settings.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsStateTest {

    /** 内存实现：同时充当持久化断言的数据源 */
    private class MemStore : SettingsStore {
        val map = HashMap<String, Any>()
        override fun getString(key: String, default: String) = map[key] as? String ?: default
        override fun putString(key: String, value: String) { map[key] = value }
        override fun getInt(key: String, default: Int) = map[key] as? Int ?: default
        override fun putInt(key: String, value: Int) { map[key] = value }
        override fun getFloat(key: String, default: Float) = map[key] as? Float ?: default
        override fun putFloat(key: String, value: Float) { map[key] = value }
    }

    @Test
    fun defaultThresholds() {
        val s = SettingsState(MemStore())
        assertEquals(500, s.searchSoftLimit)
        assertEquals(2000, s.searchHardLimit)
    }

    @Test
    fun setThresholdPersistsImmediately() {
        val store = MemStore()
        val s = SettingsState(store)
        s.searchSoftLimit = 300
        s.searchHardLimit = 1200
        assertEquals(300, store.map[SettingsKeys.SEARCH_SOFT_LIMIT])
        assertEquals(1200, store.map[SettingsKeys.SEARCH_HARD_LIMIT])
        // 重新从存储构造，验证重启后保留
        val s2 = SettingsState(store)
        assertEquals(300, s2.searchSoftLimit)
        assertEquals(1200, s2.searchHardLimit)
    }

    @Test
    fun softLimitCoercedToRange() {
        val store = MemStore()
        val s = SettingsState(store)
        s.searchSoftLimit = 50
        assertEquals(100, s.searchSoftLimit)
        s.searchSoftLimit = 9999
        assertEquals(SettingsState.SOFT_MAX, s.searchSoftLimit)
    }

    @Test
    fun raisingSoftAboveHardBumpsHard() {
        val s = SettingsState(MemStore())
        s.searchSoftLimit = 3000
        assertEquals(3000, s.searchSoftLimit)
        assertEquals(3000, s.searchHardLimit)
    }

    @Test
    fun hardLimitNeverBelowSoft() {
        val store = MemStore()
        val s = SettingsState(store)
        s.searchSoftLimit = 2000
        s.searchHardLimit = 1500 // 低于软阈值，被抬回
        assertEquals(2000, s.searchHardLimit)
        s.searchHardLimit = 999999 // 超上限，被压回
        assertEquals(SettingsState.HARD_MAX, s.searchHardLimit)
    }
}
