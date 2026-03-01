package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.FragmentFirstBinding
import kotlinx.coroutines.launch
import java.util.Locale

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ItemAdapter
    private val db by lazy { AppDatabase.getDatabase(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ItemAdapter(mutableListOf())
        binding.recyclerView.adapter = adapter

        setupSwipeToDelete()
        loadItems()
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val itemToDelete = adapter.getItemAt(position)
                
                viewLifecycleOwner.lifecycleScope.launch {
                    db.itemDao().delete(itemToDelete)
                    loadItems()
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun loadItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = db.itemDao().getAll()
            adapter.updateItems(items)
            updateTotalSavings(items)
        }
    }

    private fun updateTotalSavings(items: List<Item>) {
        val total = items.sumOf { it.amount }
        binding.totalSavingsText.text = String.format(Locale.getDefault(), "+$%.2f", total)
    }

    fun addItem(name: String, amount: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            val newItem = Item(name = name, amount = amount)
            db.itemDao().insert(newItem)
            loadItems()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}