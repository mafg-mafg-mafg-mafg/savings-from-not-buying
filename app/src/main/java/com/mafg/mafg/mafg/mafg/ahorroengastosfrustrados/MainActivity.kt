package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.ActivityMainBinding
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.DialogAddItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { saveCsvToUri(it) }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importCsvFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val adapter = ScreenSlidePagerAdapter(this)
        binding.contentMain.viewPager.adapter = adapter

        // Change title based on current page
        binding.contentMain.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                supportActionBar?.title = when (position) {
                    0 -> getString(R.string.third_fragment_label)
                    1 -> getString(R.string.first_fragment_label)
                    else -> getString(R.string.second_fragment_label)
                }
            }
        })

        binding.fab.setOnClickListener {
            val currentItem = binding.contentMain.viewPager.currentItem
            if (currentItem == 0) {
                showAddIncomeDialog()
            } else if (currentItem == 1) {
                showAddSavingDialog()
            }
        }
    }

    private fun showAddSavingDialog() {
        val dialogBinding = DialogAddItemBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_add_product_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_add_product_add) { _, _ ->
                val name = dialogBinding.etName.text.toString()
                val amountStr = dialogBinding.etAmount.text.toString()
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                
                if (name.isNotBlank()) {
                    val currentFragment = supportFragmentManager.fragments
                        .filterIsInstance<FirstFragment>()
                        .firstOrNull()
                    currentFragment?.addItem(name, amount)
                }
            }
            .setNegativeButton(R.string.dialog_add_product_cancel, null)
            .show()
    }

    private fun showAddIncomeDialog() {
        val dialogBinding = DialogAddItemBinding.inflate(layoutInflater)
        // Set custom hints for Incomes
        dialogBinding.tilName.hint = getString(R.string.dialog_add_income_name_hint)
        dialogBinding.tilAmount.hint = getString(R.string.dialog_add_income_cash_hint)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_add_income_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_add_product_add) { _, _ ->
                val origin = dialogBinding.etName.text.toString()
                val incomeStr = dialogBinding.etAmount.text.toString()
                val amount = incomeStr.toDoubleOrNull() ?: 0.0
                
                if (origin.isNotBlank()) {
                    val currentFragment = supportFragmentManager.fragments
                        .filterIsInstance<ThirdFragment>()
                        .firstOrNull()
                    currentFragment?.addItem(origin, amount)
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
            R.id.action_import -> {
                importDatabaseFromCsv()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportDatabaseToCsv() {
        createDocumentLauncher.launch("Savings from NOT buying.csv")
    }

    private fun importDatabaseFromCsv() {
        openDocumentLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv"))
    }

    private fun saveCsvToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@MainActivity)
                val items = withContext(Dispatchers.IO) { db.itemDao().getAll() }
                
                val totalSavings = items.sumOf { it.amount * it.count }
                
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write("ID,Name,Amount,Count,Subtotal,Type\n")
                            items.forEach { item ->
                                val subtotal = item.amount * item.count
                                writer.write("${item.id},${item.name},${item.amount},${item.count},${String.format(Locale.US, "%.2f", subtotal)},${item.type}\n")
                            }
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

    private fun importCsvFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val items = mutableListOf<Item>()
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            val header = reader.readLine() // Skip header
                            var line: String? = reader.readLine()
                            while (line != null && line.isNotBlank()) {
                                if (line.startsWith(",")) break // Stop at total footer
                                val parts = line.split(",")
                                if (parts.size >= 4) {
                                    val name = parts[1]
                                    val amount = parts[2].toDoubleOrNull() ?: 0.0
                                    val count = parts[3].toIntOrNull() ?: 1
                                    val type = if (parts.size > 5) parts[5] else "SAVING"
                                    items.add(Item(name = name, amount = amount, count = count, type = type))
                                }
                                line = reader.readLine()
                            }
                        }
                    }
                }

                if (items.isNotEmpty()) {
                    val db = AppDatabase.getDatabase(this@MainActivity)
                    withContext(Dispatchers.IO) {
                        db.itemDao().insertAll(items)
                    }
                    
                    supportFragmentManager.fragments.forEach { fragment ->
                        if (fragment is FirstFragment) fragment.loadItems()
                        if (fragment is ThirdFragment) fragment.loadItems()
                        if (fragment is SecondFragment) fragment.loadExpenses()
                    }
                    
                    Toast.makeText(this@MainActivity, "Import successful!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}