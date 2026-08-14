package com.example

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

object VpnTunnelManager {

    private var backend: Backend? = null
    
    private val _tunnelState = MutableStateFlow(Tunnel.State.DOWN)
    val tunnelState: StateFlow<Tunnel.State> = _tunnelState

    private val wgTunnel = object : Tunnel {
        override fun getName(): String = "SiloVPN"
        override fun onStateChange(newState: Tunnel.State) {
            _tunnelState.value = newState
        }
    }

    fun initBackend(context: Context) {
        if (backend == null) {
            backend = GoBackend(context.applicationContext)
        }
    }
    
    fun getBackend(): Backend? = backend

    suspend fun turnOn(configString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backend = backend ?: return@withContext false
            val config = Config.parse(ByteArrayInputStream(configString.toByteArray()))
            backend.setState(wgTunnel, Tunnel.State.UP, config)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun turnOff(): Boolean = withContext(Dispatchers.IO) {
        try {
            val backend = backend ?: return@withContext false
            backend.setState(wgTunnel, Tunnel.State.DOWN, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
