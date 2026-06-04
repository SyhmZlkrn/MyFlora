package com.example.flora.ml

/**
 * Care knowledge base for plants that have no curated [com.example.flora.data
 * .database.entities.FlowerSpecies] row in the local DB.
 *
 * Identification (PlantNet API or the on-device model) returns a scientific
 * name. We resolve that name to a [CareProfile] — light, watering cadence,
 * soil, feeding and a written "About" paragraph — so the result card and the
 * auto-built care schedule reflect that plant's real needs instead of one flat
 * fallback.
 *
 * Resolution order, most specific first:
 *   1. common-name keyword (e.g. "snake plant", "succulent")
 *   2. genus from the scientific name (e.g. "Hibiscus" in "Hibiscus rosa-sinensis")
 *   3. generic flowering-ornamental default
 *
 * Genus is the right granularity for hobby care — care varies far more between
 * genera than between species within a genus, and the classifier is often only
 * genus-confident anyway.
 */
object PlantCareProfiles {

    /**
     * @param waterDays   days between waterings (drives the auto schedule)
     * @param sunlight    short label for the result-card chip
     * @param soil        soil / potting preference
     * @param feeding     fertiliser cadence + type
     * @param about       full written care paragraph for the "About" section
     */
    data class CareProfile(
        val waterDays: Int,
        val sunlight: String,
        val soil: String,
        val feeding: String,
        val about: String,
    ) {
        /** Short watering label for the result-card chip, derived from [waterDays]. */
        val wateringSummary: String
            get() = when (waterDays) {
                1 -> "Daily"
                in 2..6 -> "Every $waterDays days"
                7 -> "Weekly"
                in 8..13 -> "Every $waterDays days"
                14 -> "Every 2 weeks"
                else -> "Every $waterDays days"
            }
    }

    /**
     * Resolve a [CareProfile] from a scientific and/or common name.
     */
    fun forName(scientificName: String, commonName: String = ""): CareProfile {
        val common = commonName.lowercase()
        val sci = scientificName.lowercase()
        val genus = sci.split(" ").firstOrNull().orEmpty()

        // ── Common-name keyword hits (more specific than genus) ──
        keywordProfile(common)?.let { return it }

        // ── Genus match ──
        return genusProfile(genus) ?: GENERIC
    }

    // ── Keyword-driven profiles ──────────────────────────────────────────────

    private fun keywordProfile(common: String): CareProfile? = when {
        "cactus" in common -> CACTUS
        "succulent" in common -> SUCCULENT
        "snake plant" in common || "sansevieria" in common -> SNAKE_PLANT
        "pothos" in common || "money plant" in common -> POTHOS
        "peace lily" in common -> PEACE_LILY
        "aloe" in common -> SUCCULENT
        "fern" in common -> FERN
        "bamboo" in common -> BAMBOO
        "rubber plant" in common -> FICUS
        "lavender" in common -> LAVENDER
        "rosemary" in common || "thyme" in common || "oregano" in common -> MED_HERB
        "basil" in common || "mint" in common -> LEAFY_HERB
        "tomato" in common || "chili" in common || "chilli" in common || "pepper" in common -> VEGETABLE
        "orchid" in common -> ORCHID
        else -> null
    }

    // ── Genus-driven profiles ────────────────────────────────────────────────

    private fun genusProfile(genus: String): CareProfile? = when (genus) {
        "rosa" -> ROSE
        "hibiscus" -> HIBISCUS
        "bougainvillea" -> BOUGAINVILLEA
        "ixora" -> IXORA
        "jasminum" -> JASMINE
        "tabernaemontana" -> CRAPE_JASMINE
        "lavandula" -> LAVENDER
        "rosmarinus", "thymus", "origanum" -> MED_HERB
        "ocimum", "mentha" -> LEAFY_HERB
        "salvia" -> MED_HERB
        "phalaenopsis", "dendrobium", "cattleya", "vanda", "oncidium" -> ORCHID
        "ficus" -> FICUS
        "monstera", "philodendron", "epipremnum", "scindapsus" -> AROID
        "alocasia", "colocasia" -> ELEPHANT_EAR
        "sansevieria", "dracaena" -> SNAKE_PLANT
        "aloe", "agave", "echeveria", "sedum", "haworthia", "crassula", "kalanchoe" -> SUCCULENT
        "opuntia", "mammillaria", "ferocactus", "echinopsis" -> CACTUS
        "pelargonium", "geranium" -> GERANIUM
        "tagetes" -> MARIGOLD
        "petunia" -> PETUNIA
        "lilium" -> LILY
        "tulipa" -> TULIP
        "nymphaea" -> WATER_LILY
        "spathiphyllum" -> PEACE_LILY
        "calathea", "maranta" -> PRAYER_PLANT
        "dieffenbachia", "aglaonema" -> AROID
        "anthurium" -> ANTHURIUM
        "begonia" -> BEGONIA
        "fuchsia" -> FUCHSIA
        "gerbera" -> GERBERA
        "chrysanthemum" -> CHRYSANTHEMUM
        "dahlia" -> DAHLIA
        "zinnia" -> ZINNIA
        "impatiens" -> IMPATIENS
        "viola" -> PANSY
        "helianthus" -> SUNFLOWER
        "fragaria" -> STRAWBERRY
        "solanum", "capsicum" -> VEGETABLE
        "citrus" -> CITRUS
        "mangifera" -> MANGO
        "musa" -> BANANA
        "ananas" -> PINEAPPLE
        "psidium" -> GUAVA
        "carica" -> PAPAYA
        "cocos", "areca", "dypsis" -> PALM
        "pteris", "nephrolepis", "adiantum", "asplenium", "platycerium" -> FERN
        else -> null
    }

    // ── Profile definitions ──────────────────────────────────────────────────

    private val ROSE = CareProfile(
        waterDays = 3,
        sunlight = "Full sun (6+ hrs)",
        soil = "Rich, well-draining loam",
        feeding = "Balanced fertiliser monthly in growing season",
        about = "Roses thrive in at least 6 hours of direct sun. Water deeply at the base 2-3 " +
            "times a week — never on the leaves — and let the topsoil dry between waterings to " +
            "avoid black spot. Prune dead canes after each bloom flush and feed monthly with a " +
            "balanced fertiliser while the plant is actively growing.",
    )

    private val HIBISCUS = CareProfile(
        waterDays = 2,
        sunlight = "Full sun",
        soil = "Rich, well-draining soil",
        feeding = "High-potassium feed every 2 weeks while flowering",
        about = "Hibiscus loves bright direct sun and rich, well-draining soil. It is a heavy " +
            "drinker — water generously whenever the topsoil feels dry and mist the leaves on " +
            "hot days. Pinch off spent blooms to keep flowering, and feed every 2 weeks during " +
            "the flowering season. Protect it from temperatures below 10°C.",
    )

    private val BOUGAINVILLEA = CareProfile(
        waterDays = 5,
        sunlight = "Full sun",
        soil = "Lean, fast-draining soil",
        feeding = "Low-nitrogen bloom feed, sparingly",
        about = "Bougainvillea blooms hardest under mild stress: full sun, infrequent but deep " +
            "watering, and lean soil. Let it dry out fully between waterings, prune after each " +
            "flowering cycle, and feed sparingly — too much nitrogen produces leaves instead of " +
            "the colourful bracts. Handle with care; the stems carry sharp thorns.",
    )

    private val IXORA = CareProfile(
        waterDays = 3,
        sunlight = "Full sun to bright filtered",
        soil = "Slightly acidic, moist but well-draining",
        feeding = "Acid-loving fertiliser monthly",
        about = "Ixora wants bright, filtered sun and slightly acidic soil kept moist but never " +
            "soggy. Water once the top inch of soil dries, mulch the roots to hold moisture, and " +
            "feed monthly with an acid-loving fertiliser. Yellowing leaves usually signal an iron " +
            "deficiency — correct it with a chelated iron feed.",
    )

    private val JASMINE = CareProfile(
        waterDays = 3,
        sunlight = "Full sun with afternoon shade",
        soil = "Moist, well-draining soil",
        feeding = "Potassium-rich feed monthly",
        about = "Jasmine prefers bright sun with some afternoon shade in hot climates. Keep the " +
            "soil evenly moist and water when the top inch dries. Train the vines along a trellis, " +
            "prune lightly after flowering, and feed monthly through the growing season for the " +
            "strongest scent.",
    )

    private val CRAPE_JASMINE = CareProfile(
        waterDays = 2,
        sunlight = "Full sun to partial shade",
        soil = "Consistently moist, well-draining",
        feeding = "Balanced fertiliser monthly",
        about = "Crape Jasmine is an evergreen shrub with glossy leaves and white pinwheel " +
            "flowers. It likes warm, humid conditions and soil kept lightly moist but not soggy. " +
            "Prune after flowering to keep it bushy, and feed monthly during the growing season.",
    )

    private val LAVENDER = CareProfile(
        waterDays = 10,
        sunlight = "Full sun",
        soil = "Sandy, gritty, low-fertility soil",
        feeding = "Little to none — prefers lean soil",
        about = "Lavender wants full sun, sandy or gritty soil, and very little water once " +
            "established. Let the soil dry out almost completely between waterings. Prune after " +
            "flowering to keep it compact, and avoid feeding heavily — rich soil weakens both the " +
            "scent and the plant.",
    )

    private val MED_HERB = CareProfile(
        waterDays = 4,
        sunlight = "Full sun",
        soil = "Gritty, well-draining soil",
        feeding = "Light feeding only",
        about = "Mediterranean herbs such as rosemary, thyme and sage like full sun, gritty " +
            "well-draining soil, and moderate watering — let the topsoil dry between waterings. " +
            "Good airflow prevents mildew. Pinch the tips often, and keep feeding light; lean " +
            "conditions concentrate the flavour.",
    )

    private val LEAFY_HERB = CareProfile(
        waterDays = 2,
        sunlight = "Full sun to bright light",
        soil = "Rich, moisture-retentive soil",
        feeding = "Light balanced feed every 2-3 weeks",
        about = "Leafy herbs such as basil and mint are thirsty — keep the soil consistently " +
            "moist and water whenever the surface starts to dry. Give them bright light, pinch " +
            "the growing tips often to stay bushy, and harvest regularly so they keep producing " +
            "tender new leaves.",
    )

    private val ORCHID = CareProfile(
        waterDays = 7,
        sunlight = "Bright indirect light",
        soil = "Bark or charcoal orchid mix — never soil",
        feeding = "Weak orchid fertiliser weekly",
        about = "Orchids need bright indirect light and a full dry-back between waterings, " +
            "roughly once a week. Pot them in bark or charcoal mix, never garden soil. Water in " +
            "the morning and never let the roots sit in water. Feed weakly but weekly with an " +
            "orchid fertiliser during active growth.",
    )

    private val FICUS = CareProfile(
        waterDays = 5,
        sunlight = "Bright indirect light",
        soil = "Well-draining potting mix",
        feeding = "Balanced fertiliser monthly in growing season",
        about = "Ficus likes bright indirect light and a stable position — it drops leaves when " +
            "moved or draughted. Water when the top inch of soil dries, less in cooler months. " +
            "Wipe the leaves to keep them dust-free and feed monthly through the growing season.",
    )

    private val AROID = CareProfile(
        waterDays = 5,
        sunlight = "Bright indirect light",
        soil = "Chunky, airy, well-draining mix",
        feeding = "Balanced feed every 4-6 weeks",
        about = "Tropical aroids such as Monstera and Philodendron thrive in bright indirect " +
            "light and a chunky, well-draining mix. Water when the top 2 cm dries — yellow leaves " +
            "mean overwatering, crispy brown edges mean it is too dry. Wipe the leaves monthly " +
            "and feed every 4-6 weeks.",
    )

    private val ELEPHANT_EAR = CareProfile(
        waterDays = 3,
        sunlight = "Bright indirect light",
        soil = "Rich, moisture-retentive mix",
        feeding = "Balanced feed every 3-4 weeks",
        about = "Elephant ears (Alocasia, Colocasia) want warmth, humidity and bright indirect " +
            "light. Keep the soil consistently moist and water when the top centimetre dries. " +
            "They appreciate regular misting and a balanced feed every few weeks during active " +
            "growth; growth slows or pauses in cooler months.",
    )

    private val SNAKE_PLANT = CareProfile(
        waterDays = 10,
        sunlight = "Low to bright indirect light",
        soil = "Fast-draining, sandy mix",
        feeding = "Light feed 2-3 times a year",
        about = "Snake plants (Sansevieria, Dracaena) are nearly indestructible. They tolerate " +
            "low light but grow faster in bright indirect light. Water only when the soil is " +
            "fully dry — every 10 days or so — and far less in cooler months. Overwatering and " +
            "soggy soil are the only real ways to kill them.",
    )

    private val SUCCULENT = CareProfile(
        waterDays = 12,
        sunlight = "Bright light to full sun",
        soil = "Gritty cactus/succulent mix",
        feeding = "Diluted feed 2-3 times a year",
        about = "Succulents store water in their leaves, so water deeply but rarely — only once " +
            "the soil is bone dry, roughly every 12 days. Give them plenty of bright light, pot " +
            "them in a gritty fast-draining mix, and keep them dry in cooler months. Soft, " +
            "translucent leaves are a sign of overwatering.",
    )

    private val CACTUS = CareProfile(
        waterDays = 14,
        sunlight = "Full sun",
        soil = "Gritty, mineral cactus mix",
        feeding = "Low-nitrogen cactus feed a few times a year",
        about = "Cacti want maximum sun and an extremely well-draining mineral soil. Water " +
            "thoroughly only when the soil is completely dry — about every two weeks in warm " +
            "weather and far less when it is cool. Their biggest threat is root rot from staying " +
            "wet, so err on the side of neglect.",
    )

    private val FERN = CareProfile(
        waterDays = 2,
        sunlight = "Shade to bright indirect light",
        soil = "Rich, moisture-retentive mix",
        feeding = "Diluted balanced feed monthly",
        about = "Ferns want consistently moist soil and high humidity — never let them dry out " +
            "fully. Keep them out of direct sun, which scorches the fronds, and mist them or " +
            "stand the pot on a humidity tray. Feed monthly with a diluted balanced fertiliser " +
            "during the growing season.",
    )

    private val BAMBOO = CareProfile(
        waterDays = 3,
        sunlight = "Bright indirect to full sun",
        soil = "Moist, well-draining soil",
        feeding = "Balanced feed monthly in growing season",
        about = "Ornamental bamboo likes consistently moist soil and bright light. Water when " +
            "the top of the soil starts to dry and never let it fully parch. Contain the roots " +
            "in a pot or barrier — bamboo spreads aggressively — and feed monthly while it is " +
            "actively growing.",
    )

    private val GERANIUM = CareProfile(
        waterDays = 4,
        sunlight = "Full sun to bright light",
        soil = "Well-draining potting mix",
        feeding = "Bloom fertiliser every 2-3 weeks",
        about = "Geraniums (Pelargonium) flower best in full sun. Water when the top inch of " +
            "soil dries and avoid wetting the leaves. Deadhead spent flowers to keep new blooms " +
            "coming and feed every 2-3 weeks with a bloom fertiliser through the growing season.",
    )

    private val MARIGOLD = CareProfile(
        waterDays = 3,
        sunlight = "Full sun",
        soil = "Average, well-draining soil",
        feeding = "Light feed monthly",
        about = "Marigolds are easy, cheerful annuals that want full sun. Water at the base when " +
            "the topsoil dries and keep water off the flowers to prevent rot. Deadhead regularly " +
            "for continuous blooms; they do not need rich soil or heavy feeding.",
    )

    private val PETUNIA = CareProfile(
        waterDays = 3,
        sunlight = "Full sun",
        soil = "Rich, well-draining soil",
        feeding = "Bloom fertiliser every 2 weeks",
        about = "Petunias bloom prolifically in full sun. Keep the soil lightly moist — water " +
            "when the top inch dries — and feed every 2 weeks with a bloom fertiliser. Pinch back " +
            "leggy stems and remove spent flowers to keep the plant full and flowering.",
    )

    private val LILY = CareProfile(
        waterDays = 4,
        sunlight = "Full sun to partial shade",
        soil = "Rich, well-draining soil",
        feeding = "Balanced feed monthly in growing season",
        about = "Lilies like their heads in the sun and their roots cool and shaded. Keep the " +
            "soil evenly moist but never waterlogged, watering when the top inch dries. Feed " +
            "monthly during growth and leave the foliage to die back naturally after flowering " +
            "so the bulb can recharge.",
    )

    private val TULIP = CareProfile(
        waterDays = 5,
        sunlight = "Full sun",
        soil = "Well-draining soil",
        feeding = "Bulb fertiliser at planting and emergence",
        about = "Tulips grow from bulbs and prefer full sun and well-draining soil. Water " +
            "moderately while they are in active growth — soggy soil rots the bulbs. After the " +
            "flowers fade, let the leaves yellow and die back fully before removing them so the " +
            "bulb stores energy for next season.",
    )

    private val WATER_LILY = CareProfile(
        waterDays = 1,
        sunlight = "Full sun",
        soil = "Heavy aquatic loam in a submerged pot",
        feeding = "Aquatic plant tablets monthly",
        about = "Water lilies are aquatic — they grow in a submerged pot of heavy loam with the " +
            "crown a hand-span below the water surface. They need full sun to flower well. Top up " +
            "the pond to keep the water level stable and push aquatic feed tablets into the soil " +
            "monthly during the growing season.",
    )

    private val PEACE_LILY = CareProfile(
        waterDays = 4,
        sunlight = "Low to bright indirect light",
        soil = "Rich, well-draining potting mix",
        feeding = "Balanced feed every 6-8 weeks",
        about = "Peace lilies tolerate low light but flower best in bright indirect light. Keep " +
            "the soil lightly moist — they famously droop when thirsty and recover quickly once " +
            "watered. Avoid direct sun, wipe the leaves to keep them glossy, and feed lightly " +
            "every 6-8 weeks.",
    )

    private val PRAYER_PLANT = CareProfile(
        waterDays = 3,
        sunlight = "Bright indirect light",
        soil = "Rich, moisture-retentive mix",
        feeding = "Diluted balanced feed monthly",
        about = "Prayer plants (Calathea, Maranta) want bright indirect light, consistently " +
            "moist soil, and high humidity. Use room-temperature, low-mineral water — they are " +
            "sensitive to tap-water salts that brown the leaf edges. Keep them out of direct sun " +
            "and feed monthly with a diluted fertiliser.",
    )

    private val ANTHURIUM = CareProfile(
        waterDays = 4,
        sunlight = "Bright indirect light",
        soil = "Chunky, airy, well-draining mix",
        feeding = "Phosphorus-rich feed every 6-8 weeks",
        about = "Anthuriums flower almost year-round in bright indirect light. Water when the " +
            "top 2-3 cm of soil dries and pot them in a chunky, airy mix so the roots breathe. " +
            "They love humidity; feed every 6-8 weeks with a phosphorus-rich fertiliser to keep " +
            "the colourful spathes coming.",
    )

    private val BEGONIA = CareProfile(
        waterDays = 3,
        sunlight = "Bright indirect light",
        soil = "Light, well-draining mix",
        feeding = "Balanced feed every 2-3 weeks while flowering",
        about = "Begonias like bright indirect light and lightly moist soil. Water at the base " +
            "when the top inch dries — wet leaves invite powdery mildew. Give them good airflow, " +
            "feed every 2-3 weeks while flowering, and protect them from harsh direct sun.",
    )

    private val FUCHSIA = CareProfile(
        waterDays = 2,
        sunlight = "Partial shade",
        soil = "Rich, moisture-retentive mix",
        feeding = "Bloom fertiliser every 1-2 weeks",
        about = "Fuchsias want cool partial shade and consistently moist soil — they wilt fast " +
            "in heat and dry soil. Water when the surface begins to dry, feed every 1-2 weeks " +
            "with a bloom fertiliser, and pinch the tips early on for a fuller, more flower-laden " +
            "plant.",
    )

    private val GERBERA = CareProfile(
        waterDays = 3,
        sunlight = "Full sun to bright light",
        soil = "Well-draining soil",
        feeding = "Balanced feed every 2 weeks while flowering",
        about = "Gerbera daisies want bright light and well-draining soil. Water at the base in " +
            "the morning when the top inch dries, keeping the crown dry to prevent rot. Remove " +
            "spent flowers and feed every 2 weeks during the flowering season.",
    )

    private val CHRYSANTHEMUM = CareProfile(
        waterDays = 3,
        sunlight = "Full sun",
        soil = "Rich, well-draining soil",
        feeding = "Balanced feed every 2 weeks until buds show colour",
        about = "Chrysanthemums want full sun and evenly moist, well-draining soil. Water at the " +
            "base when the topsoil dries. Pinch the growing tips through early growth for a " +
            "bushier plant with more blooms, and feed every 2 weeks until the buds begin to show " +
            "colour.",
    )

    private val DAHLIA = CareProfile(
        waterDays = 3,
        sunlight = "Full sun",
        soil = "Rich, well-draining soil",
        feeding = "Low-nitrogen bloom feed monthly",
        about = "Dahlias grow from tubers and want full sun with rich, well-draining soil. Water " +
            "deeply 2-3 times a week once they are established, and stake the taller varieties. " +
            "Feed monthly with a low-nitrogen bloom fertiliser and deadhead often to extend the " +
            "flowering season.",
    )

    private val ZINNIA = CareProfile(
        waterDays = 3,
        sunlight = "Full sun",
        soil = "Average, well-draining soil",
        feeding = "Light feed monthly",
        about = "Zinnias are fuss-free sun lovers. Water at the base when the topsoil dries and " +
            "keep the foliage dry to avoid mildew. Deadhead spent blooms to keep them flowering " +
            "and give them good airflow; they do not need rich soil or heavy feeding.",
    )

    private val IMPATIENS = CareProfile(
        waterDays = 2,
        sunlight = "Partial to full shade",
        soil = "Rich, moisture-retentive mix",
        feeding = "Balanced feed every 2 weeks",
        about = "Impatiens flower best in shade and want consistently moist soil — they wilt " +
            "quickly when dry. Water whenever the surface starts to dry, especially in hot " +
            "weather, and feed every 2 weeks with a balanced fertiliser for continuous blooms.",
    )

    private val PANSY = CareProfile(
        waterDays = 3,
        sunlight = "Full sun to partial shade",
        soil = "Rich, well-draining soil",
        feeding = "Balanced feed every 2-3 weeks",
        about = "Pansies and violas are cool-season bloomers that like full sun to light shade. " +
            "Keep the soil lightly moist, watering when the top inch dries, and deadhead spent " +
            "flowers regularly. Feed every 2-3 weeks; they slow down and fade in prolonged heat.",
    )

    private val SUNFLOWER = CareProfile(
        waterDays = 3,
        sunlight = "Full sun",
        soil = "Deep, well-draining soil",
        feeding = "Balanced feed monthly",
        about = "Sunflowers need full sun — at least 6-8 hours — and deep, well-draining soil. " +
            "Water deeply at the base 2-3 times a week, more for tall varieties, and stake the " +
            "larger ones against wind. Feed monthly with a balanced fertiliser through the " +
            "growing season.",
    )

    private val STRAWBERRY = CareProfile(
        waterDays = 2,
        sunlight = "Full sun",
        soil = "Rich, well-draining soil",
        feeding = "Potassium-rich feed every 2 weeks while fruiting",
        about = "Strawberries want full sun and rich, well-draining soil kept consistently " +
            "moist — water at the base when the surface begins to dry. Mulch to keep the fruit " +
            "off the soil, and feed every 2 weeks with a potassium-rich fertiliser through the " +
            "fruiting season.",
    )

    private val VEGETABLE = CareProfile(
        waterDays = 2,
        sunlight = "Full sun",
        soil = "Rich, well-draining soil",
        feeding = "Balanced feed every 2 weeks; potassium boost when fruiting",
        about = "Fruiting vegetables such as tomato, chilli and pepper want full sun and rich, " +
            "evenly moist soil — water deeply at the base and avoid wetting the leaves. Stake or " +
            "cage them for support, and feed every 2 weeks, shifting to a potassium-rich feed " +
            "once they start to flower and fruit.",
    )

    private val CITRUS = CareProfile(
        waterDays = 5,
        sunlight = "Full sun",
        soil = "Slightly acidic, well-draining soil",
        feeding = "Citrus-specific fertiliser every 6-8 weeks",
        about = "Citrus trees want full sun and slightly acidic, well-draining soil. Water " +
            "deeply when the top few centimetres dry and avoid leaving the roots waterlogged. " +
            "Feed every 6-8 weeks with a citrus fertiliser containing trace elements; yellowing " +
            "leaves usually point to a nutrient deficiency.",
    )

    private val MANGO = CareProfile(
        waterDays = 7,
        sunlight = "Full sun",
        soil = "Deep, well-draining soil",
        feeding = "Balanced feed before flowering season",
        about = "Mango trees need full sun and deep, well-draining soil. Young trees want " +
            "regular deep watering; established ones are drought-tolerant and a dry spell before " +
            "flowering actually encourages fruit. Reduce watering as flowering nears and feed " +
            "with a balanced fertiliser beforehand.",
    )

    private val BANANA = CareProfile(
        waterDays = 3,
        sunlight = "Full sun",
        soil = "Rich, moisture-retentive soil",
        feeding = "High-potassium feed monthly",
        about = "Banana plants are hungry and thirsty. Give them full sun, rich soil, and " +
            "consistently moist (never waterlogged) roots — water when the topsoil begins to " +
            "dry. Feed monthly with a high-potassium fertiliser and shelter them from strong " +
            "wind, which shreds the large leaves.",
    )

    private val PINEAPPLE = CareProfile(
        waterDays = 7,
        sunlight = "Full sun",
        soil = "Sandy, fast-draining soil",
        feeding = "Balanced feed every 6-8 weeks",
        about = "Pineapple is a bromeliad — it stores water and tolerates drought. Give it full " +
            "sun and sandy, fast-draining soil, and water only when the soil is dry, roughly " +
            "weekly. Avoid waterlogging the central rosette and feed lightly every 6-8 weeks.",
    )

    private val GUAVA = CareProfile(
        waterDays = 5,
        sunlight = "Full sun",
        soil = "Well-draining soil",
        feeding = "Balanced feed every 6-8 weeks",
        about = "Guava trees want full sun and well-draining soil. Water deeply but allow the " +
            "topsoil to dry between waterings — they tolerate short dry spells well. Prune to " +
            "keep the canopy open and a manageable height, and feed every 6-8 weeks during the " +
            "growing season.",
    )

    private val PAPAYA = CareProfile(
        waterDays = 4,
        sunlight = "Full sun",
        soil = "Rich, very well-draining soil",
        feeding = "Balanced feed monthly",
        about = "Papaya grows fast in full sun and rich soil but is extremely sensitive to " +
            "waterlogging — soggy roots cause rot. Water when the topsoil dries and make sure " +
            "drainage is excellent. Feed monthly; well-fed plants fruit within the first year.",
    )

    private val PALM = CareProfile(
        waterDays = 5,
        sunlight = "Bright indirect to full sun",
        soil = "Well-draining sandy mix",
        feeding = "Palm fertiliser with micronutrients every 2-3 months",
        about = "Palms want bright light and a well-draining sandy mix. Water when the top few " +
            "centimetres of soil dry and avoid leaving the roots soggy. Feed every 2-3 months " +
            "with a palm fertiliser containing magnesium and potassium; a lack of these shows as " +
            "yellowing older fronds.",
    )

    private val POTHOS = CareProfile(
        waterDays = 5,
        sunlight = "Low to bright indirect light",
        soil = "Standard well-draining potting mix",
        feeding = "Balanced feed every 6-8 weeks",
        about = "Pothos (money plant) is one of the most forgiving houseplants. It grows in " +
            "anything from low light to bright indirect light. Water when the top 2-3 cm of soil " +
            "dries — drooping leaves mean it is thirsty. Trim long vines to keep it full and feed " +
            "every 6-8 weeks.",
    )

    /** Generic flowering-ornamental fallback when nothing else matches. */
    private val GENERIC = CareProfile(
        waterDays = 4,
        sunlight = "Bright indirect to full sun",
        soil = "Well-draining potting mix",
        feeding = "Balanced fertiliser monthly in growing season",
        about = "For most flowering ornamentals, give bright indirect to full sun and water " +
            "when the top 2-3 cm of soil feels dry — overwatering is the most common cause of " +
            "plant loss. Use a well-draining soil, feed monthly during the growing season, and " +
            "prune dead growth to encourage fresh blooms.",
    )
}
