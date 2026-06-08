package com.dedm.dns

import android.content.Context
import android.provider.Settings

object DnsManager {
    fun isDnsEnabled(context: Context): Boolean {
        return Settings.Global.getString(context.contentResolver, "private_dns_mode") == "hostname"
    }

    fun getActiveSpecifier(context: Context): String? {
        return Settings.Global.getString(context.contentResolver, "private_dns_specifier")
    }

    fun enableDns(context: Context, dnsAddress: String) {
        val resolver = context.contentResolver
        Settings.Global.putString(resolver, "private_dns_specifier", dnsAddress)
        Settings.Global.putString(resolver, "private_dns_mode", "hostname")
    }

    fun disableDns(context: Context) {
        Settings.Global.putString(context.contentResolver, "private_dns_mode", "opportunistic")
    }

    fun toggleDns(context: Context, dnsAddress: String) {
        if (isDnsEnabled(context)) {
            disableDns(context)
        } else {
            enableDns(context, dnsAddress)
        }
    }
}

