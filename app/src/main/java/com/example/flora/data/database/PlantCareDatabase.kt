package com.example.flora.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.flora.data.database.dao.CareRequirementsDao
import com.example.flora.data.database.dao.CareScheduleDao
import com.example.flora.data.database.dao.DiseaseDao
import com.example.flora.data.database.dao.FlowerSpeciesDao
import com.example.flora.data.database.dao.PlantDao
import com.example.flora.data.database.dao.PlantHealthLogDao
import com.example.flora.data.database.entities.CareRequirements
import com.example.flora.data.database.entities.CareSchedule
import com.example.flora.data.database.entities.Disease
import com.example.flora.data.database.entities.FlowerSpecies
import com.example.flora.data.database.entities.Plant
import com.example.flora.data.database.entities.PlantHealthLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Plant::class,
        FlowerSpecies::class,
        CareSchedule::class,
        Disease::class,
        CareRequirements::class,
        PlantHealthLog::class
    ],
    version = 6,
    exportSchema = false
)
abstract class PlantCareDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun flowerSpeciesDao(): FlowerSpeciesDao
    abstract fun careScheduleDao(): CareScheduleDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun careRequirementsDao(): CareRequirementsDao
    abstract fun plantHealthLogDao(): PlantHealthLogDao

    companion object {
        @Volatile
        private var INSTANCE: PlantCareDatabase? = null

        fun getInstance(context: Context): PlantCareDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlantCareDatabase::class.java,
                    "flora_plant_care_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(SeedCallback())
                    .build()
                    .also { INSTANCE = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "plants", "userId")) {
                    db.execSQL("ALTER TABLE plants ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "care_schedules", "userId")) {
                    db.execSQL("ALTER TABLE care_schedules ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL(
                    """
                    UPDATE care_schedules
                    SET userId = COALESCE(
                        (SELECT plants.userId FROM plants WHERE plants.id = care_schedules.plantId),
                        0
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Clean slate: wipe all user-generated data. Keep reference tables
                // (flower_species, diseases, care_requirements) intact.
                db.execSQL("DELETE FROM plant_health_logs")
                db.execSQL("DELETE FROM care_schedules")
                db.execSQL("DELETE FROM plants")
                // Wipe reference data so refreshed seed runs on next app start
                db.execSQL("DELETE FROM flower_species")
                db.execSQL("DELETE FROM diseases")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT INTO flower_species (commonName, malayName, scientificName, careLevel, isNative, description, bloomSeason, wateringFrequency, sunlight)
                    SELECT 'Crape Jasmine', 'Bunga Susun Kelapa', 'Tabernaemontana divaricata', 'Easy', 0,
                           'Evergreen tropical shrub with glossy leaves and white pinwheel-shaped flowers. It tolerates Malaysian heat well, prefers evenly moist soil, and blooms best with morning sun plus light afternoon shade. Prune lightly after flowering to keep it bushy and encourage more blooms.',
                           'Year-round; strongest after warm rainy weeks',
                           'Every 1-2 days; keep soil lightly moist',
                           'Full sun to partial shade'
                    WHERE NOT EXISTS (
                        SELECT 1 FROM flower_species WHERE commonName = 'Crape Jasmine'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO flower_species (commonName, malayName, scientificName, careLevel, isNative, description, bloomSeason, wateringFrequency, sunlight)
                    SELECT 'Ixora', 'Jejarum', 'Ixora coccinea', 'Easy', 1,
                           'Compact tropical shrub with dense clusters of red, orange, pink, or yellow flowers. Common in Malaysian gardens and well-suited to hedges or pots. Ixora prefers warmth, humidity, and slightly acidic soil. Remove spent clusters to promote repeat blooming.',
                           'Year-round; peaks in sunny months',
                           'Every 1-2 days in hot weather; reduce slightly during heavy rain',
                           'Full sun to partial shade'
                    WHERE NOT EXISTS (
                        SELECT 1 FROM flower_species WHERE commonName = 'Ixora'
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Disease catalog expanded from 5 → 30 entries; wipe to trigger re-seed.
                db.execSQL("DELETE FROM diseases")
            }
        }

        private fun columnExists(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
            val cursor = db.query("PRAGMA table_info($tableName)")
            return try {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex == -1) return false
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == columnName) {
                        found = true
                        break
                    }
                }
                found
            } finally {
                cursor.close()
            }
        }
    }

    fun seedReferenceDataIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            if (flowerSpeciesDao().getAllOnce().isEmpty()) {
                SeedCallback.seedFlowerSpecies(flowerSpeciesDao())
            }
            if (diseaseDao().getAllOnce().isEmpty()) {
                SeedCallback.seedDiseases(diseaseDao())
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedFlowerSpecies(database.flowerSpeciesDao())
                    seedDiseases(database.diseaseDao())
                }
            }
        }

        companion object {
        suspend fun seedFlowerSpecies(dao: FlowerSpeciesDao) {
            dao.insertAll(
                listOf(
                    FlowerSpecies(
                        commonName = "Hibiscus",
                        malayName = "Bunga Raya",
                        scientificName = "Hibiscus rosa-sinensis",
                        careLevel = "Easy",
                        isNative = true,
                        description = "Malaysia's national flower since 1960. Large 5-petalled blooms 10-15cm wide in red, pink, yellow, orange, or white. Each flower lasts one day but plant blooms continuously. Thrives outdoors in Malaysian humidity; prune every 2-3 months to encourage branching. Use balanced NPK 15-15-15 fertilizer monthly.",
                        bloomSeason = "Year-round (peak Mar-Oct)",
                        wateringFrequency = "Daily in dry weather, every 2 days in rainy season",
                        sunlight = "Full sun (min 6 hours direct)"
                    ),
                    FlowerSpecies(
                        commonName = "Orchid",
                        malayName = "Orkid",
                        scientificName = "Dendrobium sp.",
                        careLevel = "Moderate",
                        isNative = false,
                        description = "Epiphytic orchid — grow in bark/charcoal mix, NEVER regular soil. Blooms last 4-8 weeks. Water by soaking pot for 10 min then drain fully. Feed with diluted orchid fertilizer (quarter-strength) weekly. Yellow leaves = overwatering. Hang under shade net (50-70% shade) or bright east-facing balcony.",
                        bloomSeason = "2-3 flushes per year",
                        wateringFrequency = "Every 3-5 days; let roots dry between waterings",
                        sunlight = "Bright indirect light; no direct afternoon sun"
                    ),
                    FlowerSpecies(
                        commonName = "Bougainvillea",
                        malayName = "Bunga Kertas",
                        scientificName = "Bougainvillea spectabilis",
                        careLevel = "Easy",
                        isNative = false,
                        description = "Drought-tolerant woody climber. Colourful 'flowers' are actually bracts (modified leaves) in magenta, purple, orange, white, or pink. Stress blooming: water LESS and withhold fertilizer to trigger flowering. Prune after each bloom cycle. Handle carefully — has sharp thorns. Ideal for fences, trellises, pot culture on balconies.",
                        bloomSeason = "Year-round; heaviest in dry spells",
                        wateringFrequency = "Every 4-5 days; drought triggers bloom",
                        sunlight = "Full sun (min 6 hours); less sun = fewer bracts"
                    ),
                    FlowerSpecies(
                        commonName = "Jasmine",
                        malayName = "Melur",
                        scientificName = "Jasminum sambac",
                        careLevel = "Moderate",
                        isNative = false,
                        description = "National flower of the Philippines/Indonesia; used in Malay weddings and religious offerings. Small white waxy flowers strongest scent at night. Pinch tips to encourage bushiness. Feed monthly with potassium-rich fertilizer for more blooms. Susceptible to spider mites in dry conditions — mist leaves regularly.",
                        bloomSeason = "Year-round; peaks Jun-Sep",
                        wateringFrequency = "Every 1-2 days; soil should stay moist not soggy",
                        sunlight = "Full sun to partial shade (4-6 hours)"
                    ),
                    FlowerSpecies(
                        commonName = "Frangipani",
                        malayName = "Kemboja",
                        scientificName = "Plumeria rubra",
                        careLevel = "Easy",
                        isNative = false,
                        description = "Small tropical tree reaching 4-8m. Fragrant 5-petalled flowers (white, yellow, pink, red) bloom in clusters at branch tips. All parts contain toxic milky sap — wash hands after pruning. Drought tolerant once established. Propagate easily from 30cm stem cuttings. Drops leaves during very dry spells (normal). Associated with temples and cemeteries across SE Asia.",
                        bloomSeason = "Mar-Nov (peaks May-Sep)",
                        wateringFrequency = "Every 4-7 days; avoid overwatering",
                        sunlight = "Full sun (min 6 hours)"
                    ),
                    FlowerSpecies(
                        commonName = "Crape Jasmine",
                        malayName = "Bunga Susun Kelapa",
                        scientificName = "Tabernaemontana divaricata",
                        careLevel = "Easy",
                        isNative = false,
                        description = "Evergreen shrub with white pinwheel flowers and glossy leaves. It thrives in warm, humid conditions and works well in pots, borders, or small hedges. Keep the soil consistently moist but not soggy, and prune lightly after flowering to maintain shape and encourage branching.",
                        bloomSeason = "Year-round; strongest after rainy periods",
                        wateringFrequency = "Every 1-2 days; avoid letting soil dry out fully",
                        sunlight = "Full sun to partial shade"
                    ),
                    FlowerSpecies(
                        commonName = "Ixora",
                        malayName = "Jejarum",
                        scientificName = "Ixora coccinea",
                        careLevel = "Easy",
                        isNative = true,
                        description = "Popular Malaysian garden shrub with clustered star-shaped flowers in red, orange, yellow, or pink. It likes warmth, humidity, and bright light, and blooms repeatedly when regularly deadheaded. Use slightly acidic, well-draining soil for best growth.",
                        bloomSeason = "Year-round; strongest in sunny months",
                        wateringFrequency = "Every 1-2 days in warm weather",
                        sunlight = "Full sun to partial shade"
                    )
                )
            )
        }

        suspend fun seedDiseases(dao: DiseaseDao) {
            dao.insertAll(
                listOf(
                    Disease(
                        name = "Powdery Mildew",
                        category = "Fungal",
                        symptoms = "White powdery coating on leaves, stunted growth, yellowing of leaf edges, curled or twisted new growth",
                        treatment = "Apply fungicide spray every 7 days\nRemove heavily infected leaves\nImprove air circulation around plant\nAvoid overhead watering\nSpray 1 tsp baking soda + 1 quart water as a home remedy",
                        prevention = "Ensure proper spacing, avoid overhead watering, use resistant varieties",
                        severityLevel = 2,
                        affectedParts = "Upper leaf surface, stems"
                    ),
                    Disease(
                        name = "Downy Mildew",
                        category = "Fungal",
                        symptoms = "Yellow patches on top of leaves with fuzzy grey-purple growth underneath, leaves dropping",
                        treatment = "Remove affected foliage\nApply copper-based fungicide\nWater early so leaves dry quickly\nThin out dense growth",
                        prevention = "Avoid wetting leaves, water in morning, ensure airflow, choose resistant cultivars",
                        severityLevel = 2,
                        affectedParts = "Lower leaf surfaces"
                    ),
                    Disease(
                        name = "Leaf Spot Disease",
                        category = "Fungal",
                        symptoms = "Brown or black circular spots on leaves with yellow halos, spots merging into large blotches",
                        treatment = "Remove and dispose of infected leaves\nApply copper-based fungicide\nWater at soil level only\nImprove drainage around plant",
                        prevention = "Avoid overhead watering, ensure good air circulation, clean up fallen leaves",
                        severityLevel = 1,
                        affectedParts = "Leaf surfaces"
                    ),
                    Disease(
                        name = "Early Blight",
                        category = "Fungal",
                        symptoms = "Dark concentric rings (target-spot pattern) on lower leaves, yellowing around lesions, leaf drop",
                        treatment = "Prune affected lower leaves\nApply chlorothalonil or copper fungicide weekly\nMulch base of plant to block soil splash",
                        prevention = "Crop rotation, stake plants to keep foliage dry, water at base, remove debris in autumn",
                        severityLevel = 2,
                        affectedParts = "Lower leaves, stems, fruit"
                    ),
                    Disease(
                        name = "Late Blight",
                        category = "Fungal",
                        symptoms = "Water-soaked grey-green leaf patches turning brown, white fuzzy growth underside, rapidly spreading",
                        treatment = "Remove and destroy infected plants — do NOT compost\nApply copper or mancozeb fungicide preventively\nClean tools between plants",
                        prevention = "Plant resistant varieties, avoid overhead watering, harvest tubers in dry weather",
                        severityLevel = 3,
                        affectedParts = "Leaves, stems, fruit, tubers"
                    ),
                    Disease(
                        name = "Bacterial Spot",
                        category = "Bacterial",
                        symptoms = "Small dark water-soaked spots on leaves and fruit, spots dry out and crack, defoliation",
                        treatment = "Remove infected plant material\nApply copper-based bactericide every 7-10 days\nAvoid working with plants when wet",
                        prevention = "Use disease-free seed, rotate crops, sanitize tools, avoid overhead irrigation",
                        severityLevel = 2,
                        affectedParts = "Leaves, stems, fruit"
                    ),
                    Disease(
                        name = "Mosaic Virus",
                        category = "Viral",
                        symptoms = "Mottled light-and-dark green pattern on leaves, distorted or stunted growth, reduced yield",
                        treatment = "No cure — remove and destroy infected plants\nDisinfect tools with 10% bleach between plants\nControl aphid and whitefly vectors",
                        prevention = "Plant resistant varieties, wash hands and tools, control insect vectors, avoid tobacco around plants",
                        severityLevel = 3,
                        affectedParts = "Whole plant"
                    ),
                    Disease(
                        name = "Yellow Leaf Curl Virus",
                        category = "Viral",
                        symptoms = "Leaves curl upward and turn yellow, stunted growth, flowers drop before fruiting",
                        treatment = "Remove infected plants\nControl whiteflies with yellow sticky traps + insecticidal soap\nProtect new transplants under fine mesh",
                        prevention = "Plant resistant varieties, install whitefly screens, control nearby weed reservoirs",
                        severityLevel = 3,
                        affectedParts = "Leaves, flowers"
                    ),
                    Disease(
                        name = "Rust",
                        category = "Fungal",
                        symptoms = "Orange, yellow or reddish-brown pustules on undersides of leaves, leaves turn yellow and drop",
                        treatment = "Remove infected leaves promptly\nApply sulfur or myclobutanil fungicide\nThin dense foliage\nClean up fallen leaves",
                        prevention = "Avoid overhead watering, ensure airflow, plant resistant varieties",
                        severityLevel = 2,
                        affectedParts = "Leaf undersides, stems"
                    ),
                    Disease(
                        name = "Anthracnose",
                        category = "Fungal",
                        symptoms = "Sunken dark lesions on leaves, stems and fruit, irregular blackened tissue, twig dieback",
                        treatment = "Prune out infected branches\nApply chlorothalonil or copper fungicide\nDestroy fallen debris",
                        prevention = "Rake fallen leaves, prune for airflow, avoid wounding plant, mulch base",
                        severityLevel = 2,
                        affectedParts = "Leaves, stems, fruit"
                    ),
                    Disease(
                        name = "Black Rot",
                        category = "Fungal",
                        symptoms = "Brown circular leaf lesions with concentric rings, black fruit rot, dried mummified fruit clinging to plant",
                        treatment = "Remove and destroy mummified fruit + cankers\nApply mancozeb or captan fungicide bloom-onward\nPrune for sunlight",
                        prevention = "Sanitation in autumn, resistant cultivars, dormant copper spray",
                        severityLevel = 2,
                        affectedParts = "Leaves, fruit, stems"
                    ),
                    Disease(
                        name = "Root Rot",
                        category = "Fungal",
                        symptoms = "Wilting despite moist soil, yellowing leaves, mushy brown roots, foul smell from soil",
                        treatment = "Remove plant from soil\nTrim affected roots\nRepot in fresh, well-draining mix\nReduce watering frequency",
                        prevention = "Use well-draining soil, avoid overwatering, ensure pots have drainage holes",
                        severityLevel = 3,
                        affectedParts = "Roots, lower stems"
                    ),
                    Disease(
                        name = "Damping Off",
                        category = "Fungal",
                        symptoms = "Seedlings collapse at soil line, water-soaked stems, sudden death of young plants",
                        treatment = "Discard affected seedlings + soil\nUse sterile seed-starting mix\nReduce watering, increase airflow",
                        prevention = "Sterilize containers, don't overwater seedlings, use bottom heat to speed germination",
                        severityLevel = 3,
                        affectedParts = "Seedlings, stem base"
                    ),
                    Disease(
                        name = "Fusarium Wilt",
                        category = "Fungal",
                        symptoms = "One-sided yellowing and wilting of leaves, brown vascular tissue when stem is cut, eventual plant death",
                        treatment = "No cure — remove and destroy infected plants\nSolarize soil before replanting\nUse resistant varieties",
                        prevention = "Crop rotation, resistant cultivars, sanitize tools, avoid wounding roots",
                        severityLevel = 3,
                        affectedParts = "Vascular system, whole plant"
                    ),
                    Disease(
                        name = "Verticillium Wilt",
                        category = "Fungal",
                        symptoms = "V-shaped yellow patches on leaves, wilting in heat that recovers at night, vascular browning",
                        treatment = "Remove infected branches\nNo systemic cure — extend life with adequate water and balanced feed\nReplant with resistant species",
                        prevention = "Plant resistant cultivars, soil solarization, avoid susceptible hosts",
                        severityLevel = 3,
                        affectedParts = "Vascular tissue, leaves"
                    ),
                    Disease(
                        name = "Botrytis (Grey Mould)",
                        category = "Fungal",
                        symptoms = "Fuzzy grey-brown mould on flowers, buds and leaves, soft watery decay, blossom drop",
                        treatment = "Remove infected tissue immediately\nReduce humidity + improve airflow\nApply iprodione or copper fungicide",
                        prevention = "Avoid overhead watering, space plants, prune for ventilation, harvest in dry weather",
                        severityLevel = 2,
                        affectedParts = "Flowers, fruit, leaves, stems"
                    ),
                    Disease(
                        name = "Sooty Mould",
                        category = "Fungal",
                        symptoms = "Black soot-like coating on leaves, often above sticky honeydew from sap-sucking insects",
                        treatment = "Control underlying insect (aphid/whitefly/scale/mealybug)\nWipe leaves with mild soapy water\nPrune heavily coated branches",
                        prevention = "Inspect for sap-suckers regularly, encourage beneficial insects, hose down sticky residue early",
                        severityLevel = 1,
                        affectedParts = "Leaf surfaces, stems"
                    ),
                    Disease(
                        name = "Citrus Greening (HLB)",
                        category = "Bacterial",
                        symptoms = "Asymmetrical yellow blotchy mottling on leaves, lopsided green-bottom fruit, bitter taste",
                        treatment = "No cure — remove infected trees to protect neighbors\nControl Asian citrus psyllid vector\nProvide micronutrient foliar feeds to extend life",
                        prevention = "Buy certified disease-free trees, monitor for psyllids, report suspect trees to extension office",
                        severityLevel = 3,
                        affectedParts = "Leaves, fruit, whole tree"
                    ),
                    Disease(
                        name = "Aphid Infestation",
                        category = "Pest",
                        symptoms = "Sticky residue on leaves, curling leaves, tiny green/black insects clustered on new growth and stems",
                        treatment = "Spray with water jet to dislodge aphids\nApply neem oil solution weekly\nIntroduce ladybugs as natural predators\nApply insecticidal soap spray",
                        prevention = "Regular inspection, avoid over-fertilizing with nitrogen, encourage beneficial insects",
                        severityLevel = 2,
                        affectedParts = "New growth, undersides of leaves, stems"
                    ),
                    Disease(
                        name = "Whitefly Infestation",
                        category = "Pest",
                        symptoms = "Tiny white flying insects when disturbed, sticky honeydew on leaves, sooty mould growth",
                        treatment = "Apply yellow sticky traps\nSpray neem oil solution\nUse insecticidal soap\nIntroduce natural predators (Encarsia wasp)",
                        prevention = "Regular inspection, companion planting with marigolds, maintain plant health",
                        severityLevel = 2,
                        affectedParts = "Undersides of leaves"
                    ),
                    Disease(
                        name = "Spider Mites",
                        category = "Pest",
                        symptoms = "Fine yellow stippling on leaves, fine webbing on undersides and between leaves, leaves drying out",
                        treatment = "Rinse plant thoroughly with water\nApply miticide or insecticidal soap every 5 days\nIncrease humidity around plant",
                        prevention = "Keep humidity up indoors, regular inspection, isolate new plants for 2 weeks",
                        severityLevel = 2,
                        affectedParts = "Leaf undersides, stems"
                    ),
                    Disease(
                        name = "Mealybugs",
                        category = "Pest",
                        symptoms = "White cottony masses in leaf joints and undersides, sticky honeydew, stunted growth",
                        treatment = "Dab insects with 70% isopropyl alcohol on a cotton swab\nSpray neem oil weekly\nApply insecticidal soap for heavy infestations",
                        prevention = "Quarantine new plants, inspect monthly, avoid over-fertilizing",
                        severityLevel = 2,
                        affectedParts = "Leaf axils, stems, roots"
                    ),
                    Disease(
                        name = "Scale Insects",
                        category = "Pest",
                        symptoms = "Brown/tan bumps stuck to stems and leaf veins, sticky residue, yellowing leaves",
                        treatment = "Scrape off with fingernail or soft brush\nApply horticultural oil spray\nPrune heavily infested branches",
                        prevention = "Quarantine new plants, regular leaf inspection, encourage natural predators",
                        severityLevel = 2,
                        affectedParts = "Stems, leaf undersides"
                    ),
                    Disease(
                        name = "Thrips",
                        category = "Pest",
                        symptoms = "Silvery streaks on leaves, black specks of frass, distorted new growth, scarred flowers",
                        treatment = "Hang blue sticky traps\nSpray spinosad or insecticidal soap\nRemove heavily infested buds",
                        prevention = "Reflective mulch deters them, monitor with sticky cards, control weed hosts",
                        severityLevel = 2,
                        affectedParts = "Flowers, new leaves, buds"
                    ),
                    Disease(
                        name = "Fungus Gnats",
                        category = "Pest",
                        symptoms = "Tiny black flies hovering over soil, larvae in top inch of soil, weak seedlings",
                        treatment = "Let soil dry between waterings\nApply BTI mosquito bits to soil\nUse yellow sticky traps for adults",
                        prevention = "Avoid soggy soil, cover soil with sand or grit, bottom-water plants",
                        severityLevel = 1,
                        affectedParts = "Soil, seedling roots"
                    ),
                    Disease(
                        name = "Caterpillar Damage",
                        category = "Pest",
                        symptoms = "Chewed leaves with ragged edges or holes, black frass on foliage, visible caterpillars",
                        treatment = "Hand-pick caterpillars at dusk\nSpray BT (Bacillus thuringiensis) for organic control\nEncourage parasitic wasps",
                        prevention = "Row cover during egg-laying season, inspect undersides of leaves weekly",
                        severityLevel = 1,
                        affectedParts = "Leaves, buds, fruit"
                    ),
                    Disease(
                        name = "Nutrient Deficiency",
                        category = "Nutritional",
                        symptoms = "Yellowing leaves (nitrogen), purple tint (phosphorus), brown leaf edges (potassium), interveinal yellowing (iron/magnesium)",
                        treatment = "Apply balanced fertilizer matching the deficiency\nFor iron: chelated iron foliar spray\nFor magnesium: 1 tbsp Epsom salt per gallon",
                        prevention = "Regular feeding schedule, soil test annually, mulch to retain nutrients",
                        severityLevel = 1,
                        affectedParts = "Whole plant"
                    ),
                    Disease(
                        name = "Sunburn / Leaf Scorch",
                        category = "Environmental",
                        symptoms = "Bleached or brown crispy patches on leaves exposed to direct sun, especially after relocation",
                        treatment = "Move plant to filtered light\nTrim severely scorched leaves\nMaintain consistent watering",
                        prevention = "Acclimate plants gradually to new light levels, provide afternoon shade in summer",
                        severityLevel = 1,
                        affectedParts = "Upper leaves"
                    ),
                    Disease(
                        name = "Overwatering",
                        category = "Environmental",
                        symptoms = "Soft yellow leaves, soggy soil, fungal growth on soil surface, stem mushy at base",
                        treatment = "Stop watering until top 2 cm of soil is dry\nCheck drainage, repot if root rot is starting\nImprove airflow",
                        prevention = "Use well-draining mix, water only when needed, avoid saucers full of water",
                        severityLevel = 2,
                        affectedParts = "Roots, lower stem, lower leaves"
                    ),
                    Disease(
                        name = "Underwatering",
                        category = "Environmental",
                        symptoms = "Crispy brown leaf edges, wilting, dry soil pulling away from pot edges, leaf drop",
                        treatment = "Soak pot in water bath for 20 min\nResume regular schedule\nMist humidity-loving plants",
                        prevention = "Set watering reminders, group thirsty plants together, mulch to retain moisture",
                        severityLevel = 1,
                        affectedParts = "Leaves, whole plant"
                    ),
                )
            )
        }
        }
    }
}
