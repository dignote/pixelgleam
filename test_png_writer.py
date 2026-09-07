import zlib
import struct
import binascii

def write_png_16bit_grayscale(path, width, height, pixels):
    # pixels is 1D list of ints (0..65535)
    raw = bytearray()
    for y in range(height):
        raw.append(0) # Filter None
        row_offset = y * width
        for x in range(width):
            val = pixels[row_offset + x]
            raw.append((val >> 8) & 0xFF)
            raw.append(val & 0xFF)
            
    compressed = zlib.compress(raw, level=6)
    
    def make_chunk(chunk_type, data):
        length = struct.pack('>I', len(data))
        crc = struct.pack('>I', binascii.crc32(chunk_type + data) & 0xffffffff)
        return length + chunk_type + data + crc
        
    ihdr_data = struct.pack('>IIBBBBB', width, height, 16, 0, 0, 0, 0)
    ihdr_chunk = make_chunk(b'IHDR', ihdr_data)
    idat_chunk = make_chunk(b'IDAT', compressed)
    iend_chunk = make_chunk(b'IEND', b'')
    
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(ihdr_chunk)
        f.write(idat_chunk)
        f.write(iend_chunk)

print("Testing PNG write...")
test_pixels = [i % 500 for i in range(100 * 100)]
write_png_16bit_grayscale("test_out.png", 100, 100, test_pixels)
print("Saved test_out.png successfully")
