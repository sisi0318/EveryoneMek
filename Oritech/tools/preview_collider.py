"""Project generated collider cuboids with the dependency's original textures for local QA."""
from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageEnhance
import numpy as np
import argparse
import io
import json

parser = argparse.ArgumentParser()
parser.add_argument('--oritech', type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
model = json.loads((root / 'src/main/resources/assets/oritechmekanism/models/block/mini_particle_collider.json').read_text(encoding='utf-8'))
canvas = Image.new('RGB', (512, 448), (35, 39, 44))
pixels = np.array(canvas)
depth_buffer = np.full((448, 512), -np.inf)
faces = []
def project(p):
    x, y, z = p
    return (256 + 12 * (x + z - 16), 312 + 6 * (x - z) - 12 * y)
for element in model['elements']:
    x0, y0, z0 = element['from']
    x1, y1, z1 = element['to']
    visible = {
        'north': ([(x0,y1,z0),(x1,y1,z0),(x1,y0,z0),(x0,y0,z0)], [16-x1,16-y1,16-x0,16-y0], .90),
        'east': ([(x1,y1,z0),(x1,y1,z1),(x1,y0,z1),(x1,y0,z0)], [16-z1,16-y1,16-z0,16-y0], .72),
        'up': ([(x0,y1,z1),(x1,y1,z1),(x1,y1,z0),(x0,y1,z0)], [x0,z0,x1,z1], 1.0),
    }
    for side, (points, default_uv, brightness) in visible.items():
        face = element['faces'][side]
        uv = face.get('uv', default_uv)
        depth = sum(p[0] - p[2] + p[1] for p in points) / 4
        faces.append((depth, points, uv, model['textures'][face['texture'][1:]], brightness))
def triangle(points, uv, texture):
    xy = np.array([project(p) for p in points])
    depth = np.array([p[0]+p[1]-p[2] for p in points])
    left, top = np.maximum(0, np.floor(xy.min(axis=0)).astype(int))
    right, bottom = np.minimum([511, 447], np.ceil(xy.max(axis=0)).astype(int))
    xx, yy = np.meshgrid(np.arange(left, right+1)+.5, np.arange(top, bottom+1)+.5)
    denominator = (xy[1,1]-xy[2,1])*(xy[0,0]-xy[2,0])+(xy[2,0]-xy[1,0])*(xy[0,1]-xy[2,1])
    a = ((xy[1,1]-xy[2,1])*(xx-xy[2,0])+(xy[2,0]-xy[1,0])*(yy-xy[2,1]))/denominator
    b = ((xy[2,1]-xy[0,1])*(xx-xy[2,0])+(xy[0,0]-xy[2,0])*(yy-xy[2,1]))/denominator
    c = 1-a-b
    z = a*depth[0]+b*depth[1]+c*depth[2]
    depth_view = depth_buffer[top:bottom+1,left:right+1]
    mask = (a>=-1e-6)&(b>=-1e-6)&(c>=-1e-6)&(z>depth_view)
    uv = np.array(uv)
    u = np.clip(np.floor(a*uv[0,0]+b*uv[1,0]+c*uv[2,0]).astype(int),0,15)
    v = np.clip(np.floor(a*uv[0,1]+b*uv[1,1]+c*uv[2,1]).astype(int),0,15)
    pixels[top:bottom+1,left:right+1][mask] = np.array(texture)[v[mask],u[mask]]
    depth_view[mask] = z[mask]
with ZipFile(args.oritech) as archive:
    for _, points3, uv, reference, brightness in sorted(faces, key=lambda entry: entry[0]):
        namespace, path = reference.split(':', 1)
        texture = Image.open(io.BytesIO(archive.read(f'assets/{namespace}/textures/{path}.png'))).convert('RGB')
        texture = ImageEnhance.Brightness(texture).enhance(brightness)
        u0, v0, u1, v1 = uv
        tex_coords = [(u0,v0),(u1,v0),(u1,v1),(u0,v1)]
        for indices in [(0,1,2),(0,2,3)]:
            triangle([points3[i] for i in indices], [tex_coords[i] for i in indices], texture)
out = root / 'build/style-reference/collider-preview.png'
out.parent.mkdir(parents=True, exist_ok=True)
Image.fromarray(pixels).save(out)
print(out)
