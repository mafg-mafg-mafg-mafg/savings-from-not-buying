package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.lifecycle.lifecycleScope
import com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados.databinding.FragmentThirdBinding
import kotlinx.coroutines.launch
import java.util.Locale

class ThirdFragment : Fragment() {

    private var _binding: FragmentThirdBinding? = null
    private val binding get() = _binding!!
    
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
        loadIncomes()
    }

    override fun onResume() {
        super.onResume()
        loadIncomes()
    }

    fun loadIncomes() {
        if (_binding == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            val items = db.itemDao().getAll()
            updateTotalIncomes(items)
        }
    }

    private fun updateTotalIncomes(items: List<Item>) {
        // En esta app, los "Ingresos" se calculan igual que los ahorros pero se muestran en la pantalla azul
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}