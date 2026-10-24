#!/usr/bin/env python3
"""Check combat PNG alpha baselines without Windows-only System.Drawing."""
import argparse
from pathlib import Path
import struct
import zlib


def bottom_padding(path, threshold):
    data = path.read_bytes()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError(f'{path.name}: invalid PNG')
    offset, payload = 8, bytearray()
    width = height = None
    while offset < len(data):
        size = struct.unpack_from('>I', data, offset)[0]
        kind = data[offset + 4:offset + 8]
        chunk = data[offset + 8:offset + 8 + size]
        if kind == b'IHDR':
            width, height, depth, color, compression, filtering, interlace = struct.unpack('>IIBBBBB', chunk)
            if (depth, color, compression, filtering, interlace) != (8, 6, 0, 0, 0):
                raise ValueError(f'{path.name}: expected non-interlaced RGBA8 PNG; convert or extend the validator explicitly')
        elif kind == b'IDAT':
            payload.extend(chunk)
        offset += size + 12
        if kind == b'IEND':
            break
    if not width or not height:
        raise ValueError(f'{path.name}: missing PNG dimensions')
    raw = zlib.decompress(payload)
    stride = width * 4 + 1
    if len(raw) != stride * height:
        raise ValueError(f'{path.name}: unexpected decoded PNG length')
    previous = bytearray(width)
    last = -1
    for y in range(height):
        start = y * stride
        mode = raw[start]
        current = bytearray(width)
        for x in range(width):
            value = raw[start + 4 + x * 4]
            left = current[x - 1] if x else 0
            up = previous[x]
            upper_left = previous[x - 1] if x else 0
            if mode == 1:
                value += left
            elif mode == 2:
                value += up
            elif mode == 3:
                value += (left + up) // 2
            elif mode == 4:
                estimate = left + up - upper_left
                distances = (abs(estimate - left), abs(estimate - up), abs(estimate - upper_left))
                value += (left, up, upper_left)[distances.index(min(distances))]
            elif mode != 0:
                raise ValueError(f'{path.name}: unknown PNG filter {mode}')
            current[x] = value & 255
        if max(current) > threshold:
            last = y
        previous = current
    if last < 0:
        raise ValueError(f'{path.name} contains no visible pixels')
    return (height - 1 - last) / height


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--expected-bottom-padding', type=float, default=0.02)
    parser.add_argument('--tolerance', type=float, default=0.006)
    parser.add_argument('--alpha-threshold', type=int, default=8)
    parser.add_argument('--sprite-dir', type=Path, default=Path(__file__).resolve().parents[1] / 'world_assets/src/main/assets/images/enemies')
    args = parser.parse_args()
    files = sorted(args.sprite_dir.glob('*_combat.png'))
    if not files:
        raise SystemExit('No combat sprites found; baseline validation is incomplete.')
    failures = []
    for path in files:
        try:
            padding = bottom_padding(path, args.alpha_threshold)
            if abs(padding - args.expected_bottom_padding) > args.tolerance:
                failures.append(f'{path.name}: bottom alpha padding {padding:.2%}; expected {args.expected_bottom_padding:.2%}')
        except (ValueError, OSError, struct.error, zlib.error) as error:
            failures.append(str(error))
    if failures:
        raise SystemExit('\n'.join(failures))
    print(f'Enemy sprite bounds validation passed: {len(files)} sprites share the ground baseline.')


if __name__ == '__main__':
    main()
