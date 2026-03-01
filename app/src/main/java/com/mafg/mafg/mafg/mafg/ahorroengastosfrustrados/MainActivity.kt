package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.ActivityMainBinding
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.DialogAddItemBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun showAddItemDialog() {
        val dialogBinding = DialogAddItemBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Product")
            .setView(dialogBinding.root)
            .setPositiveButton("Agregar") { _, _ ->
                val name = dialogBinding.etName.text.toString()
                val amountStr = dialogBinding.etAmount.text.toString()
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                
                if (name.isNotBlank()) {
                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                    val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                    if (currentFragment is FirstFragment) {
                        currentFragment.addItem(name, amount)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}