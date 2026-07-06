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
    "graphite_dust": ("石墨粉 C", "Graphite Dust C", "dark gray flaky graphite powder"),
    "activated_carbon": ("活性炭", "Activated Carbon", "porous black activated carbon granules"),
    "battery_carbon": ("电池级碳粉", "Battery Grade Carbon", "fine conductive black battery carbon powder"),
    "manganese_oxide_dust": ("锰氧化物粉 MnO2", "Manganese Oxide Dust MnO2", "dark brown manganese oxide powder"),
    "nickel_precursor": ("镍前驱体", "Nickel Precursor", "green nickel battery precursor crystals"),
    "lithium_salt": ("锂盐", "Lithium Salt", "pale white lithium salt crystals"),
    "electrolyte": ("电解液", "Electrolyte", "clear blue electrolyte vial for batteries"),
    "electrode_sheet": ("电极片", "Electrode Sheet", "thin layered black and copper battery electrode sheet"),
    "simple_battery_cell": ("简易电池单元", "Simple Battery Cell", "small cylindrical battery cell with metal caps"),
    "hard_water_sample": ("硬水样品", "Hard Water Sample", "cloudy mineral rich water sample vial"),
    "softened_water": ("软化水", "Softened Water", "clear softened water vial with pale blue tint"),
    "sludge_cake": ("污泥饼", "Sludge Cake", "dark brown pressed industrial sludge cake"),
    "neutral_salt": ("中和盐", "Neutral Salt", "mixed white gray neutralization salt crystals"),
    "activated_carbon_filter": ("活性炭滤料", "Activated Carbon Filter", "black activated carbon filter cartridge"),
    "lime_treatment_residue": ("石灰处理渣", "Lime Treatment Residue", "off-white chalky lime treatment residue clump"),
    "stabilized_tailings": ("稳定化尾矿", "Stabilized Tailings", "gray stabilized tailings aggregate blocky lump"),
    "natural_latex": ("天然胶乳", "Natural Latex", "milky white natural latex in a small sample cup"),
    "raw_rubber": ("粗橡胶", "Raw Rubber", "pale tan raw rubber lump"),
    "vulcanized_rubber": ("硫化橡胶", "Vulcanized Rubber", "dark vulcanized rubber slab"),
    "rubber_compound": ("橡胶复合料", "Rubber Compound", "black rubber compound sheet with carbon filler"),
    "rubber_gasket": ("橡胶密封圈", "Rubber Gasket", "small black rubber gasket ring"),
    "ethanol": ("乙醇 C2H5OH", "Ethanol C2H5OH", "clear ethanol vial with faint blue highlight"),
    "acetic_acid": ("乙酸 CH3COOH", "Acetic Acid CH3COOH", "clear acetic acid vial with sharp white shine"),
    "acetone": ("丙酮 C3H6O", "Acetone C3H6O", "clear solvent vial with pale purple tint"),
    "phenol": ("苯酚 C6H5OH", "Phenol C6H5OH", "pale amber phenol crystals in a small jar"),
    "phenolic_resin": ("酚醛树脂", "Phenolic Resin", "dark red brown phenolic resin pellets"),
    "epoxy_resin": ("环氧树脂", "Epoxy Resin", "golden translucent epoxy resin droplet"),
    "industrial_solvent": ("工业溶剂", "Industrial Solvent", "clear industrial solvent canister"),
    "chromite_dust": ("铬铁矿粉 FeCr2O4", "Chromite Dust FeCr2O4", "dark brown black chromite mineral powder"),
    "ferrochrome": ("铬铁 FeCr", "Ferrochrome FeCr", "dark metallic ferrochrome alloy lump"),
    "ferromanganese": ("锰铁 FeMn", "Ferromanganese FeMn", "gray metallic ferromanganese alloy lump"),
    "stainless_steel_bloom": ("不锈钢坯", "Stainless Steel Bloom", "bright silver stainless steel billet"),
    "aluminum_alloy_billet": ("铝合金坯", "Aluminum Alloy Billet", "pale silver aluminum alloy billet"),
    "magnesium_dust": ("镁粉 Mg", "Magnesium Dust Mg", "bright pale magnesium metal powder"),
    "titanium_slag": ("钛渣", "Titanium Slag", "dark blue gray titanium slag chunk"),
    "titanium_tetrachloride": ("四氯化钛 TiCl4", "Titanium Tetrachloride TiCl4", "smoky pale titanium tetrachloride vial"),
    "titanium_sponge": ("海绵钛", "Titanium Sponge", "porous silver gray titanium sponge lump"),
    "metallurgical_silicon": ("冶金级硅", "Metallurgical Silicon", "dark silver metallurgical silicon chunk"),
    "chlorosilane": ("氯硅烷", "Chlorosilane", "clear chlorosilane chemical vial"),
    "high_purity_silicon": ("高纯硅", "High Purity Silicon", "bright reflective high purity silicon shard"),
    "polysilicon": ("多晶硅", "Polysilicon", "blue gray polysilicon crystal chunks"),
    "silicon_wafer": ("硅晶圆", "Silicon Wafer", "thin shiny circular silicon wafer"),
    "dopant_dust": ("掺杂剂粉", "Dopant Dust", "tiny violet red semiconductor dopant powder"),
    "photoresist_precursor": ("光刻胶前驱体", "Photoresist Precursor", "amber photoresist precursor vial"),
    "monazite_sand": ("独居石砂", "Monazite Sand", "orange brown monazite heavy mineral sand"),
    "bastnasite_dust": ("氟碳铈矿粉", "Bastnasite Dust", "tan rare earth bastnasite powder"),
    "mixed_rare_earth_oxide": ("混合稀土氧化物", "Mixed Rare Earth Oxide", "pale cream mixed rare earth oxide powder"),
    "neodymium_salt": ("钕盐", "Neodymium Salt", "pink purple neodymium salt crystals"),
    "ndfeb_magnet": ("钕铁硼磁材", "NdFeB Magnet", "small dark metallic neodymium magnet"),
    "rare_earth_tailings": ("稀土尾渣", "Rare Earth Tailings", "gray tan rare earth tailings residue"),
    "iron_catalyst": ("铁基催化剂", "Iron Catalyst", "dark iron catalyst pellets"),
    "vanadium_catalyst": ("钒基催化剂", "Vanadium Catalyst", "orange vanadium catalyst pellets"),
    "nickel_catalyst": ("镍基催化剂", "Nickel Catalyst", "green nickel catalyst pellets"),
    "platinum_group_catalyst": ("铂族催化剂", "Platinum Group Catalyst", "silver platinum group catalyst grains"),
    "sodium_hypochlorite": ("次氯酸钠 NaClO", "Sodium Hypochlorite NaClO", "pale yellow sodium hypochlorite bleach vial"),
    "bleaching_powder": ("漂白粉", "Bleaching Powder", "off-white bleaching powder granules in a small pile"),
    "hydrogen_peroxide": ("过氧化氢 H2O2", "Hydrogen Peroxide H2O2", "clear hydrogen peroxide vial with bright highlights"),
    "soap_base": ("皂基", "Soap Base", "pale cream soap base flakes"),
    "glycerol": ("甘油 C3H8O3", "Glycerol C3H8O3", "clear viscous glycerol vial"),
    "surfactant": ("表面活性剂", "Surfactant", "blue white surfactant paste in a small sample jar"),
    "detergent_powder": ("洗涤剂粉", "Detergent Powder", "white detergent powder with blue speckles"),
    "butadiene": ("丁二烯 C4H6", "Butadiene C4H6", "pale compressed petrochemical gas cell"),
    "acrylonitrile": ("丙烯腈 C3H3N", "Acrylonitrile C3H3N", "clear acrylonitrile chemical vial"),
    "abs_resin": ("ABS 树脂", "ABS Resin", "ivory ABS plastic pellets"),
    "acrylic_monomer": ("亚克力单体", "Acrylic Monomer", "clear acrylic monomer vial"),
    "acrylic_resin": ("亚克力树脂", "Acrylic Resin", "transparent acrylic resin pellets"),
    "polyol": ("多元醇", "Polyol", "pale syrup polyol sample vial"),
    "isocyanate": ("异氰酸酯", "Isocyanate", "amber isocyanate reagent vial"),
    "polyurethane_foam": ("聚氨酯泡沫", "Polyurethane Foam", "small pale yellow polyurethane foam block"),
    "copper_wire": ("铜线", "Copper Wire", "coiled shiny copper wire"),
    "fiberglass_cloth": ("玻璃纤维布", "Fiberglass Cloth", "woven pale fiberglass cloth square"),
    "copper_clad_laminate": ("覆铜板", "Copper Clad Laminate", "green circuit laminate sheet with copper edge"),
    "printed_circuit_board": ("印刷电路板", "Printed Circuit Board", "small green printed circuit board with copper traces"),
    "solder_alloy": ("焊料合金", "Solder Alloy", "silver solder alloy wire coil"),
    "solder_flux": ("助焊剂", "Solder Flux", "amber solder flux paste jar"),
    "led_phosphor": ("LED 荧光粉", "LED Phosphor", "bright yellow rare earth LED phosphor powder"),
    "ceramic_substrate": ("陶瓷基板", "Ceramic Substrate", "white ceramic electronics substrate plate"),
    "sphalerite_dust": ("闪锌矿粉 ZnS", "Sphalerite Dust ZnS", "brown yellow sphalerite zinc sulfide powder"),
    "zinc_oxide": ("氧化锌 ZnO", "Zinc Oxide ZnO", "fine white zinc oxide powder"),
    "zinc_ingot": ("锌锭", "Zinc Ingot", "blue silver zinc metal ingot"),
    "galena_dust": ("方铅矿粉 PbS", "Galena Dust PbS", "dark lead gray galena powder with metallic grains"),
    "lead_ingot": ("铅锭", "Lead Ingot", "dull heavy blue gray lead ingot"),
    "cassiterite_dust": ("锡石粉 SnO2", "Cassiterite Dust SnO2", "dark brown cassiterite tin ore powder"),
    "tin_ingot": ("锡锭", "Tin Ingot", "bright pale tin metal ingot"),
    "kaolin_dust": ("高岭土粉", "Kaolin Dust", "soft white kaolin clay powder"),
    "refractory_clay": ("耐火黏土", "Refractory Clay", "tan refractory clay lump"),
    "firebrick": ("耐火砖", "Firebrick", "orange tan refractory firebrick"),
    "ceramic_body": ("陶瓷坯料", "Ceramic Body", "gray white ceramic body clay lump"),
    "porcelain_blank": ("瓷坯", "Porcelain Blank", "smooth white porcelain blank piece"),
    "ceramic_insulator": ("陶瓷绝缘件", "Ceramic Insulator", "white ribbed ceramic electrical insulator"),
    "mineral_wool": ("矿物棉", "Mineral Wool", "pale fibrous mineral wool tuft"),
    "ammonium_sulfate": ("硫酸铵", "Ammonium Sulfate", "white ammonium sulfate fertilizer crystals"),
    "single_superphosphate": ("过磷酸钙", "Single Superphosphate", "gray tan single superphosphate fertilizer granules"),
    "urea_formaldehyde_resin": ("脲醛树脂", "Urea Formaldehyde Resin", "cream urea formaldehyde resin granules"),
    "carbon_monoxide_cell": ("一氧化碳单元 CO", "Carbon Monoxide Cell CO", "gray compressed carbon monoxide gas cell"),
    "syngas_cell": ("合成气单元 CO/H2", "Syngas Cell CO/H2", "steel gas cell with blue gray synthesis gas glow"),
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
        "battery materials",
        "graphite anode material",
        "lithium salt electrolyte",
        "nickel manganese battery precursors",
        "simple battery cells",
        "water softening",
        "activated carbon filtration",
        "wastewater neutralization",
        "tailings stabilization",
        "sludge cake sintering",
        "natural latex and rubber vulcanization",
        "rubber compounding and gaskets",
        "ethanol fermentation",
        "acetic acid and acetone solvents",
        "phenolic resin",
        "epoxy resin",
        "stainless steel alloying",
        "aluminum alloying",
        "ferrochrome and ferromanganese",
        "titanium chloride and sponge titanium",
        "metallurgical silicon",
        "chlorosilane purification",
        "polysilicon and silicon wafers",
        "semiconductor dopants",
        "photoresist precursor",
        "monazite and bastnasite rare earth processing",
        "mixed rare earth oxide",
        "neodymium magnet material",
        "industrial catalysts",
        "chlor-alkali bleach and peroxide",
        "soap, surfactants and detergents",
        "ABS and acrylic polymer chemistry",
        "polyurethane foam",
        "copper wire and printed circuit boards",
        "solder alloys and flux",
        "LED phosphor and ceramic electronics substrates",
        "zinc, lead and tin metallurgy",
        "kaolin ceramics and firebrick",
        "mineral wool insulation",
        "ammonium sulfate and superphosphate fertilizer",
        "urea formaldehyde resin",
        "syngas and carbon monoxide processing",
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
    last_error: Exception | None = None
    for attempt in range(1, attempts + 1):
        try:
            download = requests.get(url, timeout=300)
            download.raise_for_status()
            return download.content
        except requests.RequestException as exc:
            last_error = exc
            if attempt >= attempts:
                raise
            wait = min(90, 8 * attempt)
            print(f"  download failed for {item_id}, retry {attempt}/{attempts} after {wait}s: {exc}", flush=True)
            time.sleep(wait)
    raise RuntimeError(f"Agnes image download failed for {item_id}: {last_error}")


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
