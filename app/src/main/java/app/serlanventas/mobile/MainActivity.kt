package app.serlanventas.mobile

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import app.serlanventas.mobile.VersionControl.UpdateChecker
import app.serlanventas.mobile.databinding.ActivityMainBinding
import app.serlanventas.mobile.ui.ViewModel.TabViewModel
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val updateChecker by lazy { UpdateChecker(this) }
    private lateinit var tabViewModel: TabViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        opcion 2 : actualizar aplicacion desde git
        lifecycleScope.launch {
            updateChecker.checkAndDownloadUpdate()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        tabViewModel = ViewModelProvider(this).get(TabViewModel::class.java)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_initBluetooth,
                R.id.nav_initReportePeso,
                R.id.nav_initReporteGuias,
                R.id.nav_initReporteVentasApp,
                R.id.nav_impresoraConfig
            ),
            drawerLayout
        )

        navView.setupWithNavController(navController)

        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.appBarMain.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Actualiza el ítem seleccionado en función del destino actual
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNavViewMenu(navView.menu, destination)
        }

        navView.setNavigationItemSelectedListener { item ->
            // Función para desmarcar todos los ítems, incluidos los de los submenús
            fun uncheckAll(menu: Menu) {
                for (i in 0 until menu.size()) {
                    val menuItem = menu.getItem(i)
                    menuItem.isChecked = false
                    if (menuItem.hasSubMenu()) {
                        uncheckAll(menuItem.subMenu!!)
                    }
                }
            }

            // Desmarcar todos los ítems del menú principal
            uncheckAll(navView.menu)

            // Manejar la navegación y marcar el ítem seleccionado
            when (item.itemId) {
                R.id.nav_initBluetooth -> navController.navigate(R.id.nav_initBluetooth)
                R.id.nav_initReportePeso -> navController.navigate(R.id.nav_initReportePeso)
                R.id.nav_initReporteGuias -> navController.navigate(R.id.nav_initReporteGuias)
                R.id.nav_initReporteVentasApp -> navController.navigate(R.id.nav_initReporteVentasApp)
                R.id.nav_impresoraConfig -> navController.navigate(R.id.nav_impresoraConfig)
                else -> return@setNavigationItemSelectedListener false
            }

            drawerLayout.closeDrawers()
            true
        }
        // Actualiza el ítem de la versión en el menú del NavigationView
        updateAppVersion(navView)
    }

    private fun updateAppVersion(navView: NavigationView) {
        val context = navView.context
        val packageManager = context.packageManager
        val packageName = context.packageName
        val versionName = try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Desconocida"
        }

        val versionTextView: TextView? = navView.findViewById(R.id.nav_version)
        versionTextView?.text = "Versión $versionName"
    }

    private fun updateNavViewMenu(menu: Menu, destination: NavDestination) {
        // Desmarcar todos los ítems del menú
        fun uncheckAll(menu: Menu) {
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                menuItem.isChecked = false
                if (menuItem.hasSubMenu()) {
                    uncheckAll(menuItem.subMenu!!)
                }
            }
        }

        uncheckAll(menu)

        // Marcar el ítem correspondiente
        when (destination.id) {
            R.id.nav_initBluetooth -> menu.findItem(R.id.nav_initBluetooth).isChecked = true
            R.id.nav_initReportePeso -> menu.findItem(R.id.nav_initReportePeso).isChecked = true
            R.id.nav_initReporteGuias -> menu.findItem(R.id.nav_initReporteGuias).isChecked = true
            R.id.nav_initReporteVentasApp -> menu.findItem(R.id.nav_initReporteVentasApp).isChecked = true
            R.id.nav_impresoraConfig -> menu.findItem(R.id.nav_impresoraConfig).isChecked = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerToggle.onOptionsItemSelected(item)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
