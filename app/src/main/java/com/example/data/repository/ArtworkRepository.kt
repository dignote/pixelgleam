package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.BuildConfig
import com.example.data.Artwork
import com.example.data.DecodedImage
import com.example.data.Png16Decoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject

object CategoryTaxonomyContract {
    val CANONICAL_CATEGORY_ORDER = listOf(
        "animals_friends",
        "cozy_scenes",
        "food_treats",
        "characters",
        "flowers_nature",
        "ornamental_patterns",
        "geometric_mandalas"
    )

    fun getCategoryOrderIndex(categoryId: String): Int {
        val index = CANONICAL_CATEGORY_ORDER.indexOf(categoryId.lowercase())
        return if (index >= 0) index else Int.MAX_VALUE
    }
}

data class ArtworkCatalogItem(
    val id: String,
    val title: String,
    val category: String,
    val assetFolder: String,
    val totalRegions: Int,
    val totalCells: Int,
    val sortOrder: Int = Int.MAX_VALUE,
    val releaseOrder: Int = Int.MAX_VALUE,
    val categoryId: String = "",
    val lifecycle: String = "active",
    val galleryVisible: Boolean = true
)

interface ArtworkRepository {
    fun getCatalog(): Flow<List<ArtworkCatalogItem>>
    suspend fun getArtwork(artworkId: String): Artwork
    suspend fun loadSettledTexture(artworkId: String): Bitmap
    suspend fun loadCellIdMap(artworkId: String): DecodedImage
    suspend fun loadSubjectMask(artworkId: String): Bitmap?
}

class AssetArtworkRepository(private val context: Context) : ArtworkRepository {

    @Volatile
    private var cachedCatalog: List<ArtworkCatalogItem>? = null

    // For backwards-compatibility with test code inspecting cachedDynamicCatalog
    val cachedDynamicCatalog: List<ArtworkCatalogItem>?
        get() = cachedCatalog

    val staticCatalog = listOf(
        ArtworkCatalogItem(
            id = "happy_corgi_v3",
            title = "Happy Corgi",
            category = "Animals & Companions",
            assetFolder = "artworks/happy_corgi_v3",
            totalRegions = 43,
            totalCells = 347
        ),
        ArtworkCatalogItem(
            id = "gentle_bear_v2",
            title = "Gentle Bear",
            category = "Animals & Companions",
            assetFolder = "artworks/gentle_bear_v2",
            totalRegions = 26,
            totalCells = 372
        ),
        ArtworkCatalogItem(
            id = "A1_blossom_bicycle",
            title = "Blossom Bicycle",
            category = "Objects & Scenes",
            assetFolder = "artworks/A1_blossom_bicycle",
            totalRegions = 135,
            totalCells = 391
        ),
        ArtworkCatalogItem(
            id = "A2_royal_koi_pair",
            title = "Royal Koi Pair",
            category = "Symmetry & Patterns",
            assetFolder = "artworks/A2_royal_koi_pair",
            totalRegions = 213,
            totalCells = 402
        ),
        ArtworkCatalogItem(
            id = "A3_cozy_tea_corner",
            title = "Cozy Tea Corner",
            category = "Food & Still Life",
            assetFolder = "artworks/A3_cozy_tea_corner",
            totalRegions = 160,
            totalCells = 380
        ),
        ArtworkCatalogItem(
            id = "A4_moon_fairy_portrait",
            title = "Moon Fairy Portrait",
            category = "Characters & Avatars",
            assetFolder = "artworks/A4_moon_fairy_portrait",
            totalRegions = 152,
            totalCells = 414
        ),
        ArtworkCatalogItem(
            id = "A5_lucky_shiba_garden",
            title = "Lucky Shiba Garden",
            category = "Animals & Pets",
            assetFolder = "artworks/A5_lucky_shiba_garden",
            totalRegions = 134,
            totalCells = 390
        ),
        ArtworkCatalogItem(
            id = "B2_flower",
            title = "Flower Bouquet",
            category = "Botanical & Nature",
            assetFolder = "artworks/B2_flower",
            totalRegions = 47,
            totalCells = 412
        ),
        ArtworkCatalogItem(
            id = "B3_mandala",
            title = "Mandala",
            category = "Symmetric Geometric",
            assetFolder = "artworks/B3_mandala",
            totalRegions = 93,
            totalCells = 459
        ),
        ArtworkCatalogItem(
            id = "B4_food",
            title = "Cupcake",
            category = "Food & Everyday",
            assetFolder = "artworks/B4_food",
            totalRegions = 32,
            totalCells = 359
        ),
        ArtworkCatalogItem(
            id = "B5_character",
            title = "Character Model",
            category = "Stylized Figures",
            assetFolder = "artworks/B5_character",
            totalRegions = 34,
            totalCells = 398
        )
    )

    private fun openAsset(relativePath: String): java.io.InputStream {
        val devDirs = listOf(
            "app/src/main/assets",
            "src/main/assets",
            "app/build/intermediates/assets/debug/mergeDebugAssets",
            "build/intermediates/assets/debug/mergeDebugAssets"
        )
        for (dir in devDirs) {
            val file = java.io.File(dir, relativePath)
            if (file.exists() && file.isFile) {
                return java.io.FileInputStream(file)
            }
        }
        return context.assets.open(relativePath)
    }

    suspend fun scanDynamicCatalog(): List<ArtworkCatalogItem> = withContext(Dispatchers.IO) {
        cachedDynamicCatalog?.let { return@withContext it }

        val discovered = mutableListOf<ArtworkCatalogItem>()
        val seenIds = mutableSetOf<String>()
        val rawList = try {
            context.assets.list("artworks") ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }

        val assetFoldersFromManager = rawList
            .map { name -> if (name.contains("/")) name.substringBefore("/") else name }
            .filter { name -> !name.contains(".") }

        val devDirs = listOf(
            "app/src/main/assets/artworks",
            "src/main/assets/artworks",
            "app/build/intermediates/assets/debug/mergeDebugAssets/artworks",
            "build/intermediates/assets/debug/mergeDebugAssets/artworks"
        )
        val diskFolders = devDirs
            .map { java.io.File(it) }
            .filter { it.exists() && it.isDirectory }
            .flatMap { it.listFiles()?.filter { f -> f.isDirectory }?.map { f -> f.name } ?: emptyList() }

        val assetFolders = (assetFoldersFromManager + diskFolders).distinct()

        for (folderName in assetFolders) {
            val folderPath = "artworks/$folderName"

            // CATALOG-003: Check artwork.json existence
            val jsonStr = try {
                openAsset("$folderPath/artwork.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                System.err.println("CATALOG-003: Missing or unreadable artwork.json in $folderPath: ${e.message}")
                android.util.Log.w("ArtworkRepository", "CATALOG-003: Missing or unreadable artwork.json in $folderPath")
                continue
            }

            // CATALOG-004: Parse JSON
            val jsonObj = try {
                JSONObject(jsonStr)
            } catch (e: Exception) {
                System.err.println("CATALOG-004: Malformed artwork.json in $folderPath: ${e.message}")
                android.util.Log.w("ArtworkRepository", "CATALOG-004: Malformed artwork.json in $folderPath")
                continue
            }

            val id = jsonObj.optString("id", "")
            if (id.isEmpty()) {
                System.err.println("CATALOG-004: Empty artwork id in $folderPath")
                android.util.Log.w("ArtworkRepository", "CATALOG-004: Empty artwork id in $folderPath")
                continue
            }

            // CATALOG-002: Folder name vs ID match
            if (folderName != id) {
                System.err.println("CATALOG-002: Folder name '$folderName' does not match artwork id '$id'")
                android.util.Log.w("ArtworkRepository", "CATALOG-002: Folder name '$folderName' does not match artwork id '$id'")
                continue
            }

            // CATALOG-001: Duplicate ID check
            if (seenIds.contains(id)) {
                System.err.println("CATALOG-001: Duplicate artwork id '$id' in $folderPath")
                android.util.Log.w("ArtworkRepository", "CATALOG-001: Duplicate artwork id '$id' in $folderPath")
                continue
            }

            // CATALOG-005: Check required assets
            val safeAnchorsJson = try {
                openAsset("$folderPath/safe_anchors.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                System.err.println("CATALOG-005: Missing safe_anchors.json in $folderPath: ${e.message}")
                android.util.Log.w("ArtworkRepository", "CATALOG-005: Missing safe_anchors.json in $folderPath")
                continue
            }

            val cellIdMapDecoded = try {
                openAsset("$folderPath/cell_id_map.png").use { stream ->
                    Png16Decoder.decodeGrayscale16(stream)
                }
            } catch (e: Exception) {
                System.err.println("CATALOG-005: Missing or corrupt cell_id_map.png in $folderPath: ${e.message}")
                android.util.Log.w("ArtworkRepository", "CATALOG-005: Missing or corrupt cell_id_map.png in $folderPath")
                continue
            }

            val hasSettledTexture = try {
                openAsset("$folderPath/settled_texture.png").use { true }
            } catch (e: Exception) {
                false
            }
            if (!hasSettledTexture) {
                android.util.Log.w("ArtworkRepository", "CATALOG-005: Missing settled_texture.png in $folderPath")
                continue
            }

            val hasSubjectMask = try {
                openAsset("$folderPath/subject_mask.png").use { true }
            } catch (e: Exception) {
                false
            }
            if (!hasSubjectMask) {
                android.util.Log.w("ArtworkRepository", "CATALOG-005: Missing subject_mask.png in $folderPath")
                continue
            }

            val subjectMaskBitmap = try {
                openAsset("$folderPath/subject_mask.png").use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                null
            }

            // CATALOG-007: Full ArtworkContentValidator check
            val artwork = try {
                Artwork.parseFromJson(jsonStr, safeAnchorsJson)
            } catch (e: Exception) {
                android.util.Log.w("ArtworkRepository", "CATALOG-007: Failed to parse artwork topology for $id: ${e.message}")
                continue
            }

            val validationResult = com.example.data.validation.ArtworkContentValidator.validate(
                artwork = artwork,
                cellIdData = cellIdMapDecoded.data,
                mapWidth = cellIdMapDecoded.width,
                mapHeight = cellIdMapDecoded.height,
                subjectMask = subjectMaskBitmap
            )

            // Recycle temporary subject mask bitmap to prevent OOM during full catalog scan
            subjectMaskBitmap?.recycle()

            if (validationResult is com.example.data.validation.ArtworkContentValidator.ValidationResult.Fail) {
                System.err.println("CATALOG-007: Validation failed for $id: ${validationResult.violations}")
                android.util.Log.w("ArtworkRepository", "CATALOG-007: Validation failed for $id: ${validationResult.violations}")
                continue
            }

            // Parse category and title
            val title = jsonObj.optString("title", id.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
            val categoryId = jsonObj.optString("categoryId", jsonObj.optString("category", "general"))
            val categoryLabel = if (jsonObj.has("categoryLabel")) {
                jsonObj.getString("categoryLabel")
            } else {
                mapCategoryToLabel(categoryId, jsonObj.optString("category", ""))
            }

            val sortOrder = if (jsonObj.has("sortOrder")) jsonObj.getInt("sortOrder") else Int.MAX_VALUE
            val releaseOrder = if (jsonObj.has("releaseOrder")) jsonObj.getInt("releaseOrder") else Int.MAX_VALUE
            val lifecycle = jsonObj.optString("lifecycle", "active")
            val galleryVisible = jsonObj.optBoolean("galleryVisible", true)

            seenIds.add(id)
            discovered.add(
                ArtworkCatalogItem(
                    id = id,
                    title = title,
                    category = categoryLabel,
                    assetFolder = folderPath,
                    totalRegions = artwork.regions.size,
                    totalCells = artwork.cells.size,
                    sortOrder = sortOrder,
                    releaseOrder = releaseOrder,
                    categoryId = categoryId,
                    lifecycle = lifecycle,
                    galleryVisible = galleryVisible
                )
            )
        }

        // Deterministic sorting: sortOrder -> releaseOrder -> id
        discovered.sortWith(
            compareBy<ArtworkCatalogItem> { it.sortOrder }
                .thenBy { it.releaseOrder }
                .thenBy { it.id }
        )

        cachedCatalog = discovered
        discovered
    }

    suspend fun loadCatalogFromAssets(): List<ArtworkCatalogItem> = withContext(Dispatchers.IO) {
        cachedCatalog?.let { return@withContext it }

        val jsonStr = try {
            openAsset("artworks/catalog.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            if (!BuildConfig.DEBUG) {
                throw IllegalStateException("CRITICAL: artworks/catalog.json missing in release build: ${e.message}", e)
            }
            android.util.Log.w("ArtworkRepository", "artworks/catalog.json missing, falling back to debug dynamic scan: ${e.message}")
            return@withContext scanDynamicCatalog()
        }

        try {
            val root = JSONObject(jsonStr)
            val artworksArray = root.getJSONArray("artworks")
            val items = ArrayList<ArtworkCatalogItem>(artworksArray.length())
            for (i in 0 until artworksArray.length()) {
                val obj = artworksArray.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val categoryId = obj.optString("categoryId", "")
                val categoryLabel = obj.optString("categoryLabel", obj.optString("category", "General"))
                val sortOrder = obj.optInt("sortOrder", Int.MAX_VALUE)
                val releaseOrder = obj.optInt("releaseOrder", Int.MAX_VALUE)
                val totalCells = obj.getInt("totalCells")
                val totalRegions = obj.getInt("totalRegions")
                val assetFolder = obj.optString("assetFolder", "artworks/$id")
                val lifecycle = obj.optString("lifecycle", "active")
                val galleryVisible = obj.optBoolean("galleryVisible", true)

                items.add(
                    ArtworkCatalogItem(
                        id = id,
                        title = title,
                        category = categoryLabel,
                        assetFolder = assetFolder,
                        totalRegions = totalRegions,
                        totalCells = totalCells,
                        sortOrder = sortOrder,
                        releaseOrder = releaseOrder,
                        categoryId = categoryId,
                        lifecycle = lifecycle,
                        galleryVisible = galleryVisible
                    )
                )
            }

            // Trust deterministic sort: sortOrder -> releaseOrder -> id
            items.sortWith(
                compareBy<ArtworkCatalogItem> { it.sortOrder }
                    .thenBy { it.releaseOrder }
                    .thenBy { it.id }
            )

            cachedCatalog = items
            items
        } catch (e: Exception) {
            if (!BuildConfig.DEBUG) {
                throw IllegalStateException("CRITICAL: Malformed artworks/catalog.json in release build: ${e.message}", e)
            }
            android.util.Log.e("ArtworkRepository", "Malformed artworks/catalog.json, falling back to debug dynamic scan", e)
            scanDynamicCatalog()
        }
    }

    private fun mapCategoryToLabel(categoryId: String, rawCategory: String): String {
        val key = (if (categoryId.isNotEmpty()) categoryId else rawCategory).lowercase()
        return when {
            key.contains("animal") || key.contains("pet") -> "Animals & Friends"
            key.contains("cozy") || key.contains("scene") || key.contains("object") -> "Cozy Scenes"
            key.contains("food") || key.contains("treat") -> "Food & Treats"
            key.contains("character") || key.contains("avatar") -> "Characters"
            key.contains("flower") || key.contains("botanical") || key.contains("nature") -> "Flowers & Nature"
            key.contains("ornamental") || key.contains("pattern") || key.contains("symmetry") -> "Ornamental Patterns"
            key.contains("mandala") || key.contains("geometric") -> "Geometric Mandalas"
            key == "internal_benchmark" -> "Internal Benchmark"
            key == "legacy_retired" -> "Legacy Retired"
            else -> "General"
        }
    }

    override fun getCatalog(): Flow<List<ArtworkCatalogItem>> = flow {
        emit(loadCatalogFromAssets())
    }.flowOn(Dispatchers.IO)

    override suspend fun getArtwork(artworkId: String): Artwork = withContext(Dispatchers.IO) {
        val folder = findFolder(artworkId)
        val jsonStr = openAsset("$folder/artwork.json").bufferedReader().use { it.readText() }
        val safeAnchorsJson = try {
            openAsset("$folder/safe_anchors.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
        Artwork.parseFromJson(jsonStr, safeAnchorsJson)
    }

    override suspend fun loadSettledTexture(artworkId: String): Bitmap = withContext(Dispatchers.IO) {
        val folder = findFolder(artworkId)
        openAsset("$folder/settled_texture.png").use { stream ->
            BitmapFactory.decodeStream(stream) ?: throw IllegalStateException("Could not decode settled texture for $artworkId")
        }
    }

    override suspend fun loadCellIdMap(artworkId: String): DecodedImage = withContext(Dispatchers.IO) {
        val folder = findFolder(artworkId)
        openAsset("$folder/cell_id_map.png").use { stream ->
            Png16Decoder.decodeGrayscale16(stream)
        }
    }

    override suspend fun loadSubjectMask(artworkId: String): Bitmap? = withContext(Dispatchers.IO) {
        val folder = findFolder(artworkId)
        try {
            openAsset("$folder/subject_mask.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun findFolder(artworkId: String): String {
        val dynamicFolder = cachedDynamicCatalog?.firstOrNull { it.id == artworkId }?.assetFolder
        if (dynamicFolder != null) return dynamicFolder
        val staticFolder = staticCatalog.firstOrNull { it.id == artworkId }?.assetFolder
        if (staticFolder != null) return staticFolder
        return "artworks/$artworkId"
    }
}
