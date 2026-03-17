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
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.FragmentThirdBinding
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class ThirdFragment : Fragment() {

    private var _binding: FragmentThirdBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ItemAdapter
    private val db by lazy { AppDatabase.getDatabase(requireContext()) }
    private var lastTotal = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThirdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ItemAdapter(mutableListOf()) { _ -> }
        binding.recyclerView.adapter = adapter

        loadItems()
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    fun loadItems() {
        if (_binding == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. Get ALL incomes for the total sum dashboard
            val allIncomes = db.itemDao().getByType("INCOME")
            updateTotalIncomes(allIncomes)

            // 2. Get ONLY today's incomes for the list
            val startOfDay = getStartOfDay()
            val endOfDay = getEndOfDay()
            val todaysIncomes = db.itemDao().getByTypeAndDate("INCOME", startOfDay, endOfDay)
            adapter.updateItems(todaysIncomes)
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun updateTotalIncomes(items: List<Item>) {
        val newTotal = items.sumOf { it.amount * it.count }
        animateIncomes(newTotal)
    }

    private fun animateIncomes(newTotal: Double) {
        if (newTotal > lastTotal) {
            binding.totalIncomesText.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    binding.totalIncomesText.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }.start()
        }

        val animator = ValueAnimator.ofFloat(lastTotal.toFloat(), newTotal.toFloat())
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            binding.totalIncomesText.text = String.format(Locale.getDefault(), "+$%.2f", animatedValue)
        }
        animator.start()
        lastTotal = newTotal
    }

    fun addItem(name: String, amount: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            val newItem = Item(name = name, amount = amount, count = 1, type = "INCOME")
            db.itemDao().insert(newItem)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}