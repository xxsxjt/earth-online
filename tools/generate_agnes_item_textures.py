#!/usr/bin/env python3
"""Generate Earth Online item textures through the local Agnes provider config."""

from __future__ import annotations

import argparse
import base64
import json
import pathlib
import time
import urllib.parse

import requests
from PIL import Image


ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / "neoforge-26.2" / "src" / "main" / "resources"
ASSETS = RES / "assets" / "earth_online"
TMP = ROOT / "tmp" / "imagegen" / "agnes-modern-chemistry"
PROVIDERS = pathlib.Path(r"C:\Users\du_ji\WorkBuddy\agnes\providers.json")

MODEL = "agnes-image-2.1-flash"
ENDPOINT = "https://apihub.agnes-ai.com/v1/images/generations"

ITEMS = {
    "crude_oil_sample": ("原油样品", "Crude Oil Sample", "a small dark crude oil vial with black-brown liquid"),
    "natural_gas_cell": ("天然气单元 CH4 等", "Natural Gas Cell CH4 Mix", "a pale blue compressed gas cell with a metal cap"),
    "naphtha": ("石脑油馏分", "Naphtha Fraction", "a light amber petrochemical distillate vial"),
    "kerosene_fraction": ("煤油馏分", "Kerosene Fraction", "a golden kerosene distillate vial"),
    "diesel_fraction": ("柴油馏分", "Diesel Fraction", "a darker amber diesel distillate vial"),
    "lubricating_oil": ("润滑油", "Lubricating Oil", "a thick olive-black lubricating oil canister"),
    "asphalt": ("沥青", "Asphalt", "a matte black asphalt chunk with subtle aggregate speckles"),
    "petroleum_coke": ("石油焦", "Petroleum Coke", "a porous dark petroleum coke lump"),
    "wood_chips": ("木片", "Wood Chips", "several tan brown wood chips"),
    "cellulose_pulp": ("纤维素浆", "Cellulose Pulp", "a beige wet cellulose pulp clump"),
    "bleached_pulp": ("漂白浆", "Bleached Pulp", "a clean off-white bleached paper pulp clump"),
    "cellulose_fiber": ("纤维素纤维", "Cellulose Fiber", "cream colored cellulose fibers bundled like string"),
    "titanium_dioxide": ("二氧化钛 TiO2", "Titanium Dioxide TiO2", "bright white titanium dioxide pigment powder"),
    "iron_oxide_pigment": ("氧化铁颜料 Fe2O3", "Iron Oxide Pigment Fe2O3", "red iron oxide pigment powder"),
    "carbon_black": ("炭黑 C", "Carbon Black C", "deep black carbon black powder"),
    "paint_base": ("涂料基料", "Paint Base", "a small neutral paint base bucket"),
    "styrene": ("苯乙烯 C8H8", "Styrene C8H8", "a clear pale aromatic monomer vial"),
    "polystyrene_resin": ("聚苯乙烯树脂 PS", "Polystyrene Resin PS", "white polystyrene resin pellets"),
    "ethylene_glycol": ("乙二醇 C2H6O2", "Ethylene Glycol C2H6O2", "a clear blue-tinted ethylene glycol vial"),
    "terephthalic_acid": ("对苯二甲酸 C8H6O4", "Terephthalic Acid C8H6O4", "off-white terephthalic acid crystal powder"),
    "pet_resin": ("PET 聚酯树脂", "PET Resin", "translucent pale blue PET resin pellets"),
    "synthetic_rubber": ("合成橡胶", "Synthetic Rubber", "a black synthetic rubber sheet roll"),
    "caprolactam": ("己内酰胺 C6H11NO", "Caprolactam C6H11NO", "pale caprolactam crystals"),
    "nylon_fiber": ("尼龙纤维", "Nylon Fiber", "off-white nylon fibers bundled like thread"),
}


def load_agnes_key() -> str:
    data = json.loads(PROVIDERS.read_text(encoding="utf-8-sig"))
    keys = data["agnes"]["keys"]
    if not isinstance(keys, list) or not keys:
        raise RuntimeError("Agnes provider has no usable key list.")
    return str(keys[0])


def write_json(path: pathlib.Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def update_resource_metadata() -> None:
    items_dir = ASSETS / "items"
    models_dir = ASSETS / "models" / "item"
    for item_id in ITEMS:
        write_json(items_dir / f"{item_id}.json", {
            "model": {
                "type": "minecraft:model",
                "model": f"earth_online:item/{item_id}",
            }
        })
        write_json(models_dir / f"{item_id}.json", {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": f"earth_online:item/{item_id}",
            },
        })

    for lang_name, name_index in [("zh_cn.json", 0), ("en_us.json", 1)]:
        path = ASSETS / "lang" / lang_name
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        for item_id, names in ITEMS.items():
            data[f"item.earth_online.{item_id}"] = names[name_index]
        write_json(path, data)

    tag_path = RES / "data" / "earth_online" / "tags" / "item" / "chemical_products.json"
    tag = json.loads(tag_path.read_text(encoding="utf-8-sig"))
    values = tag.setdefault("values", [])
    for item_id in ITEMS:
        value = f"earth_online:{item_id}"
        if value not in values:
            values.append(value)
    write_json(tag_path, tag)

    manifest_path = RES / "data" / "earth_online" / "earth" / "industry" / "chemical_industry_manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    manifest["scope"] = "modern first playable chemistry coverage"
    industries = manifest.setdefault("industries", [])
    for entry in [
        "petroleum refining",
        "natural gas reforming",
        "naphtha steam cracking",
        "kerosene/diesel fraction upgrading",
        "lubricants/asphalt/petroleum coke",
        "wood pulping and paper",
        "cellulose fiber",
        "titanium dioxide pigment",
        "iron oxide pigment",
        "carbon black",
        "paint base",
        "polystyrene",
        "PET polyester",
        "synthetic rubber",
        "nylon fiber",
    ]:
        if entry not in industries:
            industries.append(entry)
    manifest["note"] = (
        "Java ProcessingMachineBlock owns executable recipes; this manifest documents "
        "implemented industry families for handbook, JEI and future data-driven migration."
    )
    write_json(manifest_path, manifest)


def prompt_for(item_id: str, descriptor: str) -> str:
    return (
        "Create one Minecraft-style pixel art item icon for a mod called Earth Online.\n"
        f"Item id: {item_id}\n"
        f"Subject: {descriptor}.\n"
        "Style: clean 16x16 Minecraft item texture, readable silhouette, hand-painted pixel art, "
        "small industrial chemistry material icon, centered, no text, no letters, no UI, no watermark.\n"
        "Background: transparent if supported; otherwise pure flat #00ff00 chroma key with no shadows or gradients.\n"
        "Constraints: isolated icon only, generous padding, crisp edges, suitable for downscaling to 16x16."
    )


def request_image(key: str, item_id: str, descriptor: str, attempts: int = 5) -> bytes:
    body = {
        "model": MODEL,
        "prompt": prompt_for(item_id, descriptor),
        "size": "1024x1024",
        "n": 1,
    }
    last_error: Exception | None = None
    for attempt in range(1, attempts + 1):
        try:
            response = requests.post(
                ENDPOINT,
                headers={
                    "Authorization": f"Bearer {key}",
                    "Content-Type": "application/json",
                },
                json=body,
                timeout=300,
            )
            response.raise_for_status()
            break
        except requests.RequestException as exc:
            last_error = exc
            if attempt >= attempts:
                raise
            wait = min(90, 8 * attempt)
            print(f"  request failed for {item_id}, retry {attempt}/{attempts} after {wait}s: {exc}", flush=True)
            time.sleep(wait)
    else:
        raise RuntimeError(f"Agnes request failed for {item_id}: {last_error}")
    payload = response.json()
    data = payload.get("data") or []
    if not data:
        raise RuntimeError(f"Agnes returned no image data for {item_id}: {payload}")
    image = data[0]
    if isinstance(image, dict) and image.get("b64_json"):
        return base64.b64decode(image["b64_json"])
    url = image.get("url") or image.get("image_url") if isinstance(image, dict) else None
    if not url:
        raise RuntimeError(f"Unsupported Agnes image payload for {item_id}: {payload}")
    if url.startswith("data:image"):
        _, encoded = url.split(",", 1)
        return base64.b64decode(encoded)
    parsed = urllib.parse.urlparse(url)
    if not parsed.scheme:
        raise RuntimeError(f"Invalid image url for {item_id}: {url}")
    download = requests.get(url, timeout=300)
    download.raise_for_status()
    return download.content


def remove_flat_background(image: Image.Image) -> Image.Image:
    img = image.convert("RGBA")
    width, height = img.size
    pixels = img.load()
    corner_samples = [
        pixels[0, 0],
        pixels[width - 1, 0],
        pixels[0, height - 1],
        pixels[width - 1, height - 1],
    ]
    # Prefer green chroma key if present; otherwise use the average corner color.
    has_green_key = any(g > 150 and g > r * 1.45 and g > b * 1.45 for r, g, b, _a in corner_samples)
    if has_green_key:
        key = (0, 255, 0)
        threshold = 82
    else:
        key = tuple(sum(sample[i] for sample in corner_samples) // 4 for i in range(3))
        threshold = 42
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if g > 130 and g > r * 1.35 and g > b * 1.35:
                pixels[x, y] = (r, g, b, 0)
                continue
            dist = abs(r - key[0]) + abs(g - key[1]) + abs(b - key[2])
            if dist < threshold:
                pixels[x, y] = (r, g, b, 0)
            elif a == 255 and dist < threshold * 2:
                pixels[x, y] = (r, g, b, max(40, int(255 * (dist - threshold) / threshold)))
    return img


def crop_and_downscale(source: pathlib.Path, target: pathlib.Path) -> None:
    img = Image.open(source)
    img = remove_flat_background(img)
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
    side = max(img.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.alpha_composite(img, ((side - img.width) // 2, (side - img.height) // 2))
    padded = Image.new("RGBA", (side + side // 4, side + side // 4), (0, 0, 0, 0))
    padded.alpha_composite(canvas, (side // 8, side // 8))
    icon = padded.resize((16, 16), Image.Resampling.LANCZOS)
    target.parent.mkdir(parents=True, exist_ok=True)
    icon.save(target)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--force", action="store_true", help="regenerate textures even if final PNG exists")
    parser.add_argument("--limit", type=int, default=0, help="only generate first N missing textures")
    args = parser.parse_args()

    update_resource_metadata()
    key = load_agnes_key()
    TMP.mkdir(parents=True, exist_ok=True)
    generated = 0
    for item_id, (_zh, _en, descriptor) in ITEMS.items():
        final = ASSETS / "textures" / "item" / f"{item_id}.png"
        raw = TMP / f"{item_id}.png"
        if final.exists() and not args.force:
            print(f"skip existing {item_id}")
            continue
        if args.limit and generated >= args.limit:
            break
        print(f"generating {item_id} ...", flush=True)
        content = request_image(key, item_id, descriptor)
        raw.write_bytes(content)
        crop_and_downscale(raw, final)
        generated += 1
        time.sleep(1.0)
    print(f"Agnes texture generation complete: generated={generated}")


if __name__ == "__main__":
    main()
