"""Cut covered cuboid faces out of a model, preserving its original UV mapping.

Solid boxes remain the collision source. Later boxes own shared exterior faces;
rotated plant planes are kept unchanged.
"""
import copy

FACES = {
    'north': (2, -1, 0, -1, 1, -1), 'south': (2, 1, 0, 1, 1, -1),
    'west': (0, -1, 2, 1, 1, -1), 'east': (0, 1, 2, -1, 1, -1),
    'up': (1, 1, 0, 1, 2, 1), 'down': (1, -1, 0, 1, 2, -1),
}


def subtract(rect, cover):
    x, y, X, Y = rect
    a, b, A, B = max(x, cover[0]), max(y, cover[1]), min(X, cover[2]), min(Y, cover[3])
    if a >= A or b >= B:
        return [rect]
    return [r for r in [(x, y, a, Y), (A, y, X, Y), (a, y, A, b), (a, B, A, Y)] if r[0] < r[2] and r[1] < r[3]]


def exterior(elements):
    out = []
    solids = [(i, e) for i, e in enumerate(elements) if 'rotation' not in e and all(a < b for a, b in zip(e['from'], e['to']))]
    for i, element in enumerate(elements):
        if 'rotation' in element or any(a == b for a, b in zip(element['from'], element['to'])):
            out.append(copy.deepcopy(element)); continue
        for side, face in element['faces'].items():
            axis, normal, u, us, v, vs = FACES[side]
            lo, hi = element['from'], element['to']; plane = hi[axis] if normal > 0 else lo[axis]
            pieces = [(lo[u], lo[v], hi[u], hi[v])]
            for j, other in solids:
                if j == i:
                    continue
                a, b = other['from'][axis], other['to'][axis]
                outside = a <= plane < b if normal > 0 else a < plane <= b
                shared = j > i and (b if normal > 0 else a) == plane
                if not (outside or shared):
                    continue
                cover = (other['from'][u], other['from'][v], other['to'][u], other['to'][v])
                pieces = [piece for r in pieces for piece in subtract(r, cover)]
                if not pieces:
                    break
            uv = face['uv']
            def tex(value, dim, sign, start, end):
                fraction = (value - lo[dim]) / (hi[dim] - lo[dim])
                return round(start + (fraction if sign > 0 else 1 - fraction) * (end - start), 6)
            for x, y, X, Y in pieces:
                a, b = list(lo), list(hi); a[axis] = b[axis] = plane
                a[u], a[v], b[u], b[v] = x, y, X, Y
                cropped = dict(face, uv=[tex(x if us > 0 else X, u, us, uv[0], uv[2]),
                                         tex(y if vs > 0 else Y, v, vs, uv[1], uv[3]),
                                         tex(X if us > 0 else x, u, us, uv[0], uv[2]),
                                         tex(Y if vs > 0 else y, v, vs, uv[1], uv[3])])
                out.append({**{k: copy.deepcopy(value) for k, value in element.items() if k not in ('from', 'to', 'faces')},
                            'from': a, 'to': b, 'faces': {side: cropped}})
    return out


def validate(elements):
    """The rendered axial faces must not share any coplanar area."""
    faces = []
    for element in elements:
        if 'rotation' in element:
            continue
        for side in element['faces']:
            axis, normal, u, _, v, _ = FACES[side]
            lo, hi = element['from'], element['to']
            plane = hi[axis] if normal > 0 else lo[axis]
            faces.append((axis, plane, lo[u], lo[v], hi[u], hi[v]))
    for i, a in enumerate(faces):
        for b in faces[i + 1:]:
            if a[:2] == b[:2] and min(a[4], b[4]) > max(a[2], b[2]) and min(a[5], b[5]) > max(a[3], b[3]):
                raise ValueError(f'Overlapping model faces: {a}, {b}')
