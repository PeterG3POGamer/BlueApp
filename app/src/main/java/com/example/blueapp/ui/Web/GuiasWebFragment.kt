package com.example.blueapp.ui.Web

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.blueapp.databinding.FragmentGuiasWebBinding
import com.example.blueapp.ui.Utilidades.Constants
import com.example.blueapp.ui.ViewModel.GuiasWebViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GuiasWebFragment : Fragment() {

    private lateinit var binding: FragmentGuiasWebBinding
    private lateinit var viewModel: GuiasWebViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var webView: WebView
    private var webViewStateRestored = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGuiasWebBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(GuiasWebViewModel::class.java)
        webView = binding.webView
        swipeRefreshLayout = binding.swipeRefreshLayout

        setupWebView()

        // Ocultar WebView y mostrar ProgressBar al inicio
        webView.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        if (!webViewStateRestored && viewModel.webViewState != null) {
            Log.d(TAG, "Restoring WebView state...")
            webView.restoreState(viewModel.webViewState!!)
            webViewStateRestored = true
        } else if (!webViewStateRestored) {
            // Cargar LOGIN_URL al inicio
            webView.loadUrl(Constants.LOGIN_URL)
            webViewStateRestored = true
        }

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        return binding.root
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d(TAG, "shouldOverrideUrlLoading: $url")
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                view?.evaluateJavascript("""
                    (function() {
                        // Ocultar la barra lateral
                        var sidebar = document.getElementById('accordionSidebar');
                        if (sidebar) {
                            sidebar.style.display = 'none';
                        }
                        
                        var boton = document.getElementById('sidebarToggleTop');
                        if (boton) {
                            boton.style.display = 'none';
                        }
                    })()
                """.trimIndent(), null)

                Log.d(TAG, "onPageFinished: $url")

                when (url) {
                    Constants.LOGIN_URL -> {
                        // Mantener WebView oculto y ProgressBar visible
                        webView.visibility = View.GONE
                        binding.progressBar.visibility = View.VISIBLE
                        iniciarSesion()
                    }
                    Constants.WEB_URL_GUIA -> {
                        // Mostrar WebView y ocultar ProgressBar
                        webView.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE
                        Log.d(TAG, "Página WEB_URL_GUIA cargada correctamente.")
                    }
                    else -> {
                        // Mantener WebView oculto y ProgressBar visible
                        webView.visibility = View.GONE
                        binding.progressBar.visibility = View.VISIBLE
                        Log.d(TAG, "La URL cargada no coincide con LOGIN_URL ni WEB_URL_GUIA, volviendo a cargar...")
                        webView.loadUrl(Constants.LOGIN_URL)
                    }
                }
                swipeRefreshLayout.isRefreshing = false

            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "onReceivedError: ${error?.description}")
                // Mostrar WebView y ocultar ProgressBar en caso de error
                webView.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun iniciarSesion() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(Constants.LOGIN_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val responseCode = conn.responseCode
                val responseMessage = conn.responseMessage

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuffer()
                    var inputLine: String?
                    while (inputStream.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    inputStream.close()

                    handleResponse(response.toString())
                } else {
                    Log.e(TAG, "HTTP Error: $responseCode, $responseMessage")
                    withContext(Dispatchers.Main) {
                        // Mostrar WebView y ocultar ProgressBar en caso de error
                        webView.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data: ${e.message}")
                withContext(Dispatchers.Main) {
                    // Mostrar WebView y ocultar ProgressBar en caso de error
                    webView.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun handleResponse(response: String) {
        withContext(Dispatchers.Main) {
            try {
                val jsonResponse = JSONObject(response)
                val status = jsonResponse.optString("status")
                val message = jsonResponse.optString("message")

                when (status) {
                    "success" -> {
                        Log.d(TAG, "Success: $message")
                        // Cargar la URL deseada solo después de éxito
                        // WebView sigue oculto, ProgressBar visible
                        webView.loadUrl(Constants.WEB_URL_GUIA)
                    }
                    else -> {
                        Log.d(TAG, "Estado no exitoso: $status")
                        // Mostrar WebView y ocultar ProgressBar en caso de error
                        webView.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON: ${e.message}")
                // Mostrar WebView y ocultar ProgressBar en caso de error
                webView.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.webViewState = Bundle().also { webView.saveState(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.webViewState = Bundle().also { webView.saveState(it) }
        webViewStateRestored = false
    }

    companion object {
        private const val TAG = "GuiasWebFragment"
    }
}