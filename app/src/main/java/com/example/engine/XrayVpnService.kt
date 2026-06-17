package com.example.engine

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class XrayVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            disconnect()
            return START_NOT_STICKY
        }
        establishVpn()
        return START_STICKY
    }

    private fun establishVpn() {
        try {
            if (vpnInterface != null) return
            
            val builder = Builder()
                .setSession("TDZ Tunnel Service")
                .setMtu(1500)
                .addAddress("26.26.26.1", 24) 
                .addRoute("0.0.0.0", 0) 
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                
            vpnInterface = builder.establish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disconnect() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    companion object {
        const val ACTION_DISCONNECT = "com.example.XrayVpnService.DISCONNECT"
    }
}
