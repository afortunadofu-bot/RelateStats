# -*- coding: utf-8 -*-
"""
封測招募的社群素材產生器。

版型沿用 make_assets.py 的社群貼文那一套（藍色系 + 手機框 + 眉標/大標/條列/CTA），
只換文案。⚠️ 不要在這裡另外發明新版面 —— 兩套視覺混著用會讓貼文看起來不像同一個 App。

⚠️ 來源截圖一律取自 00_裁切後截圖／，那是用 App 內「截圖模式」拍的（名字是假名）。
   絕對不要改成用含真實聯絡人姓名的截圖。

文案規則來自共用的品牌規範（小數據所 BRAND.md）：
  · 招募文第一句講隱私，或講「我自己做的」，不要第一句就講功能
  · 不用 emoji
  · 誘因寫身分（致謝名單、首批測試者），不寫現金
  · 每個數字都要指得出來源；指不出來就不要寫
"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

BASE = r'C:\Users\user\Downloads\play商店\relatestat\上架素材'
SRC = os.path.join(BASE, '00_裁切後截圖')
OUT = os.path.join(BASE, '05_封測招募_1080x1350')
os.makedirs(OUT, exist_ok=True)

INK = (15, 23, 42)
BLUE = (37, 99, 235)
SLATE = (71, 85, 105)
BG_TOP = (244, 247, 252)
BG_BOT = (228, 235, 247)

NOTO = r'C:\Windows\Fonts\NotoSansTC-VF.ttf'
MSJH = r'C:\Windows\Fonts\msjh.ttc'
MSJHB = r'C:\Windows\Fonts\msjhbd.ttc'

OVERVIEW, OVERVIEW_TAGS, LEADER, INSIGHT, CONTACTS, SETTINGS, RECAP = (
    '306148', '306149', '306150', '306151', '306152', '306153', '306154')

shots = {}
for fn in sorted(os.listdir(SRC)):
    if fn.endswith('.png'):
        shots[fn[:-4]] = Image.open(os.path.join(SRC, fn)).convert('RGB')


def font(size, weight='Bold'):
    try:
        f = ImageFont.truetype(NOTO, size)
        f.set_variation_by_name(weight)
        return f
    except Exception:
        return ImageFont.truetype(MSJHB if weight in ('Bold', 'Black') else MSJH, size)


def gradient(w, h, top, bot):
    g = Image.new('RGB', (w, h), top)
    dr = ImageDraw.Draw(g)
    for y in range(h):
        t = y / max(1, h - 1)
        dr.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)))
    return g


def phone(shot, width, radius=None, bezel=None):
    radius = radius or int(width * 0.055)
    bezel = bezel if bezel is not None else max(6, int(width * 0.014))
    h = int(shot.height * width / shot.width)
    sc = shot.resize((width, h), Image.LANCZOS)
    W, H = width + bezel * 2, h + bezel * 2
    out = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(out).rounded_rectangle([0, 0, W - 1, H - 1], radius=radius + bezel, fill=INK + (255,))
    mask = Image.new('L', (width, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, width - 1, h - 1], radius=radius, fill=255)
    out.paste(sc, (bezel, bezel), mask)
    return out


def paste_with_shadow(canvas, obj, x, y, blur=28, offset=18, alpha=70):
    sh = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    a = obj.split()[3].point(lambda v: alpha if v > 0 else 0)
    sh.paste((15, 23, 42, 255), (x, y + offset), a)
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    base = canvas.convert('RGBA')
    base.alpha_composite(sh)
    base.alpha_composite(obj, (x, y))
    return base.convert('RGB')


def text_block(dr, x, y, lines, fnt, fill, gap):
    for ln in lines:
        dr.text((x, y), ln, font=fnt, fill=fill)
        y += fnt.size + gap
    return y


def logo(size):
    im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(im).rounded_rectangle([0, 0, size - 1, size - 1],
                                         radius=int(size * 0.26), fill=INK + (255,))
    cy = size * 0.5
    for cx, rr, al in [(0.278, 0.083, 0.55), (0.444, 0.120, 0.85), (0.648, 0.167, -1)]:
        r = size * rr
        col = BLUE + (255,) if al < 0 else (255, 255, 255, int(255 * al))
        layer = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse([size * cx - r, cy - r, size * cx + r, cy + r], fill=col)
        im.alpha_composite(layer)
    return im


def social(name, dark, eye, head, sub, key, phone_w=720, phone_y=610, bullets=None, cta=None,
           head_size=76):
    W, H = 1080, 1350
    if dark:
        cv = gradient(W, H, (15, 23, 42), (23, 37, 84))
        c_head, c_sub, c_eye = (255, 255, 255), (203, 213, 225), (147, 180, 255)
    else:
        cv = gradient(W, H, BG_TOP, BG_BOT)
        c_head, c_sub, c_eye = INK, SLATE, BLUE
    dr = ImageDraw.Draw(cv)
    lg = logo(56)
    cv.paste(lg, (84, 72), lg)
    dr.text((154, 78), 'RelateStats', font=font(36, 'Black'), fill=c_head)
    dr.text((84, 178), eye, font=font(32, 'Bold'), fill=c_eye)
    y = text_block(dr, 84, 226, head, font(head_size, 'Black'), c_head, 10)
    y = text_block(dr, 84, y + 14, sub, font(36, 'Medium'), c_sub, 10)
    if bullets:
        by = y + 36
        for b in bullets:
            dr.ellipse([84, by + 6, 118, by + 40], fill=BLUE)
            dr.line([(93, by + 24), (99, by + 31), (110, by + 16)], fill=(255, 255, 255), width=4)
            dr.text((138, by), b, font=font(36, 'Bold'), fill=c_head)
            by += 66
        y = by
    if cta:
        fw = font(36, 'Black')
        tw = dr.textlength(cta, font=fw)
        dr.rounded_rectangle([84, y + 30, 84 + tw + 72, y + 30 + 84], radius=42, fill=BLUE)
        dr.text((84 + 36, y + 30 + 20), cta, font=fw, fill=(255, 255, 255))
    if key:
        ph = phone(shots[key], phone_w)
        cv = paste_with_shadow(cv, ph, (W - ph.width) // 2, phone_y, alpha=140 if dark else 70)
    cv.save(os.path.join(OUT, name))
    print('saved', name)


# ── 1. 主招募：第一句講隱私 ──
social('01_資料只存在你的手機.png', True, '上架前封閉測試',
       ['資料只存在', '你的手機'],
       ['App 沒有網路權限，傳不出去。', 'Play 商店的權限頁可以自己查。'],
       SETTINGS, phone_w=600, phone_y=880,
       bullets=['免費，不用註冊帳號', '不收任何個人資料'],
       cta='留言 +1，我私訊你')

# ── 2. 規則講清楚：降低報名者的心理成本 ──
social('02_十四天怎麼算.png', False, '封測是怎麼運作的',
       ['不是要你', '每天打開'],
       ['Google 規定要 12 人連續 14 天保持加入，', '我才能申請上架。'],
       RECAP, phone_w=560, phone_y=900,
       bullets=['14 天內不退出測試就好', '需要 Gmail 才能加入名單', '想退出隨時可以退'],
       cta='留言 +1')

# ── 3. 產品在算什麼 ──
social('03_時間花在誰身上.png', True, '這個 App 在算什麼',
       ['你每天把時間', '花在誰身上？'],
       ['訊息、聊天室停留時間、通話，', '七個 App 合成同一張排行榜。'],
       OVERVIEW, phone_y=640)

# ── 4. 為什麼需要你：數字全部出自 docs/CHANGELOG.md ──
social('04_改了48版.png', False, '為什麼需要 20 個人',
       ['我改了 48 版', '才敢拿出來'],
       ['下面這三個都是使用者跟我說的，', '我自己一個都沒發現。'],
       INSIGHT, phone_w=560, phone_y=900,
       bullets=['未接來電看不出是誰打的', '還原備份只剩一筆', '深色模式按鈕跟背景一樣暗'],
       cta='留言 +1')

# ── 5. 誘因寫身分，不寫現金 ──
social('05_首批測試者.png', False, '給首批測試者',
       ['名字會留在', '第一版的致謝'],
       ['正式版終身無廣告 ——', '沒有網路權限，想放廣告也放不了。'],
       LEADER, phone_w=560, phone_y=900,
       bullets=['創始測試者致謝名單', '功能許願直接寄到我信箱'],
       cta='留言 +1，我私訊你')

print('OUT =', OUT)
