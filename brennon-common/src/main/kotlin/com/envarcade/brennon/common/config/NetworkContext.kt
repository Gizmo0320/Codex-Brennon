package com.envarcade.brennon.common.config

data class NetworkContext(
    val networkId: String,
    val sharing: DataSharingConfig
) {
    fun effectiveNetworkId(dataType: DataSharingMode): String? =
        if (dataType == DataSharingMode.GLOBAL) null else networkId
}
