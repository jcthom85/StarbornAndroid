package com.example.starborn.domain.repository

import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.World

interface WorldRepository {
    suspend fun getWorlds(): List<World>
    suspend fun getHubs(): List<Hub>
    suspend fun getRooms(): List<Room>
}
