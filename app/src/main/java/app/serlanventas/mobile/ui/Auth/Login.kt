package app.serlanventas.mobile.ui.Auth

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast

class Login(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun authenticate(username: String, password: String, galpon: String): Boolean {
        // Simulación de autenticación
        return username == "admin" && password == "123" && galpon.isNotEmpty()
    }

    fun isAuthenticated(): Boolean {
        return sharedPreferences.getBoolean("is_authenticated", false)
    }

    fun saveAuthenticationState(isAuthenticated: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("is_authenticated", isAuthenticated)
            apply()
        }
    }

    fun showLoginFailureMessage() {
        Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
    }
}
