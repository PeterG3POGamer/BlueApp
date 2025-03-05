package app.serlanventas.mobile.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.serlanventas.mobile.ui.Auth.Login
import kotlinx.coroutines.launch

class LoginViewModel(private val login: Login) : ViewModel() {
    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    fun login(dni: String, password: String) {
        viewModelScope.launch {
            val success = login.authenticate(dni, password)
            _loginResult.value = success
        }
    }
}