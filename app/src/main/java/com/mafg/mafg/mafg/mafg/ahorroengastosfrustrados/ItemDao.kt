package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    suspend fun getAll(): List<Item>

    @Insert
    suspend fun insert(item: Item)
}