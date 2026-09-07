package com.example.data

import java.io.InputStream
import java.io.DataInputStream
import java.util.zip.InflaterInputStream
import kotlin.math.abs

object Png16Decoder {
    fun decodeGrayscale16(inputStream: InputStream): DecodedImage {
        val dataStream = DataInputStream(inputStream)
        
        // PNG Signature
        val signature = ByteArray(8)
        dataStream.readFully(signature)
        if (!signature.contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))) {
            throw IllegalArgumentException("Not a valid PNG file")
        }
        
        var width = 0
        var height = 0
        var bitDepth = 0
        var colorType = 0
        
        val idatBytes = java.io.ByteArrayOutputStream()
        
        while (true) {
            val length = dataStream.readInt()
            val chunkTypeBytes = ByteArray(4)
            dataStream.readFully(chunkTypeBytes)
            val chunkType = String(chunkTypeBytes, Charsets.US_ASCII)
            
            if (chunkType == "IHDR") {
                width = dataStream.readInt()
                height = dataStream.readInt()
                bitDepth = dataStream.readUnsignedByte()
                colorType = dataStream.readUnsignedByte()
                val compressionMethod = dataStream.readUnsignedByte()
                val filterMethod = dataStream.readUnsignedByte()
                val interlaceMethod = dataStream.readUnsignedByte()

                if (width <= 0 || height <= 0) {
                    throw IllegalArgumentException("Invalid PNG dimensions: ${width}x${height}")
                }
                if (bitDepth != 16) {
                    throw IllegalArgumentException("Unsupported bit depth: $bitDepth. Expected 16-bit grayscale PNG.")
                }
                if (colorType != 0) {
                    throw IllegalArgumentException("Unsupported color type: $colorType. Expected 0 (grayscale).")
                }
                if (compressionMethod != 0) {
                    throw IllegalArgumentException("Unsupported compression method: $compressionMethod. Expected 0 (deflate).")
                }
                if (filterMethod != 0) {
                    throw IllegalArgumentException("Unsupported filter method: $filterMethod. Expected 0 (standard adaptive).")
                }
                if (interlaceMethod != 0) {
                    throw IllegalArgumentException("Unsupported interlace method: $interlaceMethod. Expected 0 (no interlace).")
                }

                // IHDR data length is 13 bytes. We read 4 (width) + 4 (height) + 1 (bitDepth) + 1 (colorType) + 1 (comp) + 1 (filter) + 1 (interlace) = 13 bytes.
                // Remaining IHDR data bytes: length - 13.
                // Plus 4 bytes for IHDR CRC chunk.
                val remainingIhdrBytes = (length - 13) + 4
                dataStream.skipBytes(remainingIhdrBytes)
            } else if (chunkType == "IDAT") {
                val buf = ByteArray(length)
                dataStream.readFully(buf)
                idatBytes.write(buf)
                dataStream.skipBytes(4) // Skip CRC
            } else if (chunkType == "IEND") {
                dataStream.skipBytes(4) // Skip CRC
                break
            } else {
                dataStream.skipBytes(length + 4) // Skip chunk data + CRC
            }
        }
        
        // Decompress IDAT
        val compressedBytes = idatBytes.toByteArray()
        val inflaterStream = InflaterInputStream(compressedBytes.inputStream())
        
        val bytesPerPixel = 2 // 16-bit grayscale = 2 bytes per pixel
        val bytesPerScanline = width * bytesPerPixel
        val decodedData = IntArray(width * height)
        
        val prevScanline = ByteArray(bytesPerScanline)
        val currScanline = ByteArray(bytesPerScanline)
        
        for (y in 0 until height) {
            val filterType = inflaterStream.read()
            if (filterType == -1) {
                throw java.io.EOFException("Unexpected end of IDAT stream")
            }
            
            // Read scanline bytes
            var bytesRead = 0
            while (bytesRead < bytesPerScanline) {
                val count = inflaterStream.read(currScanline, bytesRead, bytesPerScanline - bytesRead)
                if (count == -1) {
                    throw java.io.EOFException("Unexpected end of scanline data")
                }
                bytesRead += count
            }
            
            // Reconstruct scanline based on filter
            when (filterType) {
                0 -> { /* None: nothing to do */ }
                1 -> { // Sub
                    for (i in 0 until bytesPerScanline) {
                        val left = if (i >= bytesPerPixel) currScanline[i - bytesPerPixel].toInt() and 0xFF else 0
                        val raw = (currScanline[i].toInt() and 0xFF) + left
                        currScanline[i] = (raw and 0xFF).toByte()
                    }
                }
                2 -> { // Up
                    for (i in 0 until bytesPerScanline) {
                        val up = prevScanline[i].toInt() and 0xFF
                        val raw = (currScanline[i].toInt() and 0xFF) + up
                        currScanline[i] = (raw and 0xFF).toByte()
                    }
                }
                3 -> { // Average
                    for (i in 0 until bytesPerScanline) {
                        val left = if (i >= bytesPerPixel) currScanline[i - bytesPerPixel].toInt() and 0xFF else 0
                        val up = prevScanline[i].toInt() and 0xFF
                        val raw = (currScanline[i].toInt() and 0xFF) + (left + up) / 2
                        currScanline[i] = (raw and 0xFF).toByte()
                    }
                }
                4 -> { // Paeth
                    for (i in 0 until bytesPerScanline) {
                        val left = if (i >= bytesPerPixel) currScanline[i - bytesPerPixel].toInt() and 0xFF else 0
                        val up = prevScanline[i].toInt() and 0xFF
                        val leftUp = if (i >= bytesPerPixel) prevScanline[i - bytesPerPixel].toInt() and 0xFF else 0
                        
                        val p = left + up - leftUp
                        val pa = abs(p - left)
                        val pb = abs(p - up)
                        val pc = abs(p - leftUp)
                        
                        val predictor = if (pa <= pb && pa <= pc) left else if (pb <= pc) up else leftUp
                        val raw = (currScanline[i].toInt() and 0xFF) + predictor
                        currScanline[i] = (raw and 0xFF).toByte()
                    }
                }
                else -> throw IllegalArgumentException("Unknown filter type: $filterType")
            }
            
            // Convert bytes to Cell IDs
            for (x in 0 until width) {
                val b0 = currScanline[x * 2].toInt() and 0xFF
                val b1 = currScanline[x * 2 + 1].toInt() and 0xFF
                val cellId = (b0 shl 8) or b1
                decodedData[y * width + x] = cellId
            }
            
            // Copy current to previous for next iteration
            System.arraycopy(currScanline, 0, prevScanline, 0, bytesPerScanline)
        }
        
        return DecodedImage(width, height, decodedData)
    }
}

data class DecodedImage(val width: Int, val height: Int, val data: IntArray)
