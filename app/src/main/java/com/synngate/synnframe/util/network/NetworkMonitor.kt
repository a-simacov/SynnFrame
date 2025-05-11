package com.synngate.synnframe.util.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

// Сервис для мониторинга состояния сети
class NetworkMonitor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // StateFlow для текущего состояния сети
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Unavailable)
    val networkState = _networkState.asStateFlow()

    fun initialize() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val state = getNetworkState(capabilities)
                Timber.d("Network is available: $state")
                _networkState.value = state
            }

            override fun onLost(network: Network) {
                Timber.d("Network is unavailable")
                _networkState.value = NetworkState.Unavailable
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val state = getNetworkState(capabilities)
                Timber.d("Changing of network capability: $state")
                _networkState.value = state
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Установка начального состояния
        _networkState.value = getCurrentNetworkState()
    }

    fun getCurrentNetworkState(): NetworkState {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkState.Unavailable
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkState.Unavailable
        return getNetworkState(capabilities)
    }

    private fun getNetworkState(capabilities: NetworkCapabilities?): NetworkState {
        capabilities ?: return NetworkState.Unavailable

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                NetworkState.Available(ConnectionType.WIFI, isMetered)
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                NetworkState.Available(ConnectionType.CELLULAR, true)
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                NetworkState.Available(ConnectionType.ETHERNET, false)
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> {
                NetworkState.Available(ConnectionType.BLUETOOTH, true)
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                // VPN обычно работает поверх другого соединения, пытаемся определить его тип
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                        NetworkState.Available(ConnectionType.WIFI, true)
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                        NetworkState.Available(ConnectionType.CELLULAR, true)
                    else -> NetworkState.Available(ConnectionType.OTHER, true)
                }
            }
            else -> NetworkState.Available(ConnectionType.OTHER, true)
        }
    }

    fun isNetworkAvailable(): Boolean {
        return _networkState.value is NetworkState.Available
    }

    fun isWifiAvailable(): Boolean {
        val state = _networkState.value
        return state is NetworkState.Available && state.type == ConnectionType.WIFI
    }

    fun isCellularAvailable(): Boolean {
        val state = _networkState.value
        return state is NetworkState.Available && state.type == ConnectionType.CELLULAR
    }

    fun isMeteredConnection(): Boolean {
        val state = _networkState.value
        return state is NetworkState.Available && state.isMetered
    }
}

enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    BLUETOOTH,
    OTHER
}

sealed class NetworkState {

    object Unavailable : NetworkState()

    data class Available(
        val type: ConnectionType,
        val isMetered: Boolean
    ) : NetworkState()
}