package net.hearnsoft.alist_sdk.model

data class AlistConfig(
    var host: String = "",
    var username: String = "",
    var password: String = "",
    var isAnonymous: Boolean = true
) {
    fun getBaseUrl(): String {
        return if (host.endsWith("/")) host else "$host/"
    }
}