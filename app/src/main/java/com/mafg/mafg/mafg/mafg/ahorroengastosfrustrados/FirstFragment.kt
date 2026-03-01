package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.FragmentFirstBinding
import kotlinx.coroutines.launch

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

        loadItems()
    }

    private fun loadItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = db.itemDao().getAll()
            adapter.updateItems(items)
        }
    }

    fun addItem(name: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val newItem = Item(name = name)
            db.itemDao().insert(newItem)
            loadItems() // Recargar la lista desde la DB
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}