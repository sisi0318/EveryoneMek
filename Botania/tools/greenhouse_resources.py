"""Default native formula adapters. Fixed recipes can be supplied by datapacks."""


def generate(write, zh, en):
    mod = 'botanicalmekanism'
    for flower in ['endoflame', 'gourmaryllis', 'spectrolus', 'thermalily', 'kekimurus', 'munchdew', 'entropinnyum', 'rafflowsia']:
        write(f'data/{mod}/recipe/greenhouse/{flower}.json', {
            'type': f'{mod}:mana_greenhouse', 'formula': flower,
            'flower': [{'item': f'botania:{flower}'}, {'item': f'botania:floating_{flower}'}]})
    for key, cn, english in [
        ('flower', '产能花', 'Generating flower'), ('ticks', '用时：%s tick', 'Time: %s ticks'),
        ('cooldown', '冷却：%s tick', 'Cooldown: %s ticks'),
        ('variety', '首次产量；交替投料可增加产量', 'First yield; varies with meals'),
        ('spectrolus', '按花的颜色顺序供料', 'Follow the colour sequence'),
        ('munchdew', '断料或满仓后休息 80 秒', 'Empty/full: rests 80 s'),
    ]:
        zh[f'jei.{mod}.greenhouse.{key}'], en[f'jei.{mod}.greenhouse.{key}'] = cn, english
