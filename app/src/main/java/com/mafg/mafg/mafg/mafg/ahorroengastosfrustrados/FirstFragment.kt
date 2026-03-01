package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
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
    private var lastTotal = 0.0

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
        val newTotal = items.sumOf { it.amount }
        
        if (newTotal > lastTotal) {
            triggerHapticFeedback()
            playCashRegisterSound()
            
            binding.totalSavingsText.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    binding.totalSavingsText.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }.start()
        }

        val animator = ValueAnimator.ofFloat(lastTotal.toFloat(), newTotal.toFloat())
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            binding.totalSavingsText.text = String.format(Locale.getDefault(), "+$%.2f", animatedValue)
        }
        animator.start()
        lastTotal = newTotal
    }

    private fun triggerHapticFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    private fun playCashRegisterSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            // ToneGenerator es una solución rápida sin archivos externos.
            // Para un sonido real de "caja registradora", se necesitaría un archivo .mp3 en res/raw.
        } catch (e: Exception) {
            e.printStackTrace()
        }
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