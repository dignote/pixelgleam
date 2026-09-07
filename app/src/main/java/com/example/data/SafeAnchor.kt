package com.example.data

import org.json.JSONObject

/**
 * Safe interior anchor for Cell or Region label placement, hint targeting, and FX.
 * Derived from the actual raster mask using Euclidean distance transform to guarantee
 * that the anchor is strictly inside the target geometry and away from boundaries.
 */
data class SafeAnchor(
    val id: Int,
    val x: Float,
    val y: Float,
    val clearance: Float = 0f
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("x", x.toDouble())
        obj.put("y", y.toDouble())
        obj.put("clearance", clearance.toDouble())
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject, idFallback: Int = 0): SafeAnchor {
            val id = obj.optInt("id", obj.optInt("cellId", obj.optInt("regionId", idFallback)))
            val x = obj.getDouble("x").toFloat()
            val y = obj.getDouble("y").toFloat()
            val clearance = obj.optDouble("clearance", 0.0).toFloat()
            return SafeAnchor(id, x, y, clearance)
        }
    }
}

data class ArtworkAnchors(
    val cellAnchors: Map<Int, SafeAnchor>,
    val regionAnchors: Map<Int, SafeAnchor>
)
