package com.dedm.dns

import android.content.Context

class DnsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("dns_prefs", Context.MODE_PRIVATE)

    fun saveDns(address: String) {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return
        val currentList = getDnsHistory().toMutableList()
        currentList.remove(trimmed)
        currentList.add(0, trimmed)
        if (currentList.size > 3) {
            currentList.removeAt(currentList.lastIndex)
        }
        prefs.edit().putString("dns_history", currentList.joinToString(",")).apply()
    }

    fun getDnsHistory(): List<String> {
        val historyStr = prefs.getString("dns_history", null) ?: return emptyList()
        return historyStr.split(",").filter { it.isNotBlank() }
    }
}
