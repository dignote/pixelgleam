package com.example.data

import org.json.JSONObject

data class CanvasSize(val width: Int, val height: Int)

data class PaletteColor(val id: Int, val hex: String, val name: String)

data class Cell(
    val id: Int,
    val centerX: Float,
    val centerY: Float,
    val colorId: Int,
    val regionId: Int,
    val neighbors: List<Int>,
    val safeAnchor: SafeAnchor? = null
) {
    val anchorX: Float get() = safeAnchor?.x ?: centerX
    val anchorY: Float get() = safeAnchor?.y ?: centerY
    val anchorClearance: Float get() = safeAnchor?.clearance ?: 0f
}

data class Region(
    val id: Int,
    val colorId: Int,
    val cellIds: List<Int>,
    val safeAnchor: SafeAnchor? = null
) {
    val anchorX: Float get() = safeAnchor?.x ?: 0f
    val anchorY: Float get() = safeAnchor?.y ?: 0f
    val anchorClearance: Float get() = safeAnchor?.clearance ?: 0f
}

data class Artwork(
    val id: String,
    val title: String,
    val category: String,
    val canvas: CanvasSize,
    val palette: List<PaletteColor>,
    val cells: List<Cell>,
    val regions: List<Region>,
    val cellMap: Map<Int, Cell>,
    val regionMap: Map<Int, Region>,
    val cellAnchors: Map<Int, SafeAnchor> = emptyMap(),
    val regionAnchors: Map<Int, SafeAnchor> = emptyMap()
) {
    fun withAnchors(anchors: ArtworkAnchors): Artwork {
        val updatedCells = cells.map { cell ->
            cell.copy(safeAnchor = anchors.cellAnchors[cell.id])
        }
        val updatedRegions = regions.map { region ->
            region.copy(safeAnchor = anchors.regionAnchors[region.id])
        }
        return copy(
            cells = updatedCells,
            regions = updatedRegions,
            cellMap = updatedCells.associateBy { it.id },
            regionMap = updatedRegions.associateBy { it.id },
            cellAnchors = anchors.cellAnchors,
            regionAnchors = anchors.regionAnchors
        )
    }

    companion object {
        fun parseFromJson(jsonStr: String, safeAnchorsJson: String? = null): Artwork {
            val root = JSONObject(jsonStr)
            val id = root.optString("id", "")
            val title = root.optString("title", id)
            val category = root.optString("categoryLabel", root.optString("category", "default"))
            
            val canvasJson = root.getJSONObject("canvas")
            val canvas = CanvasSize(canvasJson.getInt("width"), canvasJson.getInt("height"))
            
            val paletteList = ArrayList<PaletteColor>()
            val paletteJson = root.getJSONArray("palette")
            for (i in 0 until paletteJson.length()) {
                val item = paletteJson.getJSONObject(i)
                paletteList.add(
                    PaletteColor(
                        id = item.getInt("id"),
                        hex = item.getString("hex"),
                        name = item.optString("name", "Color ${item.getInt("id")}")
                    )
                )
            }
            
            val parsedCellAnchors = HashMap<Int, SafeAnchor>()
            val parsedRegionAnchors = HashMap<Int, SafeAnchor>()

            if (!safeAnchorsJson.isNullOrEmpty()) {
                try {
                    val anchorsRoot = JSONObject(safeAnchorsJson)
                    val cellsObj = anchorsRoot.optJSONObject("cells") ?: anchorsRoot.optJSONObject("cellAnchors")
                    if (cellsObj != null) {
                        for (key in cellsObj.keys()) {
                            val obj = cellsObj.getJSONObject(key)
                            val anchor = SafeAnchor.fromJsonObject(obj, key.toIntOrNull() ?: 0)
                            parsedCellAnchors[anchor.id] = anchor
                        }
                    }
                    val regionsObj = anchorsRoot.optJSONObject("regions") ?: anchorsRoot.optJSONObject("regionAnchors")
                    if (regionsObj != null) {
                        for (key in regionsObj.keys()) {
                            val obj = regionsObj.getJSONObject(key)
                            val anchor = SafeAnchor.fromJsonObject(obj, key.toIntOrNull() ?: 0)
                            parsedRegionAnchors[anchor.id] = anchor
                        }
                    }
                } catch (_: Exception) {}
            }

            val cellList = ArrayList<Cell>()
            val cellsJson = root.getJSONArray("cells")
            for (i in 0 until cellsJson.length()) {
                val item = cellsJson.getJSONObject(i)
                val cid = item.getInt("id")
                val (cx, cy) = if (item.has("center")) {
                    val centerJson = item.getJSONArray("center")
                    Pair(centerJson.getDouble(0).toFloat(), centerJson.getDouble(1).toFloat())
                } else {
                    Pair(item.optDouble("centerX", 0.0).toFloat(), item.optDouble("centerY", 0.0).toFloat())
                }
                val neighborsJson = item.optJSONArray("neighbors")
                val neighborsList = ArrayList<Int>()
                if (neighborsJson != null) {
                    for (n in 0 until neighborsJson.length()) {
                        neighborsList.add(neighborsJson.getInt(n))
                    }
                }
                val anchor = parsedCellAnchors[cid] ?: if (item.has("safeAnchor")) {
                    SafeAnchor.fromJsonObject(item.getJSONObject("safeAnchor"), cid)
                } else null

                cellList.add(
                    Cell(
                        id = cid,
                        centerX = cx,
                        centerY = cy,
                        colorId = item.getInt("colorId"),
                        regionId = item.getInt("regionId"),
                        neighbors = neighborsList,
                        safeAnchor = anchor
                    )
                )
            }
            
            val regionList = ArrayList<Region>()
            val regionsJson = root.getJSONArray("regions")
            for (i in 0 until regionsJson.length()) {
                val item = regionsJson.getJSONObject(i)
                val rid = item.getInt("id")
                val cellIdsJson = item.getJSONArray("cellIds")
                val cellIdsList = ArrayList<Int>()
                for (c in 0 until cellIdsJson.length()) {
                    cellIdsList.add(cellIdsJson.getInt(c))
                }
                val anchor = parsedRegionAnchors[rid] ?: if (item.has("safeAnchor")) {
                    SafeAnchor.fromJsonObject(item.getJSONObject("safeAnchor"), rid)
                } else null

                regionList.add(
                    Region(
                        id = rid,
                        colorId = item.getInt("colorId"),
                        cellIds = cellIdsList,
                        safeAnchor = anchor
                    )
                )
            }
            
            val cellMap = cellList.associateBy { it.id }
            val regionMap = regionList.associateBy { it.id }
            
            return Artwork(
                id = id,
                title = title,
                category = category,
                canvas = canvas,
                palette = paletteList,
                cells = cellList,
                regions = regionList,
                cellMap = cellMap,
                regionMap = regionMap,
                cellAnchors = parsedCellAnchors,
                regionAnchors = parsedRegionAnchors
            )
        }
    }
}
