package app.serlanventas.mobile

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.serlanventas.mobile.VersionControl.UpdateChecker
import app.serlanventas.mobile.ui.login.LoginFragment
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val updateChecker by lazy { UpdateChecker(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Verificar si el usuario ya ha iniciado sesión
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Si ya está logueado, redirigir a la MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Si no está logueado, mostrar el LoginFragment
            if (savedInstanceState == null) {
                showLoginFragment()
            }
        }
        lifecycleScope.launch {
            updateChecker.checkAndDownloadUpdate()
        }
    }

    fun showLoginFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    fun onLoginSuccess() {
        // Guardar el estado de autenticación en SharedPreferences
        sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

        // Redirigir a la MainActivity después de la autenticación exitosa
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun onLoginFailed() {
        // Mostrar mensaje de error si la autenticación falla
        Toast.makeText(this, "Error en el inicio de sesión", Toast.LENGTH_SHORT).show()
    }
}
