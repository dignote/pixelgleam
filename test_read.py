import os
import json
import zlib
import struct
from collections import defaultdict, deque

def read_png_fast(path):
    with open(path, 'rb') as f:
        data = f.read()
    
    idx = 8
    width = height = bit_depth = color_type = None
    idat_chunks = []
    while idx < len(data):
        length = struct.unpack('>I', data[idx:idx+4])[0]
        chunk_type = data[idx+4:idx+8]
        chunk_data = data[idx+8:idx+8+length]
        idx += 12 + length
        
        if chunk_type == b'IHDR':
            width, height, bit_depth, color_type = struct.unpack('>IIBB', chunk_data[:10])
        elif chunk_type == b'IDAT':
            idat_chunks.append(chunk_data)
        elif chunk_type == b'IEND':
            break
            
    decompressed = zlib.decompress(b''.join(idat_chunks))
    
    bpp = 2 if (color_type == 0 and bit_depth == 16) else (1 if color_type == 0 else 4)
    stride = width * bpp
    
    # fast scanline unfiltering
    raw = bytearray(width * height * bpp)
    in_ptr = 0
    out_ptr = 0
    
    for y in range(height):
        ft = decompressed[in_ptr]
        in_ptr += 1
        scanline = bytearray(decompressed[in_ptr:in_ptr+stride])
        in_ptr += stride
        
        if ft == 0:
            pass
        elif ft == 1:
            for i in range(bpp, stride):
                scanline[i] = (scanline[i] + scanline[i - bpp]) & 0xFF
        elif ft == 2:
            if y > 0:
                prev = raw[out_ptr - stride : out_ptr]
                for i in range(stride):
                    scanline[i] = (scanline[i] + prev[i]) & 0xFF
        elif ft == 3:
            prev = raw[out_ptr - stride : out_ptr] if y > 0 else bytearray(stride)
            for i in range(stride):
                left = scanline[i - bpp] if i >= bpp else 0
                scanline[i] = (scanline[i] + ((left + prev[i]) >> 1)) & 0xFF
        elif ft == 4:
            prev = raw[out_ptr - stride : out_ptr] if y > 0 else bytearray(stride)
            for i in range(stride):
                left = scanline[i - bpp] if i >= bpp else 0
                up = prev[i]
                left_up = prev[i - bpp] if (i >= bpp and y > 0) else 0
                p = left + up - left_up
                pa = abs(p - left)
                pb = abs(p - up)
                pc = abs(p - left_up)
                pr = left if (pa <= pb and pa <= pc) else (up if pb <= pc else left_up)
                scanline[i] = (scanline[i] + pr) & 0xFF
                
        raw[out_ptr : out_ptr + stride] = scanline
        out_ptr += stride
        
    if color_type == 0 and bit_depth == 16:
        pixels = [0] * (width * height)
        for i in range(width * height):
            pixels[i] = (raw[i*2] << 8) | raw[i*2+1]
        return width, height, pixels
    elif color_type == 0 and bit_depth == 8:
        return width, height, list(raw)
    elif color_type == 6: # RGBA
        return width, height, raw
    return width, height, raw

art_path = "app/src/main/assets/artworks/A1_blossom_bicycle"
print("Reading A1...")
w, h, cmap = read_png_fast(os.path.join(art_path, "cell_id_map.png"))
print(f"Read {w}x{h}, {len(cmap)} pixels, max cid: {max(cmap)}")
