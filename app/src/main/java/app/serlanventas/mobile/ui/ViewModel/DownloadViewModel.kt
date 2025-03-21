package app.serlanventas.mobile.ui.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DownloadViewModel : ViewModel() {
    private val _isDownloading = MutableLiveData(false)
    val isDownloading: LiveData<Boolean> = _isDownloading

    fun setDownloading(isDownloading: Boolean) {
        _isDownloading.value = isDownloading
    }
}
