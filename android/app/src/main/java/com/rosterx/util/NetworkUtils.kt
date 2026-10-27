package com.rosterx.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {

    /**
     * Accurately detects and returns the active network transport type.
     * Differentiates between Mobile Data (5G/4G Cellular), Wi-Fi, Ethernet, and VPN.
     */
    fun getActiveNetworkDescription(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Unknown Network"

        val activeNetwork = cm.activeNetwork ?: return "Offline (No Connection)"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Disconnected"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data (5G / 4G Cellular)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet LAN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Secure Tunnel"
            else -> "Active Network"
        }
    }

    /**
     * Returns true if there is an active internet-capable network connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
