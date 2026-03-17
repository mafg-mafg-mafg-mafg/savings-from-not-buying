package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
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
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        
        adapter = ItemAdapter(mutableListOf()) { item ->
            onItemClicked(item)
        }
        binding.recyclerView.adapter = adapter

        setupSwipeToDelete()
        loadItems()
    }

    private fun onItemClicked(item: Item) {
        viewLifecycleOwner.lifecycleScope.launch {
            val updatedItem = item.copy(count = item.count + 1)
            db.itemDao().update(updatedItem)
            loadItems()
            triggerHapticFeedback()
            playCashRegisterSound()
            triggerCelebration()
        }
    }

    private fun triggerCelebration() {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0x8BC34A, 0x4FC3F7, 0xFFEB3B, 0xFF5252),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
        )
        binding.konfettiView.start(party)
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

    fun loadItems() {
        if (_binding == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            // Filter by SAVING type to ensure counters are visible and data is correct
            val items = db.itemDao().getByType("SAVING")
            adapter.updateItems(items)
            updateTotalSavings(items)
        }
    }

    private fun updateTotalSavings(items: List<Item>) {
        val newTotal = items.sumOf { it.amount * it.count }
        animateSavings(newTotal)
    }

    private fun animateSavings(newTotal: Double) {
        if (newTotal > lastTotal) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addItem(name: String, amount: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            val newItem = Item(name = name, amount = amount, count = 1, type = "SAVING")
            db.itemDao().insert(newItem)
            loadItems()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}