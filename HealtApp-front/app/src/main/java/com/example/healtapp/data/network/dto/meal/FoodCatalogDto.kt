package com.example.healtapp.data.network.dto.meal

import com.google.gson.annotations.SerializedName

data class FoodCatalogItemDto(
    val id: Int? = null,
    val barcode: String? = null,
    val name: String,
    val brand: String? = null,
    @SerializedName("calories_100g") val calories100g: Float? = null,
    @SerializedName("protein_g_100g") val proteinG100g: Float? = null,
    @SerializedName("fat_g_100g") val fatG100g: Float? = null,
    @SerializedName("carbs_g_100g") val carbsG100g: Float? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    val source: String = "user",
    @SerializedName("is_complete") val isComplete: Boolean = false,
    @SerializedName("needs_completion") val needsCompletion: Boolean = false,
) {
    fun macroSubtitle(): String {
        val parts = mutableListOf<String>()
        calories100g?.let { parts.add("${it.toInt()} ккал/100 г") }
        if (proteinG100g != null || fatG100g != null || carbsG100g != null) {
            parts.add(
                "Б ${proteinG100g?.let { "%.1f".format(it) } ?: "—"} · " +
                    "Ж ${fatG100g?.let { "%.1f".format(it) } ?: "—"} · " +
                    "У ${carbsG100g?.let { "%.1f".format(it) } ?: "—"}",
            )
        } else if (needsCompletion) {
            parts.add("Нужно дополнить БЖУ")
        }
        brand?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        return parts.joinToString(" · ")
    }
}

data class FoodCatalogSearchResponseDto(
    val items: List<FoodCatalogItemDto> = emptyList(),
)

data class FoodCatalogUpsertRequestDto(
    val barcode: String? = null,
    val name: String? = null,
    val brand: String? = null,
    @SerializedName("calories_100g") val calories100g: Float? = null,
    @SerializedName("protein_g_100g") val proteinG100g: Float? = null,
    @SerializedName("fat_g_100g") val fatG100g: Float? = null,
    @SerializedName("carbs_g_100g") val carbsG100g: Float? = null,
    @SerializedName("off_image_url") val offImageUrl: String? = null,
)
