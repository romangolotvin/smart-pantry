package com.smartpantry.app.update

data class UpdateManifest(
    val version: String,
    val notes: String,
    val androidUrl: String
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object UpToDate : UpdateState()
    data class Available(val manifest: UpdateManifest) : UpdateState()
    data class Downloading(val loaded: Long, val total: Long) : UpdateState()
    data object ReadyToInstall : UpdateState()
    data class Error(val message: String) : UpdateState()
}
