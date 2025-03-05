package app.serlanventas.mobile.ui.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TabViewModel : ViewModel() {
    private val _navigateToTab = MutableLiveData<Int>()
    val navigateToTab: LiveData<Int> = _navigateToTab

    fun setNavigateToTab(position: Int) {
        _navigateToTab.value = position
    }
}