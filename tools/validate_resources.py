#!/usr/bin/env python3
"""Validate Earth on Minecraft resource files without requiring Minecraft to boot."""

from __future__ import annotations

import json
import pathlib
import re
import struct
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / "neoforge-26.2" / "src" / "main" / "resources"
ASSETS = RES / "assets" / "earth_on_minecraft"
JAVA_ENTRYPOINT = ROOT / "neoforge-26.2" / "src" / "main" / "java" / "com" / "xxsx" / "earthonminecraft" / "EarthOnMinecraft.java"


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


def model_path(ref: str) -> pathlib.Path | None:
    if ":" not in ref:
        return None
    namespace, model = ref.split(":", 1)
    if namespace != "earth_on_minecraft":
        return None
    return ASSETS / "models" / f"{model}.json"


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
                if namespace != "earth_on_minecraft":
                    continue
                texture = ASSETS / "textures" / f"{tex}.png"
                if not texture.exists():
                    fail(f"missing texture {ref} referenced by {path.relative_to(ROOT)}")
    return count


def model_refs_from_blockstate(blockstate_data: dict) -> list[str]:
    refs: list[str] = []
    variants = blockstate_data.get("variants")
    if isinstance(variants, dict):
        for value in variants.values():
            entries = value if isinstance(value, list) else [value]
            for entry in entries:
                if isinstance(entry, dict) and isinstance(entry.get("model"), str):
                    refs.append(entry["model"])
    multipart = blockstate_data.get("multipart")
    if isinstance(multipart, list):
        for part in multipart:
            if not isinstance(part, dict):
                continue
            apply = part.get("apply")
            entries = apply if isinstance(apply, list) else [apply]
            for entry in entries:
                if isinstance(entry, dict) and isinstance(entry.get("model"), str):
                    refs.append(entry["model"])
    return refs


def validate_model_refs() -> int:
    count = 0
    for path in (ASSETS / "items").glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        model = data.get("model")
        if isinstance(model, dict) and isinstance(model.get("model"), str):
            target = model_path(model["model"])
            if target is not None and not target.exists():
                fail(f"missing item model {model['model']} referenced by {path.relative_to(ROOT)}")
        count += 1
    for path in (ASSETS / "blockstates").glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        for ref in model_refs_from_blockstate(data):
            target = model_path(ref)
            if target is not None and not target.exists():
                fail(f"missing blockstate model {ref} referenced by {path.relative_to(ROOT)}")
        count += 1
    return count


def registry_ids() -> tuple[list[str], list[str], list[str]]:
    java = JAVA_ENTRYPOINT.read_text(encoding="utf-8")
    block_ids = [
        match.group(1)
        for match in re.finditer(r'DeferredBlock<[^>]+>\s+\w+\s*=\s*\w+\("([a-z0-9_]+)"', java)
    ]
    direct_item_ids = [
        match.group(1)
        for match in re.finditer(r'DeferredItem<[^>]+>\s+\w+\s*=\s*\w+\("([a-z0-9_]+)"', java)
    ]
    # Projection blocks are internal ghost helpers and intentionally have no inventory item.
    block_item_ids = [block_id for block_id in block_ids if block_id != "structure_projection"]
    stack_item_ids = sorted(set(direct_item_ids + block_item_ids))
    return block_ids, direct_item_ids, stack_item_ids


def validate_registry_resource_coverage() -> tuple[int, int, int, set[str]]:
    block_ids, direct_item_ids, stack_item_ids = registry_ids()
    for lang in ("en_us", "zh_cn"):
        data = json.loads((ASSETS / "lang" / f"{lang}.json").read_text(encoding="utf-8-sig"))
        for block_id in block_ids:
            if block_id == "structure_projection":
                continue
            key = f"block.earth_on_minecraft.{block_id}"
            if key not in data:
                fail(f"missing {lang} block lang key {key}")
        for item_id in stack_item_ids:
            key = f"item.earth_on_minecraft.{item_id}"
            if key not in data:
                fail(f"missing {lang} item lang key {key}")
    for item_id in stack_item_ids:
        path = ASSETS / "items" / f"{item_id}.json"
        if not path.exists():
            fail(f"missing 26.2 item definition {path.relative_to(ROOT)}")
    for block_id in block_ids:
        path = ASSETS / "blockstates" / f"{block_id}.json"
        if not path.exists():
            fail(f"missing blockstate {path.relative_to(ROOT)}")
    return len(block_ids), len(direct_item_ids), len(stack_item_ids), set(block_ids) | set(stack_item_ids)


def walk_values(value):
    if isinstance(value, dict):
        for nested in value.values():
            yield from walk_values(nested)
    elif isinstance(value, list):
        for nested in value:
            yield from walk_values(nested)
    else:
        yield value


def validate_recipe_and_tag_refs(known_ids: set[str]) -> int:
    count = 0
    roots = [
        RES / "data" / "earth_on_minecraft" / "recipe",
        RES / "data" / "earth_on_minecraft" / "tags",
        RES / "data" / "minecraft" / "tags",
    ]
    for root in roots:
        for path in root.rglob("*.json"):
            data = json.loads(path.read_text(encoding="utf-8-sig"))
            for value in walk_values(data):
                if not isinstance(value, str) or not value.startswith("earth_on_minecraft:"):
                    continue
                local_id = value.split(":", 1)[1]
                if local_id not in known_ids:
                    fail(f"unknown item/block id {value} referenced by {path.relative_to(ROOT)}")
            count += 1
    return count


def validate_worldgen_refs() -> int:
    configured = {path.stem for path in (RES / "data" / "earth_on_minecraft" / "worldgen" / "configured_feature").glob("*.json")}
    placed = {path.stem for path in (RES / "data" / "earth_on_minecraft" / "worldgen" / "placed_feature").glob("*.json")}
    count = 0
    for path in (RES / "data" / "earth_on_minecraft" / "worldgen" / "placed_feature").glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        feature = data.get("feature")
        if isinstance(feature, str) and feature.startswith("earth_on_minecraft:"):
            local_id = feature.split(":", 1)[1]
            if local_id not in configured:
                fail(f"unknown configured feature {feature} referenced by {path.relative_to(ROOT)}")
        count += 1
    for path in (RES / "data" / "earth_on_minecraft" / "neoforge" / "biome_modifier").glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        features = data.get("features")
        refs = features if isinstance(features, list) else [features]
        for ref in refs:
            if isinstance(ref, str) and ref.startswith("earth_on_minecraft:"):
                local_id = ref.split(":", 1)[1]
                if local_id not in placed:
                    fail(f"unknown placed feature {ref} referenced by {path.relative_to(ROOT)}")
        count += 1
    return count


def main() -> None:
    json_count = validate_json()
    png_count = validate_png_headers()
    model_count = validate_model_texture_refs()
    ref_count = validate_model_refs()
    block_count, item_count, stack_count, known_ids = validate_registry_resource_coverage()
    data_ref_count = validate_recipe_and_tag_refs(known_ids)
    worldgen_ref_count = validate_worldgen_refs()
    print(
        f"OK json={json_count} png={png_count} models={model_count} refs={ref_count} "
        f"blocks={block_count} items={item_count} stacks={stack_count} "
        f"dataRefs={data_ref_count} worldgenRefs={worldgen_ref_count}"
    )


if __name__ == "__main__":
    main()
