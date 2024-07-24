package com.example.blueapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.blueapp.VersionControl.UpdateChecker
import com.example.blueapp.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

//import com.github.javiersantos.appupdater.AppUpdater
//import com.github.javiersantos.appupdater.enums.UpdateFrom

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val updateChecker by lazy { UpdateChecker(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // opcion 1 : Actualizar aplicacion desde git
//        AppUpdater(this)
//            .setUpdateFrom(UpdateFrom.GITHUB)
//            .setGitHubUserAndRepo("PeterG3POGamer", "BlueApp")
//            .setTitleOnUpdateAvailable("Actualización disponible")
//            .setContentOnUpdateAvailable("Una nueva versión está disponible. ¿Deseas actualizar?")
//            .setButtonUpdate("Actualizar")
//            .setButtonDismiss("Más tarde")
//            .showAppUpdated(true)
//            .start()

//        opcion 2 : actualizar aplicacion desde git
//        lifecycleScope.launch {
//            updateChecker.checkAndDownloadUpdate()
//        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Definir los destinos de nivel superior del AppBarConfiguration según tus necesidades
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_slideshow, // Agrega aquí todos los destinos que deben ser de nivel superior
            ),
            drawerLayout
        )

        // Configurar el NavigationView con el NavController y el AppBarConfiguration
        navView.setupWithNavController(navController)

        // Configurar el botón de hamburguesa para abrir el drawer
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.appBarMain.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()


    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            android.R.id.home -> {
                drawerToggle.onOptionsItemSelected(item)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

//
//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration)
//    }
}