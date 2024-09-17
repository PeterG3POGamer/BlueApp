package app.serlanventas.mobile.ui.login

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.serlanventas.mobile.MainActivity
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentLoginBinding
import app.serlanventas.mobile.ui.Auth.Login
import kotlin.math.PI
import kotlin.math.sin

class LoginFragment : Fragment() {

    private lateinit var viewModel: LoginViewModel
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var loadingAnimatorSet: AnimatorSet
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        setupLoadingAnimation()
        updateAppVersion()

        val login = Login(requireContext())

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    return LoginViewModel(login) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }).get(LoginViewModel::class.java)


        binding.buttonLogin.setOnClickListener {
            val username = binding.editTextUsername.text.toString()
            val password = binding.editTextPassword.text.toString()

            var hasError = false

            // Verificar campo de usuario
            if (username.isEmpty()) {
                binding.editTextUsername.error = "Ingrese un usuario válido"
                binding.editTextUsername.requestFocus()
                hasError = true
            } else {
                binding.editTextUsername.error = null
            }

            // Verificar campo de contraseña
            if (password.isEmpty()) {
                binding.editTextPassword.error = "Ingrese una contraseña válida"
                if (!hasError) {
                    binding.editTextPassword.requestFocus()
                }
                hasError = true
            } else {
                binding.editTextPassword.error = null
            }

            if (hasError) {
                return@setOnClickListener
            }

            // Ocultar teclado
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)

            // Iniciar animación de carga y realizar login
            startLoadingAnimation()
            viewModel.login(username, password)
        }


        viewModel.loginResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                binding.buttonLogin.postDelayed({
                    sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                    sharedPreferences.edit().putBoolean("isProduction", true).apply()
                    navigateToMainActivity()
                }, 2000)
            } else {
                Toast.makeText(context, "No se pudo conectar, inténtelo de nuevo o más tarde", Toast.LENGTH_SHORT).show()
                stopLoadingAnimation()
            }
        }

        binding.buttonGoToRegister.setOnClickListener {
            // TODO: Implementar navegación a la pantalla de registro
            Toast.makeText(context, "Navegar a registro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLoadingAnimation() {
        val dot1 = binding.dot1
        val dot2 = binding.dot2
        val dot3 = binding.dot3

        val dots = listOf(dot1, dot2, dot3)
        val waveHeight = 4.5f
        val waveDuration = 1500L

        val waveAnimator = ValueAnimator.ofFloat(0f, 2 * PI.toFloat()).apply {
            duration = waveDuration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val phase = animator.animatedValue as Float
                dots.forEachIndexed { index, dot ->
                    val offset = index * (2 * PI.toFloat() / dots.size)
                    val y = -waveHeight * sin((phase + offset).toDouble()).toFloat()
                    dot.translationY = y
                }
            }
        }

        loadingAnimatorSet = AnimatorSet().apply {
            play(waveAnimator)
        }
    }

    private fun startLoadingAnimation() {
        binding.buttonLogin.text = ""
        binding.buttonLogin.isEnabled = false
        binding.loadingDotsContainer.visibility = View.VISIBLE
        loadingAnimatorSet.start()
    }

    private fun stopLoadingAnimation() {
        binding.buttonLogin.text = getString(R.string.login)
        binding.buttonLogin.isEnabled = true
        binding.loadingDotsContainer.visibility = View.GONE
        loadingAnimatorSet.cancel()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(requireActivity(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    @SuppressLint("SetTextI18n")
    private fun updateAppVersion() {
        val context = requireContext()
        val packageManager = context.packageManager
        val packageName = context.packageName
        val versionName = try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Desconocida"
        }

        val versionTextView = binding.version
        versionTextView.text = "Versión: v$versionName"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}