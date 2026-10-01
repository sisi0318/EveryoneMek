"""Mechanically project the installed Oritech textures onto the generated cube; no artwork is repainted."""
from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw, ImageEnhance
import argparse
import io
import json

parser = argparse.ArgumentParser()
parser.add_argument('--oritech', type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
model = json.loads((root / 'src/main/resources/assets/oritechmekanism/models/block/universal_processor.json').read_text(encoding='utf-8'))
canvas = Image.new('RGB', (512, 448), (35, 39, 44))
faces = [
    ('north', [(64, 120), (256, 216), (256, 408), (64, 312)], .90),
    ('east', [(256, 216), (448, 120), (448, 312), (256, 408)], .72),
    ('up', [(256, 24), (448, 120), (256, 216), (64, 120)], 1.0),
]
with ZipFile(args.oritech) as archive:
    for side, points, brightness in faces:
        namespace, path = model['textures'][side].split(':', 1)
        texture = Image.open(io.BytesIO(archive.read(f'assets/{namespace}/textures/{path}.png'))).convert('RGB')
        texture = ImageEnhance.Brightness(texture).enhance(brightness)
        x0, y0 = points[0]
        ax, ay = (points[1][0] - x0) / 16, (points[1][1] - y0) / 16
        bx, by = (points[3][0] - x0) / 16, (points[3][1] - y0) / 16
        determinant = ax * by - ay * bx
        a, b, d, e = by / determinant, -bx / determinant, -ay / determinant, ax / determinant
        warped = texture.transform(canvas.size, Image.Transform.AFFINE, (a, b, -a * x0 - b * y0, d, e, -d * x0 - e * y0), resample=Image.Resampling.NEAREST)
        mask = Image.new('L', canvas.size)
        ImageDraw.Draw(mask).polygon(points, fill=255)
        canvas.paste(warped, (0, 0), mask)
output = root / 'build/style-reference/processor-preview.png'
output.parent.mkdir(parents=True, exist_ok=True)
canvas.save(output)
print(output)
