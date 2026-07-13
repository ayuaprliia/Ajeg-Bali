package com.example.ajegbali.data.model

// Struktur data WayangCategory
enum class WayangCategory {
    DEWA, PROTAGONIS, ANTAGONIS, PUNAKAWAN
}

// Struktur data WayangCharacter
data class WayangCharacter(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val category: WayangCategory,
    val group: String,
    val traits: List<String>,
    val description: String,
    val philosophy: String,
    val visualTraits: List<String>,
    val imageResId: Int,
    val modelClassId: Int
)
