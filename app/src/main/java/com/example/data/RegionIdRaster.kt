package com.example.data

/**
 * Authoritative Region-ID raster derived deterministically from the Cell-ID raster
 * and artwork cell->region topology mapping.
 *
 * Each pixel holds the authoritative Region-ID (0 for void/background).
 */
class RegionIdRaster(
    val width: Int,
    val height: Int,
    val data: IntArray
) {
    fun getRegionId(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0
        return data[y * width + x]
    }

    companion object {
        fun fromCellIdRaster(
            width: Int,
            height: Int,
            cellIdData: IntArray,
            cellMap: Map<Int, Cell>
        ): RegionIdRaster {
            val size = width * height
            val regionData = IntArray(size)
            for (i in 0 until size) {
                val cellId = cellIdData[i]
                if (cellId > 0) {
                    val cell = cellMap[cellId]
                    if (cell != null) {
                        regionData[i] = cell.regionId
                    }
                }
            }
            return RegionIdRaster(width, height, regionData)
        }
    }
}
