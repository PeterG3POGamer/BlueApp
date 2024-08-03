package app.serlanventas.mobile.ui.Web

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import app.serlanventas.mobile.databinding.FragmentVentasWebBinding
import app.serlanventas.mobile.ui.Utilidades.Constants
import app.serlanventas.mobile.ui.ViewModel.VentasWebViewModel
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

class VentasWebFragment : Fragment() {

    private lateinit var binding: FragmentVentasWebBinding
    private lateinit var viewModel: VentasWebViewModel
    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var webViewStateRestored = false

    private var downloadID: Long = 0

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVentasWebBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(VentasWebViewModel::class.java)
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

        // Configurar SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            reloadWithoutCache()
        }

        requireActivity().registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
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

        webView.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun printPDF(url: String) {
                activity?.runOnUiThread {
                    val printManager = activity?.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Documento PDF"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                }
            }
        }, "AndroidPrinter")

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
                    Constants.WEB_URL_VENTAS -> {
                        // Mostrar WebView y ocultar ProgressBar
                        webView.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE
                        Log.d(TAG, "Página WEB_URL_VENTAS cargada correctamente.")
                    }
                    else -> {
                        // Mantener WebView oculto y ProgressBar visible
                        webView.visibility = View.GONE
                        binding.progressBar.visibility = View.VISIBLE
                        Log.d(TAG, "La URL cargada no coincide con LOGIN_URL ni WEB_URL_VENTAS, volviendo a cargar...")
                        webView.loadUrl(Constants.LOGIN_URL)
                    }
                }

                // Detener la animación de recarga
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "onReceivedError: ${error?.description}")
                // Mostrar WebView y ocultar ProgressBar en caso de error
                webView.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
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
                        webView.loadUrl(Constants.WEB_URL_VENTAS)
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
        requireActivity().unregisterReceiver(onDownloadComplete)
    }

    companion object {
        private const val TAG = "VentasWebFragment"
    }
}
