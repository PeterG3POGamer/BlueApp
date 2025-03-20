package app.serlanventas.mobile.ui.Web

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.serlanventas.mobile.LoginActivity
import app.serlanventas.mobile.R
import app.serlanventas.mobile.databinding.FragmentGuiasWebBinding
import app.serlanventas.mobile.ui.Utilidades.Constants
import app.serlanventas.mobile.ui.ViewModel.GuiasWebViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GuiasWebFragment : Fragment() {

    private lateinit var binding: FragmentGuiasWebBinding
    private lateinit var viewModel: GuiasWebViewModel
    private lateinit var swipeRefreshLayout: CustomSwipeRefreshLayout
    private lateinit var webView: WebView
    private var webViewStateRestored = false
    private var downloadID: Long = 0
    private lateinit var progressBar: ImageView

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGuiasWebBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(GuiasWebViewModel::class.java)
        webView = binding.webView
        swipeRefreshLayout = binding.swipeRefreshLayout
        progressBar = binding.loadingGif
        val overlay = binding.overlay.findViewById<View>(R.id.overlay)
        val loadingGif = binding.loadingGif.findViewById<ImageView>(R.id.loadingGif)

        setupWebView()

        // Ocultar WebView y mostrar ProgressBar al inicio
        webView.visibility = View.VISIBLE
        overlay.visibility = View.VISIBLE
        loadingGif.visibility = View.VISIBLE

        Glide.with(this)
            .asGif()
            .load(R.drawable.icon_loader)
            .into(loadingGif)

        if (!webViewStateRestored && viewModel.webViewState != null) {
            Log.d(TAG, "Restoring WebView state...")
            webView.restoreState(viewModel.webViewState!!)
            webViewStateRestored = true
        } else if (!webViewStateRestored) {
            val loginUrl = Constants.buildLoginUrl(requireContext())
            webView.loadUrl(loginUrl)
            webViewStateRestored = true
        }

        swipeRefreshLayout.setOnRefreshListener {
            reloadWithoutCache()
        }

        requireActivity().registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )

        return binding.root
    }

    private fun reloadWithoutCache() {
        webView.clearCache(true)
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.reload()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // Extraer el nombre del archivo de la URL
            val fileName = url.substringAfterLast("/")
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val dm = activity?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadID = dm.enqueue(request)
        }

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

                        var navbar = document.querySelector('.iq-navbar');
                        if (navbar) {
                            navbar.style.display = 'none';
                        }
                    })()
                """.trimIndent(), null)

                Log.d(TAG, "onPageFinished: $url")

                // Comparación de URLs
                val loginUrl = Constants.buildLoginUrl(requireContext())
                val guiaUrl = Constants.getGuiaUrl()

                if (url == loginUrl) {
                    // Mantener WebView oculto y ProgressBar visible
                    webView.visibility = View.GONE
                    binding.overlay.visibility = View.VISIBLE
                    binding.loadingGif.visibility = View.VISIBLE
                    iniciarSesion()
                } else if (url == guiaUrl) {
                    // Mostrar WebView y ocultar ProgressBar
                    webView.visibility = View.VISIBLE
                    binding.overlay.visibility = View.GONE
                    binding.loadingGif.visibility = View.GONE
                    Log.d(TAG, "Página WEB_URL_GUIA cargada correctamente.")
                } else {
                    // Mantener WebView oculto y ProgressBar visible
                    webView.visibility = View.GONE
                    binding.overlay.visibility = View.VISIBLE
                    binding.loadingGif.visibility = View.VISIBLE
                    Log.d(TAG, "La URL cargada no coincide con LOGIN_URL ni WEB_URL_GUIA, volviendo a cargar...")
                    webView.loadUrl(loginUrl)
                }
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "onReceivedError: ${error?.description}")
                // Mostrar WebView y ocultar ProgressBar en caso de error
                webView.visibility = View.VISIBLE
                binding.overlay.visibility = View.GONE
                binding.loadingGif.visibility = View.GONE
                // Detener la animación de recarga
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("Range")
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadID) {
                val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query()
                query.setFilterById(id)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val fileUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                    val file = File(Uri.parse(fileUri).path!!)
                    val fileUriForOpening = FileProvider.getUriForFile(
                        context!!,
                        context.applicationContext.packageName + ".fileprovider",
                        file
                    )
                    val openIntent = Intent(Intent.ACTION_VIEW)
                    openIntent.setDataAndType(fileUriForOpening, getMimeType(file.toString()))
                    openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(openIntent)
                }
            }
        }
    }

    private fun getMimeType(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private fun iniciarSesion() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loginUrl = Constants.buildLoginUrl(requireContext())
                val url = URL(loginUrl)
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
                        binding.overlay.visibility = View.GONE
                        binding.loadingGif.visibility = View.GONE
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data: ${e.message}")
                withContext(Dispatchers.Main) {
                    // Mostrar WebView y ocultar ProgressBar en caso de error
                    webView.visibility = View.VISIBLE
                    binding.overlay.visibility = View.GONE
                    binding.loadingGif.visibility = View.GONE
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

                val guiaUrl = Constants.getGuiaUrl()
                when (status) {
                    "success" -> {
                        Log.d(TAG, "Success: $message")
                        // Cargar la URL deseada solo después de éxito
                        // WebView sigue oculto, ProgressBar visible
                        webView.loadUrl(guiaUrl)
                    }
                    else -> {
                        Log.d(TAG, "Estado no exitoso: $status")
                        // Mostrar WebView y ocultar ProgressBar en caso de error
                        webView.visibility = View.VISIBLE
                        binding.overlay.visibility = View.GONE
                        binding.loadingGif.visibility = View.GONE
                        redirectToLoginActivity()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON: ${e.message}")
                // Mostrar WebView y ocultar ProgressBar en caso de error
                webView.visibility = View.VISIBLE
                binding.overlay.visibility = View.GONE
                binding.loadingGif.visibility = View.GONE
            }
        }
    }

    private fun redirectToLoginActivity() {
        // Asegurar que esta función se ejecute en el hilo principal
        activity?.runOnUiThread {
            // Ocultar ProgressBar y WebView
            binding.overlay.visibility = View.GONE
            binding.loadingGif.visibility = View.GONE
            webView.visibility = View.GONE

            val sharedPreferences = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().apply {
                remove("isLoggedIn")
                remove("storedUsername")
                remove("storedPassword")
                apply()
            }
            // Mostrar un mensaje de error
            Toast.makeText(context, "Error en el inicio de sesión. Por favor, inténtelo de nuevo.", Toast.LENGTH_LONG).show()

            // Crear un Intent para iniciar LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            // Agregar una bandera para limpiar la pila de actividades
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Finalizar la actividad actual si es necesario
            activity?.finish()
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
        requireActivity().unregisterReceiver(onDownloadComplete)
    }

    companion object {
        private const val TAG = "GuiasWebFragment"
    }
}
