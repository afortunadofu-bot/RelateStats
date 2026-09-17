# -*- coding: utf-8 -*-
"""把截圖模式名字後面的編號（「明哲 8」的 8）用底色蓋掉。座標來自 Windows OCR（原圖 1080×2340）"""
import os
import shutil
from PIL import Image, ImageDraw

SRC = r'C:\Users\user\Downloads\play商店\relatestat\匿名版本'
DST = r'C:\Users\user\Downloads\play商店\relatestat\匿名版本_去編號'
os.makedirs(DST, exist_ok=True)

NAV_TOP = 1975  # 底部導覽列上緣之上

# OCR 抓到的編號框 (x, y, w, h)
BOXES = {
    '306148.png': [(182, 1140, 27, 39), (699, 1140, 27, 39)],                      # 概況：訊息最多／停留最久
    '306149.png': [(244, 1565, 20, 31), (252, 1763, 45, 31), (283, 1864, 22, 30)],  # 概況 Top 5
    '306150.png': [(335, 966, 44, 31), (320, 1166, 45, 31), (328, 1370, 20, 30),
                   (463, 1570, 22, 30), (345, 1766, 20, 31)],                       # 排行
    '306152.png': [(175, 1031, 20, 31), (215, 1259, 22, 30), (300, 1487, 20, 31),
                   (175, 1715, 20, 30), (182, 1939, 44, 31)],                       # 聯絡人
}
# OCR 沒抓到的列（被導覽列切一半、或辨識漏掉）：在這個範圍裡找「名字後面最後一組字」
SCANS = {
    '306149.png': [(1648, 1700, 140, 470), (1948, NAV_TOP, 140, 470)],
    '306150.png': [(1948, NAV_TOP, 225, 700)],
}
DARK = 120
GAP = 9


def bg_at(px, x, y):
    return px[x, y]


def scan(px, y0, y1, x0, x1):
    cols = [any(sum(px[x, y]) / 3 < DARK for y in range(y0, y1)) for x in range(x0, x1)]
    words, start, last = [], None, None
    for i, d in enumerate(cols):
        if d:
            if start is None:
                start = i
            elif i - last > GAP:
                words.append((start, last)); start = i
            last = i
    if start is not None:
        words.append((start, last))
    return [(x0 + a, x0 + b) for a, b in words]


for fn in sorted(os.listdir(SRC)):
    if not fn.endswith('.png'):
        continue
    if fn not in BOXES and fn not in SCANS:
        shutil.copy2(os.path.join(SRC, fn), os.path.join(DST, fn))
        continue
    im = Image.open(os.path.join(SRC, fn)).convert('RGB')
    px = im.load()
    rects = []
    for (x, y, w, h) in BOXES.get(fn, []):
        rects.append((x, y, x + w, y + h))
    for (y0, y1, x0, x1) in SCANS.get(fn, []):
        words = scan(px, y0, y1, x0, x1)
        print(fn, 'scan', (y0, y1), words)
        if len(words) >= 2 and words[-1][1] - words[-1][0] < 50:
            a, b = words[-1]
            # 「10」的「1」很窄，跟「0」之間的空隙會被當成另一段 —— 往前吃掉窄的數字段
            i = len(words) - 2
            while i >= 1 and words[i][1] - words[i][0] < 18 and a - words[i][1] < 20:
                a = words[i][0]
                i -= 1
            if y1 == NAV_TOP:
                # 被導覽列切一半的列：直接從上方空白一路蓋到分隔線的上一格
                div = next(y for y in range(1960, 2000) if sum(px[a - 10, y]) < 740)
                rects.append((a, y0 + 2, b, div - 1))
            else:
                ys = [y for y in range(y0, y1) if any(sum(px[x, y]) / 3 < DARK + 40 for x in range(a, b + 1))]
                rects.append((a, min(ys), b, max(ys)))
    draw = ImageDraw.Draw(im)
    for (a, t, b, btm) in rects:
        bg = bg_at(px, a - 7, (t + btm) // 2)
        bottom = btm if btm > 1950 else btm + 3
        draw.rectangle([a - 3, t - 6, b + 3, bottom], fill=bg)
        print(fn, 'mask', (a, t, b, btm), bg)
    im.save(os.path.join(DST, fn))
    # 預覽用縮圖
    im.resize((540, 1170)).save(os.path.join(DST, '_preview_' + fn))
print('DONE')
