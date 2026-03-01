package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.ActivityMainBinding
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.DialogAddItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { saveCsvToUri(it) }
    }

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
            .setTitle(R.string.dialog_add_product_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_add_product_add) { _, _ ->
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
            .setNegativeButton(R.string.dialog_add_product_cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                exportDatabaseToCsv()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportDatabaseToCsv() {
        createDocumentLauncher.launch("Savings from NOT buying.csv")
    }

    private fun saveCsvToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@MainActivity)
                val items = withContext(Dispatchers.IO) { db.itemDao().getAll() }
                
                // Calculate total savings: Sum of (amount * count) for all items
                val totalSavings = items.sumOf { it.amount * it.count }
                
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            // Header with Subtotal column
                            writer.write("ID,Name,Amount,Count,Subtotal\n")
                            
                            items.forEach { item ->
                                val subtotal = item.amount * item.count
                                writer.write("${item.id},${item.name},${item.amount},${item.count},${String.format(Locale.US, "%.2f", subtotal)}\n")
                            }
                            
                            // Footer with total sum
                            writer.write("\n,,,,Total: ${String.format(Locale.US, "%.2f", totalSavings)}\n")
                        }
                    }
                }
                Toast.makeText(this@MainActivity, "Export successful!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
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