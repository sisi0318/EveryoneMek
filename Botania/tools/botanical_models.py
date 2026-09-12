"""Original apparatus geometry using Botania's runtime materials; no copied bitmaps.

The same solid elements generate models and rotated collision/selection shapes.
Plant displays are decorative cutout planes, not hidden flower entities.
"""
import json

MOD = 'botanicalmekanism'
PALETTE = {
    'stone': 'botania:block/polished_livingrock',
    'wood': 'botania:block/livingwood_log',
    'wood_end': 'botania:block/livingwood_log_top',
    'rune': 'botania:block/runic_altar_top',
    'terra': 'botania:block/terrestrial_agglomeration_plate_top',
    'metal': 'botania:block/manasteel_block',
    'green': 'botania:block/terrasteel_block',
    'pink': 'botania:block/elementium_block',
    'crystal': 'botania:block/mana_diamond_block',
    'elf_crystal': 'botania:block/dragonstone_block',
    'daisy': 'botania:block/pure_daisy',
    'orechid': 'botania:block/orechid',
    'morph_flower': 'botania:block/marimorphosis',
    'rock': 'minecraft:block/stone',
}


def box(a, b, material='stone', top=None, uv=None):
    x, y, z = a
    X, Y, Z = b
    uvs = {'down': [x, 16-Z, X, 16-z], 'up': [x, z, X, Z],
           'north': [16-X, 16-Y, 16-x, 16-y], 'south': [x, 16-Y, X, 16-y],
           'west': [z, 16-Y, Z, 16-y], 'east': [16-Z, 16-Y, 16-z, 16-y]}
    return {'from': list(a), 'to': list(b), 'faces': {face: {'texture': '#' + (top if face == 'up' and top else material),
            'uv': list(uv if uv is not None else uvs[face])} for face in uvs}}


def slab(a, b, top):
    result = box(a, b)
    result['faces']['up'] = {'texture': '#' + top, 'uv': [0, 0, 16, 16]}
    return result


def base():
    return [box((1, 0, 1), (15, 2, 15)), box((3, 2, 3), (13, 4, 13), 'wood', 'wood_end')]


def ports():
    # Four small sockets meet adjacent pipe centers. The central stem remains exposed.
    return [box((0, 6, 6), (4, 10, 10), 'wood'), box((12, 6, 6), (16, 10, 10), 'wood'),
            box((6, 6, 0), (10, 10, 4), 'wood'), box((6, 6, 12), (10, 10, 16), 'wood'),
            box((0, 7, 7), (.25, 9, 9), 'metal', uv=[6, 6, 10, 10]),
            box((15.75, 7, 7), (16, 9, 9), 'metal', uv=[6, 6, 10, 10]),
            box((7, 7, 0), (9, 9, .25), 'indicator', uv=[6, 6, 10, 10]),
            box((7, 7, 15.75), (9, 9, 16), 'metal', uv=[6, 6, 10, 10])]


def crystal(x, y, z, size=4, material='indicator'):
    half = size / 2
    return [box((x-half/2, y, z-half/2), (x+half/2, y+1, z+half/2), material, uv=[4, 4, 12, 12]),
            box((x-half, y+1, z-half), (x+half, y+size-1, z+half), material, uv=[2, 2, 14, 14]),
            box((x-half/2, y+size-1, z-half/2), (x+half/2, y+size, z+half/2), material, uv=[4, 4, 12, 12])]


def bowl(bottom=10, rim=16):
    return [box((4, 4, 4), (12, bottom, 12)), box((1, bottom, 1), (15, bottom+2, 15)),
            box((1, bottom+2, 1), (15, rim, 3)), box((1, bottom+2, 13), (15, rim, 15)),
            box((1, bottom+2, 3), (3, rim, 13)), box((13, bottom+2, 3), (15, rim, 13))]


def plant(x, y, z, size, height, texture):
    a, b = (x-size/2, y, z), (x+size/2, y+height, z)
    return [{'from': list(a), 'to': list(b), 'rotation': {'origin': [x, y, z], 'axis': 'y', 'angle': angle, 'rescale': False},
             'shade': False, 'faces': {face: {'texture': '#' + texture, 'uv': [0, 0, 16, 16]} for face in ('north', 'south')}} for angle in (-45, 45)]


def models():
    shapes = {}
    shapes['mechanical_apothecary'] = base() + bowl() + ports()
    shapes['mana_infuser'] = base() + bowl(8, 13) + ports() + [slab((4, 10, 4), (12, 10.25, 12), 'rune')]
    shapes['runic_forge'] = base() + [box((4, 4, 4), (12, 8, 12)), slab((0, 8, 0), (16, 12, 16), 'rune'),
          box((2, 12, 2), (4, 14, 4), 'wood'), box((12, 12, 2), (14, 14, 4), 'wood'),
          box((2, 12, 12), (4, 14, 14), 'wood'), box((12, 12, 12), (14, 14, 14), 'wood')] + ports()
    shapes['mana_bridge'] = base() + [box((4, 4, 4), (12, 12, 12), 'wood', 'wood_end'),
          box((5, 11, 5), (11, 12, 11))] + ports() + crystal(8, 12, 8)
    shapes['mana_charger'] = base() + [box((5, 4, 5), (11, 10, 11)), slab((2, 10, 2), (14, 12, 14), 'rune'),
          box((2, 12, 6), (4, 15, 10)), box((12, 12, 6), (14, 15, 10))] + ports() + crystal(8, 12, 8, 3)
    shapes['terra_condenser'] = base() + [box((2, 4, 2), (14, 6, 14)), slab((0, 6, 0), (16, 9, 16), 'terra'),
          slab((2, 9, 2), (14, 10, 14), 'terra')] + ports() + crystal(8, 10, 8, 5, 'green')
    shapes['pure_converter'] = base() + bowl(6, 10) + ports() + plant(8, 8, 8, 10, 8, 'daisy')
    shapes['ore_processor'] = base() + [box((3, 4, 3), (13, 7, 13)), box((1, 7, 1), (15, 9, 15)),
          box((9, 9, 5), (14, 14, 11), 'rock'), box((1, 9, 11), (15, 11, 15))] + ports() + plant(5, 9, 7, 7, 7, 'orechid')
    shapes['metamorphic_stone'] = base() + [box((3, 4, 3), (13, 7, 13)), slab((1, 7, 1), (15, 9, 15), 'rune'),
          box((2, 9, 9), (6, 13, 13)), box((10, 9, 9), (14, 13, 13), 'wood')] + ports() + plant(8, 9, 6, 8, 7, 'morph_flower')
    shapes['botanical_brewery'] = base() + [box((3, 4, 3), (13, 6, 13)), box((2, 6, 2), (14, 8, 14)),
          box((2, 8, 11), (4, 15, 13), 'wood'), box((12, 8, 11), (14, 15, 13), 'wood'),
          box((2, 14, 11), (14, 16, 13), 'wood')] + ports()
    for x, color in [(5, 'indicator'), (11, 'green')]:
        shapes['botanical_brewery'] += [box((x-2, 8, 4), (x+2, 12, 8), color, uv=[2, 2, 14, 14]),
              box((x-1, 12, 5), (x+1, 14, 7), color, uv=[6, 0, 10, 8]), box((x-1, 14, 5), (x+1, 15, 7), 'wood')]
    shapes['elven_trade_controller'] = base() + [box((3, 4, 5), (5, 14, 11), 'wood'), box((11, 4, 5), (13, 14, 11), 'wood'),
          box((4, 13, 5), (12, 16, 11), 'wood'), box((5, 4, 5), (11, 6, 11))] + ports() + crystal(8, 8, 8, 4, 'elf_crystal')
    shapes['mana_enchanter_controller'] = base() + [box((4, 4, 4), (12, 10, 12)), slab((1, 10, 1), (15, 12, 15), 'rune'),
          box((2, 12, 11), (4, 16, 13), 'wood'), box((12, 12, 11), (14, 16, 13), 'wood')] + ports() + crystal(8, 12, 8, 4)
    return shapes


def generate(root, write):
    definitions = models()
    for name, elements in definitions.items():
        model = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': True,
                 'textures': {**PALETTE, 'particle': '#stone', 'indicator': PALETTE['metal']}, 'elements': elements}
        write(f'assets/{MOD}/models/block/{name}.json', model)
        write(f'assets/{MOD}/models/block/{name}_active.json', {'parent': f'{MOD}:block/{name}', 'textures': {'indicator': PALETTE['crystal']}})
        write(f'assets/{MOD}/models/item/{name}.json', {'parent': f'{MOD}:block/{name}'})
    manifest = {'version': 'alpha.7', 'method': 'Original JSON geometry; runtime references to the pinned Botania materials. No upstream bitmaps are bundled.',
                'palette': PALETTE, 'models': {name: {'elements': len(elements), 'solid_elements': sum('rotation' not in e for e in elements)} for name, elements in definitions.items()}}
    (root / 'art/botanical-machine-models.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
    # Keep server collision independent of client resource packs, generated from the same solids.
    source = ['package dev.everyonemek.botania;', '', '// Generated by tools/botanical_models.py. Do not edit separately.',
              'public final class BotanicalMachineShapes {', '    private static final java.util.Map<String, net.minecraft.world.phys.shapes.VoxelShape[]> SHAPES = new java.util.HashMap<>();',
              '    static {']
    for name, elements in definitions.items():
        boxes = [e['from'] + e['to'] for e in elements if 'rotation' not in e]
        source.append('        register("' + name + '", new double[][]{')
        source += ['              {' + ', '.join(str(n) for n in bounds) + '},' for bounds in boxes]
        source.append('        });')
    source += ['    }', '    private static void register(String id, double[][] boxes) {',
               '        var rotated = new net.minecraft.world.phys.shapes.VoxelShape[4];',
               '        for (int turn = 0; turn < 4; turn++) {',
               '            var shape = net.minecraft.world.phys.shapes.Shapes.empty();',
               '            for (double[] b : boxes) {', '                double x = b[0], z = b[2], X = b[3], Z = b[5];',
               '                for (int t = 0; t < turn; t++) { double oldX = x, oldMaxX = X; x = 16 - Z; X = 16 - z; z = oldX; Z = oldMaxX; }',
               '                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, net.minecraft.world.level.block.Block.box(x, b[1], z, X, b[4], Z));',
               '            }', '            rotated[turn] = shape.optimize();', '        }', '        SHAPES.put(id, rotated);', '    }',
               '    public static net.minecraft.world.phys.shapes.VoxelShape get(String id, net.minecraft.core.Direction direction) {',
               '        int turn = switch (direction) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };',
               '        return SHAPES.get(id)[turn];', '    }', '    private BotanicalMachineShapes() { }', '}']
    (root / 'src/main/java/dev/everyonemek/botania/BotanicalMachineShapes.java').write_text('\n'.join(source) + '\n', encoding='utf-8', newline='\n')
