package com.example.flora.ml

/**
 * Verified Wikipedia/Wikimedia image URLs for each seeded disease.
 * Resolved via Wikipedia REST API (`scratch/disease_export/fetch_image_urls.py`).
 *
 * Fetched by Coil with the Flora User-Agent set in [com.example.flora.FloraApplication].
 * Disk-cached on first view so subsequent opens work offline.
 */
object DiseaseImageRefs {

    private val map: Map<String, String> = mapOf(
        // Curated diseases (DB seed)
        "Powdery Mildew" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fb/Golovinomyces_sordidus_on_Broadleaf_Plantain_-_Plantago_major_%2844171864324%29.jpg/330px-Golovinomyces_sordidus_on_Broadleaf_Plantain_-_Plantago_major_%2844171864324%29.jpg",
        "Downy Mildew" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/17/Diplocarpon_mespili_-_Lindsey.jpg/330px-Diplocarpon_mespili_-_Lindsey.jpg",
        "Leaf Spot Disease" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/76/%27Cercospora_capsici.jpg/330px-%27Cercospora_capsici.jpg",
        "Early Blight" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/Alternaria_solani_-_leaf_lesions.jpg/330px-Alternaria_solani_-_leaf_lesions.jpg",
        "Late Blight" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/aa/Late_blight_on_potato_leaf_2.jpg/330px-Late_blight_on_potato_leaf_2.jpg",
        "Rust" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/54/Bruine_roest_op_tarwe_%28Puccinia_recondita_f.sp._tritici_on_Triticum_aestivum%29.jpg/330px-Bruine_roest_op_tarwe_%28Puccinia_recondita_f.sp._tritici_on_Triticum_aestivum%29.jpg",
        "Anthracnose" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Colletotrichum_lindemuthianum.jpg/330px-Colletotrichum_lindemuthianum.jpg",
        "Black Rot" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a4/Black_Knot.jpg/330px-Black_Knot.jpg",
        "Root Rot" to "file:///android_asset/disease_images/root_rot.jpg",
        "Damping Off" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/Pinus_taeda_seedling_damping_off_%28cropped%29.jpg/330px-Pinus_taeda_seedling_damping_off_%28cropped%29.jpg",
        "Fusarium Wilt" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/Ralstonia_solanacearum_symptoms.jpg/330px-Ralstonia_solanacearum_symptoms.jpg",
        "Verticillium Wilt" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6a/Verticillium_dahliae.jpg/330px-Verticillium_dahliae.jpg",
        "Botrytis (Grey Mould)" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e4/Aardbei_Lambada_vruchtrot_Botrytis_cinerea.jpg/330px-Aardbei_Lambada_vruchtrot_Botrytis_cinerea.jpg",
        "Sooty Mould" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/Scale_and_sooty_mold_on_a_Eucalyptus_tree.jpg/330px-Scale_and_sooty_mold_on_a_Eucalyptus_tree.jpg",
        "Bacterial Spot" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Bacterial_wilt.JPG/330px-Bacterial_wilt.JPG",
        "Citrus Greening (HLB)" to "https://upload.wikimedia.org/wikipedia/commons/1/1e/Huanglongbing.jpg",
        "Mosaic Virus" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/33/Cucumber_mosaic_virus_symptoms.jpg/330px-Cucumber_mosaic_virus_symptoms.jpg",
        "Yellow Leaf Curl Virus" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/Yellow_curl_leaf_disease_Pj_IMG_3162.jpg/330px-Yellow_curl_leaf_disease_Pj_IMG_3162.jpg",
        "Aphid Infestation" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Aphids_September_2008-1.jpg/330px-Aphids_September_2008-1.jpg",
        "Whitefly Infestation" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5b/Weisse-Fliege.jpg/330px-Weisse-Fliege.jpg",
        "Spider Mites" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Tetranychus_urticae_%284883560779%29.jpg/330px-Tetranychus_urticae_%284883560779%29.jpg",
        "Mealybugs" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/Mealybugs_on_flower_stem%2C_Yogyakarta%2C_2014-10-31.jpg/330px-Mealybugs_on_flower_stem%2C_Yogyakarta%2C_2014-10-31.jpg",
        "Scale Insects" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Wax_Scale.jpg/330px-Wax_Scale.jpg",
        "Thrips" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2f/Thysanoptera.jpg/330px-Thysanoptera.jpg",
        "Fungus Gnats" to "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/Sciara_hemerobioides_-_Flickr_-_gailhampshire.jpg/330px-Sciara_hemerobioides_-_Flickr_-_gailhampshire.jpg",
        "Caterpillar Damage" to "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Chenille_de_Grand_porte_queue_%28macaon%29.jpg/330px-Chenille_de_Grand_porte_queue_%28macaon%29.jpg",
        "Nutrient Deficiency" to "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d6/Clorosi_fulles_llimonerhu.JPG/330px-Clorosi_fulles_llimonerhu.JPG",
        "Sunburn / Leaf Scorch" to "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7f/Sun_Scald_on_Sitka_Spruce.JPG/330px-Sun_Scald_on_Sitka_Spruce.JPG",
        "Overwatering" to "file:///android_asset/disease_images/overwatering.jpg",
        "Underwatering" to "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2a/Tigridia_pavonia_-_wilted_overnight_-_2018-07-25_focus_stack.jpg/330px-Tigridia_pavonia_-_wilted_overnight_-_2018-07-25_focus_stack.jpg",
        // Disease-model class names (English-only, plant prefix stripped) — map to closest reference.
        "Whitefly / Leaf Miner" to "https://upload.wikimedia.org/wikipedia/commons/thumb/3/33/Cameraria_ohridella_150893811.jpg/330px-Cameraria_ohridella_150893811.jpg",
        "Sigatoka (Leaf Streak)" to "https://upload.wikimedia.org/wikipedia/commons/thumb/4/42/Black_Leaf_Streak.jpg/330px-Black_Leaf_Streak.jpg",
        "Moniliasis (Frosty Pod Rot)" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Mroreri.jpg/330px-Mroreri.jpg",
        "Leaf Rust" to "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bd/Hemileia_vastatrix.jpg/330px-Hemileia_vastatrix.jpg",
        "Fruit Fly Damage" to "https://upload.wikimedia.org/wikipedia/commons/thumb/8/81/Fly_October_2008-4.jpg/330px-Fly_October_2008-4.jpg",
        "Olive Fly Damage" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/68/Fly_December_2007-11.jpg/330px-Fly_December_2007-11.jpg",
        "Nematode Damage" to "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/A_juvenile_root-knot_nematode_%28Meloidogyne_incognita%29_penetrates_a_tomato_root_-_USDA-ARS.jpg/330px-A_juvenile_root-knot_nematode_%28Meloidogyne_incognita%29_penetrates_a_tomato_root_-_USDA-ARS.jpg",
    )

    /** Coil-loadable URL for the disease, or null if no reference is mapped. */
    fun urlFor(diseaseName: String): String? = map[diseaseName]
}
