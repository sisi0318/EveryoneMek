"""Generate design-only construction diagrams and Mek-style UI wireframes; no runtime textures."""
from collections import Counter
from html import escape
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
spec = json.loads((ROOT / "layout.json").read_text(encoding="utf-8"))
size = spec["size"]
cells = []
overrides = {tuple(b["pos"]): b for b in spec["blocks"]}
assert len(overrides) == len(spec["blocks"])
for y in range(size):
    for z in range(size):
        for x in range(size):
            edges = sum(v in (0, size - 1) for v in (x, y, z))
            kind = "frame" if edges >= 2 else "casing" if edges and y in (0, size-1) else "glass" if edges else "air"
            cell = dict(kind=kind, pos=[x, y, z])
            cell.update(overrides.get((x, y, z), {}))
            cells.append(cell)
counts = Counter(c["kind"] for c in cells)
assert counts == Counter(frame=68, casing=50, glass=93, controller=1, fuel=1,
                         excitation=1, output=4, coil=6, core=1, air=118)
lookup = {tuple(c["pos"]): c for c in cells}
vectors = dict(left=(-1,0,0), right=(1,0,0), front=(0,0,-1), back=(0,0,1), up=(0,1,0), down=(0,-1,0))
for b in spec["blocks"]:
    if b["kind"] == "core":
        assert b["pos"] == [3,3,3]
        continue
    v = vectors[b["face"]]
    next_pos = tuple(p+d for p,d in zip(b["pos"], v))
    if b["kind"] == "coil":
        assert lookup[next_pos]["kind"] == "air"
        assert tuple(p+2*d for p,d in zip(b["pos"], v)) == (3,3,3)
    else:
        assert any(p < 0 or p >= size for p in next_pos), b

marks = dict(left="←", right="→", front="↑", back="↓", up="向上", down="向下")
titles = ["底板", "接口与下线圈", "反应腔", "核心与四向线圈", "反应腔", "上线圈", "顶盖"]
face_labels = dict(left="向左", right="向右", front="朝正面", back="朝背面", up="向上", down="向下")

def text(x, y, content, color="#dee5ec", font_size=18, anchor="start", extra=""):
    return f'<text x="{x}" y="{y}" fill="{color}" font-size="{font_size}" text-anchor="{anchor}" {extra}>{escape(str(content))}</text>'

def rect(x, y, w, h, fill, extra=""):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}" {extra}/>'

svg = ['<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="1070" viewBox="0 0 1600 1070" role="img" aria-labelledby="title desc">',
       '<title id="title">引力约束反应堆：七层搭建图</title>',
       '<desc id="desc">每层均从顶部向下看，上边为主控正面。225 个方块，118 格留空。线圈箭头朝向核心。</desc>',
       '<g font-family="Microsoft YaHei, sans-serif">', rect(0,0,1600,1070,"#20262d"), text(32,43,"引力约束反应堆 · 7 × 7 × 7",font_size=29),
       text(32,77,"每层从上往下看 · 图上方为主控正面 · 第 1 层为底部 · alpha.2 默认布局",color="#aebcc8",font_size=19)]
for layer in range(7):
    ox, oy = 32 + layer % 4 * 393, 106 + layer // 4 * 465
    svg += [rect(ox,oy,367,440,"#29313b"), text(ox+18,oy+33,f"第 {layer+1} 层 · {titles[layer]}",font_size=23), text(ox+185,oy+65,"正面",font_size=15,anchor="middle",color="#aebcc8")]
    for c in (c for c in cells if c["pos"][1] == layer):
        x,y,z=c["pos"]; kind=spec["kinds"][c["kind"]]; px,py=ox+42+x*40,oy+82+z*40
        mark=marks[c["face"]] if c["kind"]=="coil" else kind["mark"]
        svg.append(rect(px,py,38,38,kind["color"]))
        svg.append(text(px+19,py+26,mark,color="#e5e9ee" if c["kind"]=="air" else "#18212b",font_size=15 if len(mark)>1 else 20,anchor="middle"))
    number=sum(c["kind"]!="air" for c in cells if c["pos"][1]==layer)
    svg += [text(ox+18,oy+399,f"{number} 个方块 · {49-number} 格留空",font_size=18), text(ox+18,oy+424,"线圈箭头朝向核心；向上/向下为竖直方向。",color="#aebcc8",font_size=13)]
ox,oy=32+3*393,571
svg += [rect(ox,oy,367,440,"#29313b"),text(ox+18,oy+33,"材料清单",font_size=23)]
for i,(key,kind) in enumerate(spec["kinds"].items()):
    py=oy+66+i*29
    svg.extend([rect(ox+18,py-16,18,18,kind["color"]),text(ox+46,py,kind["label"],font_size=17),text(ox+339,py,counts[key],font_size=17,anchor="end")])
svg += [text(32,1043,"线圈等级可更换，六组取最低等级。默认布局含 4 个发电口；图示为设计，不是游戏内成型验证。",font_size=18,color="#aebcc8"),"</g></svg>"]
(ROOT/"layers.svg").write_text("\n".join(svg)+"\n",encoding="utf-8")

def bevel(x,y,w,h,fill="#c6c6c6",inset=False):
    a,b=("#454545","#ffffff") if inset else ("#ffffff","#555555")
    return rect(x,y,w,h,"#171717")+rect(x+1,y+1,w-2,h-2,fill)+f'<path d="M{x+1} {y+h-2}V{y+1}H{x+w-2}" fill="none" stroke="{a}"/><path d="M{x+w-2} {y+1}V{y+h-2}H{x+1}" fill="none" stroke="{b}"/>'

def slot(x,y): return bevel(x,y,18,18,"#8c8c8c",True)

def player_inventory(x,y):
    s=text(x,y-5,"物品栏",font_size=9,color="#454545")
    for row in range(3):
        for col in range(9): s+=slot(x+col*18,y+row*18)
    for col in range(9): s+=slot(x+col*18,y+58)
    return s

def button(x,y,w,label,action):
    return f'<g data-action="{action}" class="game-button" role="button" tabindex="0" aria-label="{label}">'+bevel(x,y,w,16,"#929292")+text(x+w/2,y+11,label,"#ffffff",8,"middle")+'</g>'

def ui_shell(w,h,title):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="-30 -4 {w+62} {h+8}" class="game-svg" role="group" aria-label="{title}界面草图"><g font-family="Microsoft YaHei,sans-serif">'+bevel(0,0,w,h)+text(w/2,14,title,"#454545",10,"middle")

main = ui_shell(230,244,"引力约束反应堆")
main += '<g data-action="energy" class="game-button" role="button" tabindex="0" aria-label="能量明细">'+bevel(-25,22,24,24)+text(-13,38,"能","#454545",10,"middle")+'</g>'
main += '<g data-action="cooling" class="game-button" role="button" tabindex="0" aria-label="冷却回路">'+bevel(-25,49,24,24)+text(-13,65,"冷","#454545",10,"middle")+'</g>'
main += '<g data-action="structure" class="game-button" role="button" tabindex="0" aria-label="结构设置">'+bevel(-25,76,24,24)+text(-13,92,"构","#454545",10,"middle")+'</g>'
main += '<g data-action="safety" class="game-button" role="button" tabindex="0" aria-label="访问权限">'+bevel(231,22,24,24)+text(243,38,"安","#454545",10,"middle")+'</g>'
main += '<g data-action="redstone" class="game-button" role="button" tabindex="0" aria-label="红石控制">'+bevel(231,49,24,24)+text(243,65,"红","#454545",10,"middle")+'</g>'
for x,label,color,fill in [(8,"钠","#80b8dd",40),(29,"热钠","#d3a15f",15)]:
    main+=text(x+8,24,label,"#454545",7,"middle")+bevel(x,28,16,56,"#353535",True)
    main+=f'<g><title>{label}：悬停查看存量与流量</title>'+rect(x+2,82-fill,12,fill,color)+'</g>'
main+=bevel(51,28,151,56,"#141414",True)
main+=text(57,40,"运行正常","#35f2ac",8,extra='id="game-status"')
main+=text(57,53,"净发电：980 MFE/t","#35f2ac",8,extra='id="game-generation"')
main+=text(57,66,"实际输出：980 MFE/t","#35f2ac",8,extra='id="game-output"')
main+=text(57,78,"约束稳定度：99%","#35f2ac",8,extra='id="game-stability"')
main+=bevel(208,28,6,101,"#353535",True)+rect(210,56,2,71,"#55edbd")
main+=text(8,96,"反应余量","#454545",8)+bevel(8,101,194,6,"#404040",True)+rect(10,103,129,2,"#a088c9")
main+=button(8,114,94,"负载上限 100%","load")+button(108,114,94,"停机","power")
main+=player_inventory(34,158)
main+="</g></svg>"

fuel = ui_shell(176,208,"燃料仓")+text(8,27,"燃料","#454545",9)
for row in range(2):
    for col in range(9): fuel+=slot(7+18*col,32+row*18)
fuel+=text(16,45,"燃","#eeeeee",8,"middle")+text(23,48,"32","#ffffff",6,"end")
fuel+=text(8,84,"可放入：致密燃料丸","#454545",8)+text(8,99,"接入物品管道可持续供料","#454545",8)
fuel+=player_inventory(7,120)+"</g></svg>"
for filename,content in [("main-ui.svg",main),("fuel-ui.svg",fuel)]:
    (ROOT/filename).write_text(content+"\n",encoding="utf-8")

template=(ROOT/"board.template.html").read_text(encoding="utf-8")
board=template.replace("__DATA__",json.dumps(dict(spec=spec,cells=cells,counts=dict(counts),titles=titles),ensure_ascii=False)).replace("__MAIN_UI__",main).replace("__FUEL_UI__",fuel)
assert all(token not in board for token in ("__DATA__","__MAIN_UI__","__FUEL_UI__"))
(ROOT/"design-board.html").write_text(board,encoding="utf-8")
print("Design verified: 343 cells, 225 blocks, 118 air; all 6 coils face the core across an air gap; all 7 shell devices face out.")
