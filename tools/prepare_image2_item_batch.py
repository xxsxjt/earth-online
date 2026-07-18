#!/usr/bin/env python3
"""Prepare and post-process gpt-image-2 item texture batches.

The bundled image generation CLI remains responsible for API calls. This tool
only builds prompt JSONL, removes flat backgrounds, downsamples, and audits the
resulting Minecraft item textures.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import pathlib
import time

from PIL import Image, ImageDraw


ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "neoforge-26.2" / "src" / "main" / "resources" / "assets" / "earth_on_minecraft"
ITEM_TEXTURES = ASSETS / "textures" / "item"
LANG = ASSETS / "lang" / "en_us.json"
AGNES_TOOL = ROOT / "tools" / "generate_agnes_item_textures.py"


def load_texture_helpers():
    spec = importlib.util.spec_from_file_location("earth_item_texture_helpers", AGNES_TOOL)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load texture helpers from {AGNES_TOOL}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def image_size(path: pathlib.Path) -> tuple[int, int]:
    with Image.open(path) as image:
        return image.size


def low_resolution_ids(min_size: int) -> list[str]:
    selected = []
    for path in sorted(ITEM_TEXTURES.glob("*.png")):
        width, height = image_size(path)
        if width < min_size or height < min_size:
            selected.append(path.stem)
    return selected


def display_names() -> dict[str, str]:
    data = json.loads(LANG.read_text(encoding="utf-8"))
    prefix = "item.earth_on_minecraft."
    return {key.removeprefix(prefix): value for key, value in data.items() if key.startswith(prefix)}


def inferred_descriptor(item_id: str, display_name: str) -> str:
    readable = display_name or item_id.replace("_", " ")
    if item_id.endswith("_cell"):
        return f"a compact sealed industrial gas sample cylinder representing {readable}, with a colored glass status window and metal valve cap"
    if item_id.endswith(("_acid", "_water", "_sample")) or item_id in {
        "ammonia", "benzene", "ethylene", "formaldehyde", "methanol", "propylene", "urea", "vinyl_chloride"
    }:
        return f"a small sealed laboratory-industrial vial or canister containing {readable}, using a physically plausible material color"
    if item_id.endswith(("_dust", "_powder")):
        return f"a small granular pile of {readable}, with physically plausible mineral or chemical color and texture"
    if item_id.endswith(("_concentrate", "_meal")):
        return f"a dense processed mineral concentrate pile representing {readable}, with realistic grains and restrained metallic highlights"
    if item_id.endswith(("_resin", "_pellet")):
        return f"a small cluster of industrial {readable} pellets, clean molded granules with a readable material silhouette"
    if item_id.endswith(("_ingot", "_bloom", "_billet")):
        return f"a compact metallurgical {readable} billet or ingot with realistic metal color and forged edges"
    if item_id.endswith(("_module", "_controller", "_switchgear", "_gateway", "_camera", "_motor", "_drive", "_bus")):
        return f"a compact modern industrial automation component representing {readable}, with a unique functional silhouette and no readable markings"
    if item_id.endswith(("_assembly", "_rod", "_tube", "_cask")):
        return f"a compact engineered industrial component representing {readable}, mechanically coherent and physically plausible"
    return f"a physically plausible industrial chemistry or metallurgy item representing {readable}, with a clear material-specific silhouette"


def image2_prompt(item_id: str, descriptor: str) -> str:
    return (
        "Use case: stylized-concept\n"
        "Asset type: Minecraft mod item texture source art for Earth on Minecraft\n"
        f"Primary request: one isolated item icon for {item_id}\n"
        f"Subject: {descriptor}.\n"
        "Style/medium: true hand-crafted pixel art at an apparent 128x128 working resolution, clearly visible square pixel clusters, limited material-specific palette, hard clean pixel edges, modern Minecraft mod style, readable at 128x128 and 64x64\n"
        "Composition/framing: one centered object or compact material pile, generous padding, no inventory slot, no scene, no floor plane, no cast shadow\n"
        "Background: perfectly flat solid #ff00ff chroma-key background, uniform edge to edge, no gradients, texture, lighting variation, or reflections; do not use #ff00ff in the subject\n"
        "Constraints: no text, no letters, no pseudo-letters, no numbers, no labels, no chemical formula, no watermark, no logo, no UI frame\n"
        "Avoid: smooth photoreal product rendering, photographic glass reflections, subpixel antialiasing, airbrushed gradients, full machine block, fantasy ornament, steampunk, rusty junk, blurry silhouette, multiple unrelated objects"
    )


def write_prompts(path: pathlib.Path, selected: list[str]) -> None:
    helpers = load_texture_helpers()
    names = display_names()
    lines = []
    for item_id in selected:
        if item_id in helpers.ITEMS:
            descriptor = helpers.ITEMS[item_id][2]
        else:
            descriptor = inferred_descriptor(item_id, names.get(item_id, ""))
        lines.append(json.dumps({"id": item_id, "prompt": image2_prompt(item_id, descriptor)}, ensure_ascii=False))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"IMAGE2_ITEM_PROMPTS_WRITTEN count={len(lines)} path={path}")


def prompt_ids(path: pathlib.Path) -> list[str]:
    return [json.loads(line)["id"] for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def terminal_failure(path: pathlib.Path) -> bool:
    if not path.exists():
        return False
    text = path.read_text(encoding="utf-8", errors="ignore")
    return "Traceback (most recent call last)" in text or "Error:" in text or "PermissionDeniedError" in text


def wait_for_batch(raw_dir: pathlib.Path, selected: list[str], wait_seconds: int, poll_seconds: int) -> None:
    if wait_seconds <= 0:
        return
    deadline = time.monotonic() + wait_seconds
    while time.monotonic() < deadline:
        complete = sum((raw_dir / f"{item_id}.png").exists() for item_id in selected)
        failed = sum(terminal_failure(raw_dir / f"{item_id}.err.log") for item_id in selected)
        if complete + failed >= len(selected):
            return
        print(f"IMAGE2_ITEM_BATCH_WAIT complete={complete} failed={failed} total={len(selected)}", flush=True)
        time.sleep(max(2, poll_seconds))


def make_contact_sheet(selected: list[str], target: pathlib.Path) -> None:
    cell = 48
    thumb = 32
    cols = 10
    rows = (len(selected) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (24, 24, 24, 255))
    draw = ImageDraw.Draw(sheet)
    for index, item_id in enumerate(selected):
        path = ITEM_TEXTURES / f"{item_id}.png"
        if not path.exists():
            continue
        with Image.open(path) as image:
            icon = image.convert("RGBA").resize((thumb, thumb), Image.Resampling.NEAREST)
        x = (index % cols) * cell + 8
        y = (index // cols) * cell + 4
        sheet.alpha_composite(icon, (x, y))
        draw.text((x, y + 34), str(index + 1), fill=(210, 210, 210, 255))
    target.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(target)


def process_batch(raw_dir: pathlib.Path, selected: list[str], size: int) -> None:
    helpers = load_texture_helpers()
    completed = []
    failed = []
    missing = []
    for item_id in selected:
        raw = raw_dir / f"{item_id}.png"
        if raw.exists():
            helpers.crop_and_downscale(raw, ITEM_TEXTURES / f"{item_id}.png", size)
            completed.append(item_id)
        elif terminal_failure(raw_dir / f"{item_id}.err.log"):
            failed.append(item_id)
        else:
            missing.append(item_id)
    contact_sheet = raw_dir / f"item_contact_sheet_{size}px.png"
    make_contact_sheet(completed, contact_sheet)
    report = {
        "completed": completed,
        "failed": failed,
        "missing": missing,
        "size": size,
        "contact_sheet": str(contact_sheet),
    }
    report_path = raw_dir / "processing-report.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(
        f"IMAGE2_ITEM_PROCESSING_DONE completed={len(completed)} failed={len(failed)} "
        f"missing={len(missing)} report={report_path}"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write-prompts", type=pathlib.Path)
    parser.add_argument("--prompt-file", type=pathlib.Path)
    parser.add_argument("--process-dir", type=pathlib.Path)
    parser.add_argument("--min-existing-size", type=int, default=128)
    parser.add_argument("--size", type=int, default=128, choices=[32, 64, 128])
    parser.add_argument("--wait-seconds", type=int, default=0)
    parser.add_argument("--poll-seconds", type=int, default=15)
    args = parser.parse_args()

    selected = prompt_ids(args.prompt_file) if args.prompt_file else low_resolution_ids(args.min_existing_size)
    if args.write_prompts:
        write_prompts(args.write_prompts, selected)
    if args.process_dir:
        wait_for_batch(args.process_dir, selected, args.wait_seconds, args.poll_seconds)
        process_batch(args.process_dir, selected, args.size)
    if not args.write_prompts and not args.process_dir:
        parser.error("Specify --write-prompts and/or --process-dir")


if __name__ == "__main__":
    main()
