#!/usr/bin/env python3
"""Validate Earth Online resource files without requiring Minecraft to boot."""

from __future__ import annotations

import json
import pathlib
import struct
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / "neoforge-26.2" / "src" / "main" / "resources"
ASSETS = RES / "assets" / "earth_online"


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    sys.exit(1)


def validate_json() -> int:
    count = 0
    for path in RES.rglob("*.json"):
        try:
            json.loads(path.read_text(encoding="utf-8-sig"))
        except Exception as exc:  # noqa: BLE001 - this is a diagnostic script.
            fail(f"bad json {path.relative_to(ROOT)}: {exc}")
        count += 1
    return count


def validate_png_headers() -> int:
    count = 0
    for path in ASSETS.rglob("*.png"):
        data = path.read_bytes()
        if not data.startswith(b"\x89PNG\r\n\x1a\n"):
            fail(f"not a png {path.relative_to(ROOT)}")
        if len(data) < 24:
            fail(f"truncated png {path.relative_to(ROOT)}")
        width, height = struct.unpack(">II", data[16:24])
        if width <= 0 or height <= 0:
            fail(f"invalid png size {path.relative_to(ROOT)}: {width}x{height}")
        count += 1
    return count


def texture_refs(model_data: dict) -> list[str]:
    refs: list[str] = []
    textures = model_data.get("textures")
    if isinstance(textures, dict):
        refs.extend(value for value in textures.values() if isinstance(value, str))
    model = model_data.get("model")
    if isinstance(model, dict):
        nested = model.get("textures")
        if isinstance(nested, dict):
            refs.extend(value for value in nested.values() if isinstance(value, str))
    return refs


def validate_model_texture_refs() -> int:
    count = 0
    roots = [ASSETS / "models", ASSETS / "items"]
    for root in roots:
        for path in root.rglob("*.json"):
            model = json.loads(path.read_text(encoding="utf-8-sig"))
            count += 1
            for ref in texture_refs(model):
                if ref.startswith("#") or ":" not in ref:
                    continue
                namespace, tex = ref.split(":", 1)
                if namespace != "earth_online":
                    continue
                texture = ASSETS / "textures" / f"{tex}.png"
                if not texture.exists():
                    fail(f"missing texture {ref} referenced by {path.relative_to(ROOT)}")
    return count


def main() -> None:
    json_count = validate_json()
    png_count = validate_png_headers()
    model_count = validate_model_texture_refs()
    print(f"OK json={json_count} png={png_count} models={model_count}")


if __name__ == "__main__":
    main()
