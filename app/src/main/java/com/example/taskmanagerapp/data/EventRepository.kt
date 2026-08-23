package com.example.taskmanagerapp.data

import kotlinx.coroutines.flow.Flow

class EventRepository(private val dao: EventDao) {

    val allEvents: Flow<List<Event>> = dao.getAllEvents()

    suspend fun insert(event: Event) {
        dao.insertEvent(event)
    }

    suspend fun update(event: Event) {
        dao.updateEvent(event)
    }

    suspend fun delete(event: Event) {
        dao.deleteEvent(event)
    }
}