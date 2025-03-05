package app.serlanventas.mobile.ui.DataSyncManager

sealed class SyncResult {
    data class Success(val needsSync: Boolean) : SyncResult()
    data class Error(val message: String) : SyncResult()
}