package com.example.flora.ml

/**
 * Per-species watering defaults. Used when an identified plant has no curated
 * [com.example.flora.data.database.entities.FlowerSpecies] DB row, so the
 * auto-built care schedule still reflects real care needs instead of a flat
 * fallback.
 *
 * Backed by [PlantCareProfiles] — the single care knowledge base shared with
 * the identification screen's "About" copy, so the schedule interval and the
 * written advice never disagree.
 */
object PlantCareDefaults {

    /**
     * Days between waterings, resolved from a scientific and/or common name.
     */
    fun wateringIntervalDays(scientificName: String, commonName: String = ""): Int =
        PlantCareProfiles.forName(scientificName, commonName).waterDays
}
