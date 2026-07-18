#!/usr/bin/env python3
"""Validate Earth on Minecraft resource files without requiring Minecraft to boot."""

from __future__ import annotations

import json
import hashlib
import pathlib
import re
import struct
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / "neoforge-26.2" / "src" / "main" / "resources"
ASSETS = RES / "assets" / "earth_on_minecraft"
JAVA_ENTRYPOINT = ROOT / "neoforge-26.2" / "src" / "main" / "java" / "com" / "xxsx" / "earthonminecraft" / "EarthOnMinecraft.java"
CURRENT_JAR = ROOT / "neoforge-26.2" / "build" / "libs" / "earth-on-minecraft-neoforge-26.2-0.1.9.jar"
CURRENT_RELEASE_NOTES = ROOT / "docs" / "release-notes-0.1.9-beta.md"
VANILLA_ORE_PLACED_FEATURES = (
    "ore_coal_lower",
    "ore_coal_upper",
    "ore_copper",
    "ore_copper_large",
    "ore_diamond",
    "ore_diamond_buried",
    "ore_diamond_large",
    "ore_diamond_medium",
    "ore_emerald",
    "ore_gold",
    "ore_gold_extra",
    "ore_gold_lower",
    "ore_iron_middle",
    "ore_iron_small",
    "ore_iron_upper",
    "ore_lapis",
    "ore_lapis_buried",
    "ore_redstone",
    "ore_redstone_lower",
)

FORBIDDEN_PLAYER_TEXT = (
    ("placeholder", "use player-facing wording instead of development placeholders"),
    ("占位", "use player-facing wording instead of development placeholders"),
    ("push item drops", "conveyors use controlled cargo, not dropped-item pushing"),
    ("entities forward", "conveyors should not claim they push entities"),
)

INTEGRATION_TAG_REQUIREMENTS = {
    "data/earth_on_minecraft/tags/item/water/potable.json": {
        "earth_on_minecraft:softened_water",
    },
    "data/earth_on_minecraft/tags/item/water/treated.json": {
        "earth_on_minecraft:softened_water",
    },
    "data/earth_on_minecraft/tags/item/water/treatment_inputs.json": {
        "earth_on_minecraft:hard_water_sample",
        "earth_on_minecraft:activated_carbon_filter",
    },
    "data/earth_on_minecraft/tags/item/soil/amendments.json": {
        "earth_on_minecraft:humus_sample",
        "earth_on_minecraft:fertilizer_blend",
    },
    "data/earth_on_minecraft/tags/item/construction/materials.json": {
        "earth_on_minecraft:cement_powder",
        "earth_on_minecraft:asphalt",
    },
    "data/earth_on_minecraft/tags/item/electrical/conductors.json": {
        "earth_on_minecraft:copper_wire",
        "earth_on_minecraft:copper_busbar",
    },
    "data/earth_on_minecraft/tags/item/electrical/insulators.json": {
        "earth_on_minecraft:ceramic_insulator",
        "earth_on_minecraft:rubber_gasket",
    },
    "data/earth_on_minecraft/tags/item/filtration/media.json": {
        "earth_on_minecraft:activated_carbon",
        "earth_on_minecraft:activated_carbon_filter",
    },
    "data/earth_on_minecraft/tags/item/power/sources.json": {
        "earth_on_minecraft:nuclear_heat_module",
    },
    "data/earth_on_minecraft/tags/item/power/connectors.json": {
        "earth_on_minecraft:copper_wire",
    },
    "data/earth_on_minecraft/tags/block/power/generators.json": {
        "earth_on_minecraft:combustion_generator",
        "earth_on_minecraft:steam_turbine_generator",
    },
    "data/earth_on_minecraft/tags/block/power/connectors.json": {
        "earth_on_minecraft:copper_power_cable",
    },
    "data/earth_on_minecraft/tags/block/environment/heat_sources.json": {
        "earth_on_minecraft:industrial_kiln",
    },
    "data/earth_on_minecraft/tags/block/environment/air_pollution_sources.json": {
        "earth_on_minecraft:combustion_generator",
    },
    "data/earth_human/tags/item/care/hydration.json": {
        "earth_on_minecraft:softened_water",
    },
}


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


def validate_player_facing_text() -> int:
    roots = [
        ASSETS / "lang",
        ROOT / "neoforge-26.2" / "src" / "main" / "java" / "com" / "xxsx" / "earthonminecraft" / "client",
    ]
    count = 0
    for root in roots:
        for path in root.rglob("*"):
            if path.suffix not in {".json", ".java"}:
                continue
            text = path.read_text(encoding="utf-8-sig")
            for needle, reason in FORBIDDEN_PLAYER_TEXT:
                if needle in text:
                    fail(f"player-facing text contains {needle!r} in {path.relative_to(ROOT)}: {reason}")
            count += 1
    return count


def validate_release_note_sha() -> int:
    if not CURRENT_RELEASE_NOTES.exists():
        return 0
    text = CURRENT_RELEASE_NOTES.read_text(encoding="utf-8-sig")
    match = re.search(r"SHA256: `([A-Fa-f0-9]{64})`", text)
    if match is None:
        fail(f"missing SHA256 in {CURRENT_RELEASE_NOTES.relative_to(ROOT)}")
    if CURRENT_JAR.exists():
        digest = hashlib.sha256(CURRENT_JAR.read_bytes()).hexdigest().upper()
        if match.group(1).upper() != digest:
            fail(f"release-note SHA mismatch for {CURRENT_JAR.relative_to(ROOT)}")
    return 1


def walk_values(value):
    if isinstance(value, dict):
        for nested in value.values():
            yield from walk_values(nested)
    elif isinstance(value, list):
        for nested in value:
            yield from walk_values(nested)
    else:
        yield value


def tag_values(data: dict) -> list[str]:
    result: list[str] = []
    values = data.get("values", [])
    if not isinstance(values, list):
        return result
    for entry in values:
        if isinstance(entry, str):
            result.append(entry)
        elif isinstance(entry, dict) and isinstance(entry.get("id"), str):
            result.append(entry["id"])
    return result


def validate_local_reference(value: str, path: pathlib.Path, known_ids: set[str]) -> None:
    is_tag = value.startswith("#")
    reference = value[1:] if is_tag else value
    if not reference.startswith("earth_on_minecraft:"):
        return
    local_id = reference.split(":", 1)[1]
    parts = path.relative_to(RES / "data").parts
    registry_type = parts[2] if len(parts) >= 4 and parts[1] == "tags" else None
    if not is_tag:
        if registry_type in (None, "item", "block") and local_id not in known_ids:
            fail(f"unknown item/block id {reference} referenced by {path.relative_to(ROOT)}")
        if registry_type not in (None, "item", "block"):
            target = RES / "data" / "earth_on_minecraft" / registry_type / f"{local_id}.json"
            if not target.exists():
                fail(f"unknown {registry_type} id {reference} referenced by {path.relative_to(ROOT)}")
        return

    if registry_type is None:
        return
    target = RES / "data" / "earth_on_minecraft" / "tags" / registry_type / f"{local_id}.json"
    if not target.exists():
        fail(f"unknown local tag {reference} referenced by {path.relative_to(ROOT)}")


def validate_recipe_and_tag_refs(known_ids: set[str]) -> int:
    count = 0
    roots = [RES / "data" / "earth_on_minecraft" / "recipe"]
    roots.extend(path / "tags" for path in (RES / "data").iterdir() if (path / "tags").is_dir())
    for root in roots:
        for path in root.rglob("*.json"):
            data = json.loads(path.read_text(encoding="utf-8-sig"))
            for value in walk_values(data):
                if not isinstance(value, str):
                    continue
                validate_local_reference(value, path, known_ids)
            count += 1
    return count


def validate_living_world(known_ids: set[str]) -> tuple[int, int, int]:
    langs = {
        lang: json.loads((ASSETS / "lang" / f"{lang}.json").read_text(encoding="utf-8-sig"))
        for lang in ("en_us", "zh_cn")
    }

    def require_lang(key: str, source: pathlib.Path) -> None:
        for lang, values in langs.items():
            if key not in values:
                fail(f"missing {lang} living-world lang key {key} referenced by {source.relative_to(ROOT)}")

    role_dir = RES / "data" / "earth_on_minecraft" / "settlements" / "resident_roles"
    trade_root = RES / "data" / "earth_on_minecraft" / "villager_trade" / "resident"
    tag_root = RES / "data" / "minecraft" / "tags" / "villager_trade"
    expected_trades: set[str] = set()
    expected_tags: dict[tuple[str, int], set[str]] = {}

    for role_path in sorted(role_dir.glob("*.json")):
        role = json.loads(role_path.read_text(encoding="utf-8-sig"))
        role_name = role_path.stem
        role_id = f"earth_on_minecraft:{role_name}"
        require_lang(role["title_key"], role_path)
        require_lang(role["identity_key"], role_path)
        professions = [
            value.split(":", 1)[1]
            for value in role.get("vanilla_professions", [])
            if value.startswith("minecraft:") and value != "minecraft:none"
        ]
        for index, definition in enumerate(role.get("trades", []), start=1):
            level = max(1, min(5, int(definition["level"])))
            local_path = f"resident/{role_name}/level_{level}_{index}"
            trade_id = f"earth_on_minecraft:{local_path}"
            expected_trades.add(local_path)
            trade_path = RES / "data" / "earth_on_minecraft" / "villager_trade" / f"{local_path}.json"
            if not trade_path.exists():
                fail(f"missing generated living-world trade {trade_path.relative_to(ROOT)}")
            trade = json.loads(trade_path.read_text(encoding="utf-8-sig"))
            predicate = trade.get("merchant_predicate", {}).get("predicate", {}).get("minecraft:nbt", "")
            if role_id not in predicate:
                fail(f"trade predicate does not select {role_id}: {trade_path.relative_to(ROOT)}")
            for field in ("input", "output"):
                item_id = definition[field]
                if item_id.startswith("earth_on_minecraft:") and item_id.split(":", 1)[1] not in known_ids:
                    fail(f"unknown living-world trade item {item_id} in {role_path.relative_to(ROOT)}")
            for profession in professions:
                expected_tags.setdefault((profession, level), set()).add(trade_id)

    actual_trades = {
        path.relative_to(trade_root.parent).with_suffix("").as_posix()
        for path in trade_root.rglob("*.json")
    }
    if actual_trades != expected_trades:
        missing = sorted(expected_trades - actual_trades)
        stale = sorted(actual_trades - expected_trades)
        fail(f"living-world generated trade drift; missing={missing} stale={stale}")

    for (profession, level), expected_values in sorted(expected_tags.items()):
        tag_path = tag_root / profession / f"level_{level}.json"
        if not tag_path.exists():
            fail(f"missing generated villager trade tag {tag_path.relative_to(ROOT)}")
        values = set(tag_values(json.loads(tag_path.read_text(encoding="utf-8-sig"))))
        if not expected_values.issubset(values):
            fail(f"villager trade tag is missing role trades: {tag_path.relative_to(ROOT)}")

    settlement_root = RES / "data" / "earth_on_minecraft" / "settlements"
    for path in sorted((settlement_root / "name_pools").glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        for key in data.get("name_keys", []):
            require_lang(key, path)
    for path in sorted((settlement_root / "profiles").glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        for field in ("display_name_key", "scale_key", "technology_key"):
            require_lang(data[field], path)
        for field in ("name_keys", "industry_keys", "demand_keys", "supply_keys"):
            for key in data.get(field, []):
                require_lang(key, path)

    required_loot = {
        "villager_work_items": "living/villager_work_items",
        "pillager_stolen_supplies": "living/pillager_stolen_supplies",
    }
    for modifier_name, table_id in required_loot.items():
        modifier_path = RES / "data" / "earth_on_minecraft" / "loot_modifiers" / f"{modifier_name}.json"
        table_path = RES / "data" / "earth_on_minecraft" / "loot_table" / f"{table_id}.json"
        if not modifier_path.exists() or not table_path.exists():
            fail(f"missing Living World loot modifier/table pair {modifier_name}")
        modifier = json.loads(modifier_path.read_text(encoding="utf-8-sig"))
        if modifier.get("table") != f"earth_on_minecraft:{table_id}":
            fail(f"wrong Living World loot table reference in {modifier_path.relative_to(ROOT)}")

    return len(expected_trades), len(expected_tags), len(required_loot)


def validate_integration_contract(known_ids: set[str]) -> int:
    count = 0
    for relative, required_values in INTEGRATION_TAG_REQUIREMENTS.items():
        path = RES / relative
        if not path.exists():
            fail(f"missing integration tag {path.relative_to(ROOT)}")
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        if data.get("replace") is not False:
            fail(f"integration tag must use replace=false: {path.relative_to(ROOT)}")
        values = set(tag_values(data))
        missing = sorted(required_values - values)
        if missing:
            fail(f"integration tag {path.relative_to(ROOT)} is missing {', '.join(missing)}")
        for value in values:
            validate_local_reference(value, path, known_ids)
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


def absolute_height(value: object) -> int | None:
    if isinstance(value, dict) and isinstance(value.get("absolute"), int):
        return value["absolute"]
    return None


def validate_worldgen_height_ranges() -> int:
    count = 0
    root = RES / "data" / "earth_on_minecraft" / "worldgen" / "placed_feature"
    for path in root.glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        for placement in data.get("placement", []):
            if not isinstance(placement, dict) or placement.get("type") != "minecraft:height_range":
                continue
            height = placement.get("height")
            if not isinstance(height, dict):
                fail(f"missing height object in {path.relative_to(ROOT)}")
            min_y = absolute_height(height.get("min_inclusive"))
            max_y = absolute_height(height.get("max_inclusive"))
            if min_y is None or max_y is None:
                fail(f"non-absolute height range in {path.relative_to(ROOT)}")
            if min_y < -64 or max_y > 320 or min_y > max_y:
                fail(f"height out of vanilla 26.2 bounds in {path.relative_to(ROOT)}: {min_y}..{max_y}")
        count += 1
    return count


def validate_earth_strata_refs() -> int:
    configured = {path.stem for path in (RES / "data" / "earth_on_minecraft" / "worldgen" / "configured_feature").glob("*.json")}
    placed = {path.stem for path in (RES / "data" / "earth_on_minecraft" / "worldgen" / "placed_feature").glob("*.json")}
    count = 0
    root = RES / "data" / "earth_on_minecraft" / "earth" / "strata"
    for path in root.glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        for layer in data.get("layers", []) + data.get("planned_layers", []):
            if not isinstance(layer, dict):
                continue
            for field in ("background_features", "deposit_features"):
                for ref in layer.get(field, []):
                    if not isinstance(ref, str) or not ref.startswith("earth_on_minecraft:"):
                        continue
                    local_id = ref.split(":", 1)[1]
                    if local_id not in placed and local_id not in configured:
                        fail(f"unknown strata feature {ref} referenced by {path.relative_to(ROOT)}")
            y_range = layer.get("y_range")
            if isinstance(y_range, list) and len(y_range) == 2:
                min_y, max_y = y_range
                if not isinstance(min_y, int) or not isinstance(max_y, int) or min_y > max_y:
                    fail(f"bad strata y_range in {path.relative_to(ROOT)}: {y_range}")
        count += 1
    return count


def has_count_zero_placement(data: dict) -> bool:
    placement = data.get("placement")
    if not isinstance(placement, list):
        return False
    return any(
        isinstance(entry, dict) and entry.get("type") == "minecraft:count" and entry.get("count") == 0
        for entry in placement
    )


def validate_vanilla_ore_suppression() -> int:
    noise_path = RES / "data" / "minecraft" / "worldgen" / "noise" / "ore_veininess.json"
    if not noise_path.exists():
        fail(f"missing vanilla ore veininess override {noise_path.relative_to(ROOT)}")
    noise = json.loads(noise_path.read_text(encoding="utf-8-sig"))
    if noise.get("amplitudes") != [0.0]:
        fail(f"vanilla ore veininess is not disabled in {noise_path.relative_to(ROOT)}")

    count = 1
    root = RES / "data" / "minecraft" / "worldgen" / "placed_feature"
    for feature in VANILLA_ORE_PLACED_FEATURES:
        path = root / f"{feature}.json"
        if not path.exists():
            fail(f"missing vanilla ore placed_feature override {path.relative_to(ROOT)}")
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        if not has_count_zero_placement(data):
            fail(f"vanilla ore placed_feature is not count=0: {path.relative_to(ROOT)}")
        count += 1
    return count


def main() -> None:
    json_count = validate_json()
    png_count = validate_png_headers()
    model_count = validate_model_texture_refs()
    ref_count = validate_model_refs()
    block_count, item_count, stack_count, known_ids = validate_registry_resource_coverage()
    data_ref_count = validate_recipe_and_tag_refs(known_ids)
    living_trade_count, living_tag_count, living_loot_count = validate_living_world(known_ids)
    integration_tag_count = validate_integration_contract(known_ids)
    worldgen_ref_count = validate_worldgen_refs()
    worldgen_height_count = validate_worldgen_height_ranges()
    strata_count = validate_earth_strata_refs()
    vanilla_ore_suppression_count = validate_vanilla_ore_suppression()
    player_text_count = validate_player_facing_text()
    release_note_count = validate_release_note_sha()
    print(
        f"OK json={json_count} png={png_count} models={model_count} refs={ref_count} "
        f"blocks={block_count} items={item_count} stacks={stack_count} "
        f"dataRefs={data_ref_count} integrationTags={integration_tag_count} worldgenRefs={worldgen_ref_count} "
        f"livingTrades={living_trade_count} livingTradeTags={living_tag_count} livingLoot={living_loot_count} "
        f"worldgenHeights={worldgen_height_count} strata={strata_count} "
        f"vanillaOreSuppression={vanilla_ore_suppression_count} playerText={player_text_count} "
        f"releaseNotes={release_note_count}"
    )


if __name__ == "__main__":
    main()
