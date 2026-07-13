package com.example.ajegbali.data.remote.repository

import com.example.ajegbali.core.characters
import com.example.ajegbali.data.model.WayangCategory
import com.example.ajegbali.data.model.WayangCharacter

/**
 * Repository containing all 16 Balinese Wayang Kulit characters.
 * Class IDs correspond to YOLO model output indices (alphabetical order).
 */
object WayangRepository {

    /** Get all 16 characters */
    fun getAll(): List<WayangCharacter> = characters

    /** Get character by ID string */
    fun getById(id: String): WayangCharacter? = characters.find { it.id == id }

    /** Get multiple characters by their IDs */
    fun getByIds(ids: List<String>): List<WayangCharacter> = characters.filter { it.id in ids }

    /** Get character by YOLO model class ID */
    fun getByClassId(classId: Int): WayangCharacter? = characters.find { it.modelClassId == classId }

    /** Get characters filtered by category */
    fun getByCategory(category: WayangCategory): List<WayangCharacter> =
        characters.filter { it.category == category }

    /** Search characters by name or alias */
    fun search(query: String): List<WayangCharacter> {
        if (query.isBlank()) return characters
        val lowerQuery = query.lowercase()
        return characters.filter { char ->
            char.name.lowercase().contains(lowerQuery) ||
                    char.aliases.any { it.lowercase().contains(lowerQuery) } ||
                    char.group.lowercase().contains(lowerQuery)
        }
    }
}
