package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double = 0.0,
    val count: Int = 1,
    val type: String = "SAVING" // SAVING or INCOME
)