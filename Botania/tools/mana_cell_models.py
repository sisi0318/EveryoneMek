"""Reuse AE's native cell models, with an animated mana label; no copied AE art."""
TIERS = (1, 4, 16, 64, 256)


def cell_id(tier):
    return 'mana_storage_cell' + (f'_{tier}k' if tier != 1 else '')


def label(a, b, side, cull=False):
    return {'from': a, 'to': b, 'shade': False, 'faces': {
        side: {'texture': '#mana', 'uv': [0, 0, 16, 16], **({'cullface': 'north'} if cull else {})}}}


def generate(write):
    for tier in TIERS:
        name = cell_id(tier)
        write(f'assets/botanicalmekanism/models/item/{name}.json', {
            'parent': 'minecraft:item/generated', 'loader': 'botanicalmekanism:mana_cell',
            'textures': {'particle': 'botania:block/mana_water'},
            'children': {
                'base': {'parent': f'ae2:item/fluid_storage_cell_{tier}k'},
                'mana': {'textures': {'mana': 'botania:block/mana_water'}, 'elements': [
                    label([5, 6, 7.48], [11, 9, 7.48], 'north'),
                    label([5, 6, 8.52], [11, 9, 8.52], 'south')]},
            }, 'item_render_order': ['base', 'mana'],
        })
        write(f'assets/botanicalmekanism/models/block/drive/{name}.json', {
            'loader': 'neoforge:composite', 'ambientocclusion': False,
            'textures': {'particle': 'ae2:block/drive/drive_cells'},
            'children': {
                'base': {'parent': f'ae2:block/drive/cells/{tier}k_fluid_cell'},
                # AE's status LED occupies x=4..5, y=0..1; keep it unobstructed.
                'mana': {'textures': {'mana': 'botania:block/mana_water'}, 'elements': [
                    label([1, .5, -.01], [3, 1.5, -.01], 'north', True)]},
            },
        })
