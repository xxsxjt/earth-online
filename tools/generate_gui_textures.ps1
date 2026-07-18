param(
    [string]$OutputRoot = (Join-Path $PSScriptRoot '..\neoforge-26.2\src\main\resources\assets\earth_on_minecraft\textures\gui\container')
)

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$canvasWidth = 256
$canvasHeight = 256
$guiWidth = 176
$guiHeight = 166

function Get-VanillaContainerBase {
    $artifactRoot = Join-Path $PSScriptRoot '..\neoforge-26.2\build\moddev\artifacts'
    $jar = Get-ChildItem -LiteralPath $artifactRoot -Filter 'minecraft-patched-*.jar' -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $jar) {
        return $null
    }

    $archive = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        $entry = $archive.GetEntry('assets/minecraft/textures/gui/container/furnace.png')
        if ($null -eq $entry) {
            return $null
        }
        $stream = $entry.Open()
        try {
            $source = [System.Drawing.Image]::FromStream($stream)
            try {
                return [System.Drawing.Bitmap]::new($source)
            } finally {
                $source.Dispose()
            }
        } finally {
            $stream.Dispose()
        }
    } finally {
        $archive.Dispose()
    }
}

$vanillaContainerBase = Get-VanillaContainerBase

function Color([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Fill-Rect($graphics, [string]$hex, [int]$x, [int]$y, [int]$width, [int]$height) {
    $brush = [System.Drawing.SolidBrush]::new((Color $hex))
    try {
        $graphics.FillRectangle($brush, $x, $y, $width, $height)
    } finally {
        $brush.Dispose()
    }
}

function Draw-Slot($graphics, [int]$itemX, [int]$itemY) {
    $x = $itemX - 1
    $y = $itemY - 1
    Fill-Rect $graphics '#373737' $x $y 18 18
    Fill-Rect $graphics '#FFFFFF' ($x + 1) ($y + 1) 17 17
    Fill-Rect $graphics '#8B8B8B' ($x + 1) ($y + 1) 16 16
}

function Draw-Base($graphics) {
    Fill-Rect $graphics '#C6C6C6' 0 0 $guiWidth $guiHeight
    Fill-Rect $graphics '#FFFFFF' 0 0 $guiWidth 1
    Fill-Rect $graphics '#FFFFFF' 0 0 1 $guiHeight
    Fill-Rect $graphics '#555555' 0 ($guiHeight - 1) $guiWidth 1
    Fill-Rect $graphics '#555555' ($guiWidth - 1) 0 1 $guiHeight

    for ($row = 0; $row -lt 3; $row++) {
        for ($col = 0; $col -lt 9; $col++) {
            Draw-Slot $graphics (8 + $col * 18) (84 + $row * 18)
        }
    }
    for ($col = 0; $col -lt 9; $col++) {
        Draw-Slot $graphics (8 + $col * 18) 142
    }
}

function Draw-Arrow($graphics, [int]$x, [int]$y) {
    Fill-Rect $graphics '#8B8B8B' $x ($y + 5) 17 6
    Fill-Rect $graphics '#8B8B8B' ($x + 14) ($y + 2) 4 12
    Fill-Rect $graphics '#8B8B8B' ($x + 18) ($y + 4) 3 8
    Fill-Rect $graphics '#8B8B8B' ($x + 21) ($y + 6) 3 4
}

function New-Gui([string]$name, [scriptblock]$drawTop) {
    $bitmap = [System.Drawing.Bitmap]::new(
        $canvasWidth,
        $canvasHeight,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        if ($null -ne $vanillaContainerBase) {
            $graphics.DrawImageUnscaled($vanillaContainerBase, 0, 0)
            Fill-Rect $graphics '#C6C6C6' 2 2 172 80
        } else {
            Draw-Base $graphics
        }
        & $drawTop $graphics

        [System.IO.Directory]::CreateDirectory($OutputRoot) | Out-Null
        $path = Join-Path $OutputRoot $name
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Output $path
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

New-Gui 'processing_machine.png' {
    param($graphics)
    Draw-Slot $graphics 38 35
    Draw-Slot $graphics 38 57
    foreach ($slot in @(
        @(102, 20), @(120, 20), @(138, 20), @(156, 20),
        @(111, 42), @(129, 42), @(147, 42)
    )) {
        Draw-Slot $graphics $slot[0] $slot[1]
    }
    Draw-Arrow $graphics 64 38
}

New-Gui 'energy_generator.png' {
    param($graphics)
    Draw-Slot $graphics 38 57
}

New-Gui 'battery_box.png' {
    param($graphics)
}

if ($null -ne $vanillaContainerBase) {
    $vanillaContainerBase.Dispose()
}
