"""Original thin mana cartridge and AE drive insert, using native Botania materials."""
from botanical_models import box

PALETTE = {
    'case': 'botania:block/polished_livingrock',
    'metal': 'botania:block/manasteel_block',
    'core': 'botania:block/mana_water',
    'particle': 'botania:block/polished_livingrock',
}


def casing(a, b, material='case'):
    return box(a, b, material, uv=[0, 0, 1, 1] if material == 'metal' else [4, 4, 12, 12])


def window(a, b, face):
    return {'from': a, 'to': b, 'shade': False,
            'faces': {face: {'texture': '#core', 'uv': [0, 0, 16, 16]}}}


def generate(write):
    # Stepped corners and a two-pixel rim give the cell a cartridge silhouette.
    elements = [
        casing((2, 3, 7), (14, 13, 9), 'metal'),
        casing((3, 2, 7), (13, 3, 9), 'metal'),
        casing((3, 13, 7), (13, 14, 9), 'metal'),
        casing((3, 4, 6.875), (13, 12, 9.125)),
        casing((4, 3, 6.875), (12, 4, 9.125)),
        casing((4, 12, 6.875), (12, 13, 9.125)),
        casing((3.75, 6.75, 6.75), (12.25, 11.25, 9.25), 'metal'),
        window([4, 7, 6.73], [12, 11, 6.73], 'north'),
        window([4, 7, 9.27], [12, 11, 9.27], 'south'),
    ]
    for x in (4, 7, 10):
        elements.append(box((x, 1, 7.25), (x + 2, 3, 8.75), 'metal', uv=[3, 1, 4, 3]))
    write('assets/botanicalmekanism/models/item/mana_storage_cell.json', {
        'parent': 'minecraft:block/block', 'gui_light': 'front', 'ambientocclusion': False,
        'textures': PALETTE,
        'display': {
            'gui': {'rotation': [12, -20, 0], 'translation': [0, 0, 0], 'scale': [1.05, 1.05, 1.05]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [.5, .5, .5]},
            'fixed': {'rotation': [0, 0, 0], 'scale': [.8, .8, .8]},
            'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 1], 'scale': [.5, .5, .5]},
            'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 1], 'scale': [.5, .5, .5]},
            'firstperson_righthand': {'rotation': [0, -25, 0], 'scale': [.65, .65, .65]},
            'firstperson_lefthand': {'rotation': [0, 25, 0], 'scale': [.65, .65, .65]},
        }, 'elements': elements,
    })
    # AE places a 6 x 2 x 2 insert at each slot origin. Its own status LED occupies
    # x=4..5, y=0..1 on the north face; keep the mana window to its left.
    insert = [
        casing((0, 0, 0), (6, 2, 2)),
        casing((.5, .25, -.02), (3.75, 1.75, .125), 'metal'),
        window([.75, .5, -.04], [3.5, 1.5, -.04], 'north'),
    ]
    for element in insert:
        element['faces'] = {side: dict(face, cullface='north') for side, face in element['faces'].items()
                            if side in ('north', 'up', 'down')}
    write('assets/botanicalmekanism/models/block/drive/mana_storage_cell.json', {
        'ambientocclusion': False, 'textures': PALETTE, 'elements': insert,
    })
