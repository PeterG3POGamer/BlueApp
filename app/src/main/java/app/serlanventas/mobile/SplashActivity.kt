package app.serlanventas.mobile

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import app.serlanventas.mobile.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar el MediaPlayer con el sonido del gallo
        mediaPlayer = MediaPlayer.create(this, R.raw.pollo_cantando)

        // Reproducir el sonido del gallo al iniciar la actividad
        mediaPlayer.start()

        // Aplicar la animación al logo
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.logo.startAnimation(fadeInAnimation)

        // Animacion de derecha a izquierda
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()

            // Liberar el recurso de MediaPlayer
            mediaPlayer.release()
        }, 3000)

    }

    override fun onDestroy() {
        super.onDestroy()
        // Asegurarse de liberar el MediaPlayer si la actividad se destruye
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}