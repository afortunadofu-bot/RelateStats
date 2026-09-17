# -*- coding: utf-8 -*-
"""RelateStats 上架與宣傳素材產生器（來源：截圖模式拍的實機畫面，名字編號已去除）"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

SRC = r'C:\Users\user\Downloads\play商店\relatestat\匿名版本_去編號'
OUT = r'C:\Users\user\Downloads\play商店\relatestat\上架素材'
D_RAW = os.path.join(OUT, '00_裁切後截圖')
D_STORE = os.path.join(OUT, '01_商店截圖_1080x1920')
D_FEAT = os.path.join(OUT, '02_主題圖片_1024x500')
D_ICON = os.path.join(OUT, '03_圖示_512x512')
D_SOC = os.path.join(OUT, '04_社群宣傳_1080x1350')
for d in (D_RAW, D_STORE, D_FEAT, D_ICON, D_SOC):
    os.makedirs(d, exist_ok=True)

INK = (15, 23, 42)        # #0F172A 品牌深色（與 App logo 相同）
BLUE = (37, 99, 235)      # #2563EB 品牌藍
SLATE = (71, 85, 105)     # #475569
MUTED = (148, 163, 184)   # #94A3B8
BG_TOP = (244, 247, 252)
BG_BOT = (228, 235, 247)

NOTO = r'C:\Windows\Fonts\NotoSansTC-VF.ttf'
MSJH = r'C:\Windows\Fonts\msjh.ttc'
MSJHB = r'C:\Windows\Fonts\msjhbd.ttc'


def font(size, weight='Bold'):
    try:
        f = ImageFont.truetype(NOTO, size)
        f.set_variation_by_name(weight)
        return f
    except Exception:
        return ImageFont.truetype(MSJHB if weight in ('Bold', 'Black') else MSJH, size)


# ── 1. 裁切：去掉狀態列（通知圖示）與系統導覽列，長寬比符合 Play（長邊 ≤ 短邊 2 倍） ──
TOP, BOTTOM = 100, 2200
shots = {}
for fn in sorted(os.listdir(SRC)):
    if fn.endswith('.png') and not fn.startswith('_'):
        im = Image.open(os.path.join(SRC, fn)).convert('RGB').crop((0, TOP, 1080, BOTTOM))
        im.save(os.path.join(D_RAW, fn))
        shots[fn[:-4]] = im
print('cropped', len(shots), shots['306148'].size)

OVERVIEW, OVERVIEW_TAGS, LEADER, INSIGHT, CONTACTS, SETTINGS, RECAP = (
    '306148', '306149', '306150', '306151', '306152', '306153', '306154')


def gradient(w, h, top, bot):
    g = Image.new('RGB', (w, h), top)
    dr = ImageDraw.Draw(g)
    for y in range(h):
        t = y / max(1, h - 1)
        dr.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)))
    return g


def phone(shot, width, radius=None, bezel=None):
    """截圖包成手機外框（圓角＋深色邊框），回傳 RGBA"""
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


def logo(size, full_bleed=False):
    """App 記號：深色圓角方塊＋三個遞增、彼此交疊的圓（比例同 ic_launcher_foreground）"""
    im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    dr = ImageDraw.Draw(im)
    if full_bleed:
        dr.rectangle([0, 0, size, size], fill=INK + (255,))
    else:
        dr.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.26), fill=INK + (255,))
    cy = size * 0.5
    for cx, rr, al in [(0.278, 0.083, 0.55), (0.444, 0.120, 0.85), (0.648, 0.167, -1)]:
        r = size * rr
        col = BLUE + (255,) if al < 0 else (255, 255, 255, int(255 * al))
        layer = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse([size * cx - r, cy - r, size * cx + r, cy + r], fill=col)
        im.alpha_composite(layer)
    return im


# ── 2. 商店截圖 1080×1920 ──
STORE = [
    (OVERVIEW, '一週總覽', ['看見你把時間', '花在誰身上'], '訊息、停留時間、最常聊的人，一眼看完'),
    (RECAP, '每日報告', ['每晚一則', '今天跟昨天比'], '聊了誰、聊多少、幾點最熱絡'),
    (LEADER, '排行', ['誰是你', '最常聊的人'], '依訊息或停留時間排名，還能按標籤篩選'),
    (OVERVIEW_TAGS, '關係分布', ['伴侶、家人、朋友', '各佔多少'], '幫聯絡人貼上標籤，看見時間的去向'),
    (CONTACTS, '聯絡人', ['同一個人', '不同 App 合成一個'], '合併帳號、自訂暱稱、標籤人數一目了然'),
    (INSIGHT, '趨勢分析', ['這週比上週', '多聊了嗎'], '逐日趨勢、與上期比較、每日平均'),
    (SETTINGS, '隱私', ['資料只存在', '你的手機'], 'App 沒有網路權限，開權限前先說清楚'),
]
f_eye, f_head, f_sub = font(34, 'Bold'), font(84, 'Black'), font(40, 'Medium')
for i, (key, eye, head, sub) in enumerate(STORE, 1):
    W, H = 1080, 1920
    cv = gradient(W, H, BG_TOP, BG_BOT)
    dr = ImageDraw.Draw(cv)
    x = 96
    # 眉標：小藍點＋文字
    dr.ellipse([x, 138, x + 16, 154], fill=BLUE)
    dr.text((x + 30, 124), eye, font=f_eye, fill=BLUE)
    y = text_block(dr, x, 190, head, f_head, INK, 14)
    dr.text((x, y + 18), sub, font=f_sub, fill=SLATE)
    ph = phone(shots[key], 800)
    cv = paste_with_shadow(cv, ph, (W - ph.width) // 2, 560)
    cv.save(os.path.join(D_STORE, '%02d_%s.png' % (i, eye)))
print('store done')

# ── 3. 主題圖片 1024×500 ──
W, H = 1024, 500
cv = gradient(W, H, (15, 23, 42), (23, 37, 84))
glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
ImageDraw.Draw(glow).ellipse([560, -120, 1160, 480], fill=BLUE + (90,))
glow = glow.filter(ImageFilter.GaussianBlur(90))
cv = cv.convert('RGBA'); cv.alpha_composite(glow); cv = cv.convert('RGB')
lg = logo(84)
cv.paste(lg, (64, 120), lg)
dr = ImageDraw.Draw(cv)
dr.text((166, 126), 'RelateStats', font=font(60, 'Black'), fill=(255, 255, 255))
dr.text((64, 238), '看見你把時間', font=font(46, 'Black'), fill=(255, 255, 255))
dr.text((64, 296), '花在誰身上', font=font(46, 'Black'), fill=(255, 255, 255))
dr.text((64, 382), '訊息 ・ 停留時間 ・ 每日報告　資料只存在手機', font=font(22, 'Medium'), fill=MUTED)
p1 = phone(shots[RECAP], 230)
p2 = phone(shots[OVERVIEW], 250)
cv = paste_with_shadow(cv, p1, 612, 96, blur=18, offset=10, alpha=120)
cv = paste_with_shadow(cv, p2, 770, 48, blur=18, offset=10, alpha=120)
cv.save(os.path.join(D_FEAT, 'feature_graphic.png'))
print('feature done')

# ── 4. 圖示 512×512（Play 會自己套圓角，所以滿版） ──
logo(512, full_bleed=True).convert('RGB').save(os.path.join(D_ICON, 'icon_512.png'))
print('icon done')

# ── 5. 社群宣傳 1080×1350 ──
def social(name, dark, eye, head, sub, key, phone_w=720, phone_y=610, bullets=None, cta=None):
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
    y = text_block(dr, 84, 226, head, font(76, 'Black'), c_head, 10)
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
        cv = paste_with_shadow(cv, ph, (W - ph.width) // 2, phone_y,
                               alpha=140 if dark else 70)
    cv.save(os.path.join(D_SOC, name))


social('01_你把時間花在誰身上.png', True, '新 App 介紹',
       ['你每天把時間', '花在誰身上？'], ['RelateStats 把聊天變成看得懂的圖表'], OVERVIEW, phone_y=600)
social('02_每日報告.png', False, '每日報告',
       ['每晚一則', '今天跟昨天比'], ['聊了誰、聊多少、幾點最熱絡'], RECAP, phone_y=560)
social('03_最常聊的人.png', False, '排行',
       ['誰才是你', '最常聊的人？'], ['依訊息或停留時間排名，還能按標籤篩選'], LEADER, phone_y=560)
social('04_隱私.png', True, '隱私是第一優先',
       ['資料只存在', '你的手機'], [],
       SETTINGS, phone_w=640, phone_y=790, bullets=['App 沒有網路權限', '訊息只算詞頻，不存原文', '開權限前先說清楚', '雲端備份存在你自己的硬碟'])
social('05_招募測試.png', False, '搶先測試招募中',
       ['找 12 位 Android 朋友', '一起測 14 天'], ['上架 Google Play 前的封閉測試', '裝好每天打開看一下就好'],
       RECAP, phone_w=600, phone_y=860, bullets=['免費、不用註冊帳號', '資料只在你自己的手機'], cta='私訊我加入測試')
print('social done')
