# Usage: python3 docs/make_previews.py docs/screenshots   (needs Pillow and the Inter font)
"""Draws preview images of Arrmada360's screens (layout + colours match the app's Compose code).
These are illustrations, not real captures – replace with phone screenshots using the same file names."""
import math
import sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = sys.argv[1]
W, H = 720, 1480
F = "/usr/share/fonts/opentype/inter/Inter-"
def font(w, s): return ImageFont.truetype(F + w + ".otf", s)

BG = (14, 17, 23); SURF = (23, 27, 36); HIGH = (34, 40, 55); ON = (237, 239, 243); MUTED = (161, 171, 186)
ACC = (53, 197, 244); ACC2 = (168, 85, 247); PINK = (244, 114, 182); GOOD = (74, 222, 128); WARN = (251, 191, 36); BAD = (248, 113, 113)
RAINBOW = [ACC, ACC2, PINK, WARN, GOOD, (249, 115, 22)]
APPS = {
    "Sonarr": ("So", (53, 197, 244)), "Radarr": ("Ra", (255, 194, 48)), "Lidarr": ("Li", (34, 197, 94)), "Readarr": ("Re", (224, 70, 75)),
    "Prowlarr": ("Pr", (249, 115, 22)), "Jackett": ("Ja", (148, 163, 184)), "qBittorrent": ("qB", (59, 130, 246)), "Transmission": ("Tr", (220, 38, 38)),
    "Deluge": ("De", (91, 141, 239)), "ruTorrent": ("rT", (132, 204, 22)), "SABnzbd": ("SB", (234, 179, 8)), "NZBGet": ("NG", (16, 185, 129)),
    "Overseerr": ("Se", (129, 140, 248)), "Tautulli": ("Ta", (229, 160, 13)), "Unraid": ("Un", (241, 90, 44)),
}

def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def canvas(tint, tint2=None):
    tint2 = tint2 or ACC2
    im = Image.new("RGB", (W, H), BG)
    glow = Image.new("RGB", (W, H), BG)
    g = ImageDraw.Draw(glow)
    for (cx, cy, r, c, a) in [(W * .2, 0, 620, tint, .32), (W * .95, H * .22, 520, tint2, .22), (W * .3, H * .85, 600, tint, .10)]:
        for i in range(30, 0, -1):
            rr = r * i / 30
            g.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=mix(BG, c, a * (1 - i / 30) ** .8))
    im = Image.blend(im, glow.filter(ImageFilter.GaussianBlur(40)), 1.0)
    return im, ImageDraw.Draw(im)

def rr(d, box, r, fill=None, outline=None, w=1): d.rounded_rectangle(box, r, fill=fill, outline=outline, width=w)

def grad_rect(im, box, c1, c2, r=24, horizontal=True, alpha1=1.0, alpha2=1.0):
    x0, y0, x1, y1 = [int(v) for v in box]
    w, h = x1 - x0, y1 - y0
    g = Image.new("RGB", (w, h))
    gd = ImageDraw.Draw(g)
    for i in range(w if horizontal else h):
        t = i / max(1, (w if horizontal else h) - 1)
        c = mix(c1, c2, t)
        if horizontal: gd.line([(i, 0), (i, h)], fill=c)
        else: gd.line([(0, i), (w, i)], fill=c)
    m = Image.new("L", (w, h), 0); ImageDraw.Draw(m).rounded_rectangle([0, 0, w - 1, h - 1], r, fill=255)
    im.paste(g, (x0, y0), m)

def panel(im, d, box, tint=ACC, r=26):
    grad_rect(im, box, mix(SURF, tint, .16), mix(SURF, BG, .1), r)
    rr(d, box, r, outline=mix(SURF, tint, .45), w=2)

def badge(d, x, y, name, s=56):
    sh, c = APPS[name]
    rr(d, [x, y, x + s, y + s], int(s * .28), fill=mix(BG, c, .30), outline=mix(BG, c, .6), w=2)
    f = font("Bold", int(s * .38))
    tw = d.textlength(sh, font=f)
    d.text((x + (s - tw) / 2, y + s * .26), sh, font=f, fill=c)

def text(d, xy, s, size=24, w="Regular", fill=ON): d.text(xy, s, font=font(w, size), fill=fill)

def pill(d, x, y, s, c, size=18, solid=False):
    f = font("SemiBold", size); tw = d.textlength(s, font=f)
    rr(d, [x, y, x + tw + 22, y + size + 12], 50, fill=c if solid else mix(BG, c, .25))
    d.text((x + 11, y + 5), s, font=f, fill=(11, 13, 16) if solid else c)
    return x + tw + 30

def bar(d, x, y, w, p, c, h=10):
    rr(d, [x, y, x + w, y + h], h // 2, fill=HIGH)
    if p > 0: rr(d, [x, y, x + max(h, w * p), y + h], h // 2, fill=c)

def icon_menu(d, x, y):
    for i in range(3): rr(d, [x, y + i * 12, x + 32, y + i * 12 + 4], 2, fill=ON)

def icon_search(d, x, y, c=MUTED):
    d.ellipse([x, y, x + 24, y + 24], outline=c, width=4); d.line([(x + 21, y + 21), (x + 32, y + 32)], fill=c, width=5)

def icon_down(d, x, y, c=GOOD):
    d.line([(x + 14, y), (x + 14, y + 26)], fill=c, width=5); d.polygon([(x, y + 16), (x + 28, y + 16), (x + 14, y + 32)], fill=c)
    d.line([(x, y + 38), (x + 28, y + 38)], fill=c, width=4)

def icon_restart(d, x, y, c=ON):
    d.arc([x, y, x + 30, y + 30], 40, 330, fill=c, width=4); d.polygon([(x + 22, y - 2), (x + 32, y + 8), (x + 20, y + 12)], fill=c)

def topbar(d, title, sub=None, app=None, menu=True):
    if menu: icon_menu(d, 30, 76)
    else: text(d, (30, 66), "←", 40, "Bold")
    x = 92
    if app: badge(d, x, 60, app, 52); x += 68
    text(d, (x, 60 if sub else 70), title, 34, "Bold")
    if sub: text(d, (x, 104), sub, 20, "Medium", ACC if not app else APPS[app][1])
    icon_search(d, W - 80, 74)

def bottom(im, d, labels, sel, c):
    y = H - 140
    d.rectangle([0, y, W, H], fill=mix(SURF, BG, .1))
    d.line([(0, y), (W, y)], fill=mix(BG, c, .6), width=2)
    n = len(labels); cw = W / n
    for i, l in enumerate(labels):
        cx = cw * i + cw / 2
        if i == sel: rr(d, [cx - 46, y + 22, cx + 46, y + 70], 24, fill=c)
        d.ellipse([cx - 13, y + 33, cx + 13, y + 59], outline=(11, 13, 16) if i == sel else MUTED, width=3)
        f = font("Bold" if i == sel else "Regular", 19); tw = d.textlength(l, font=f)
        d.text((cx - tw / 2, y + 82), l, font=f, fill=c if i == sel else MUTED)

def poster(im, d, box, c1, c2, title=None, r=18):
    grad_rect(im, box, c1, c2, r, horizontal=False)
    if title:
        x0, y0, x1, y1 = box
        f = font("Bold", 17)
        words = title.split(); lines = []; cur = ""
        for wd in words:
            if d.textlength(cur + " " + wd, font=f) < (x1 - x0 - 16): cur = (cur + " " + wd).strip()
            else: lines.append(cur); cur = wd
        lines.append(cur)
        for i, l in enumerate(lines[:3]): d.text((x0 + 9, y1 - 26 - 22 * (len(lines[:3]) - 1 - i)), l, font=f, fill=(255, 255, 255))

def chip_row(d, x, y, labels, sel, counts=None):
    for i, l in enumerate(labels):
        c = ACC if i == 0 else RAINBOW[(i - 1) % 6]
        s = l + (f"  {counts[i]}" if counts else "")
        f = font("SemiBold", 19); tw = d.textlength(s, font=f)
        rr(d, [x, y, x + tw + 30, y + 42], 50, fill=c if i == sel else mix(BG, c, .16), outline=mix(BG, c, .55), w=2)
        d.text((x + 15, y + 10), s, font=f, fill=(11, 13, 16) if i == sel else c)
        x += tw + 42
        if x > W - 40: break

def save(im, name):
    im.save(f"{OUT}/{name}", optimize=True)
    print("wrote", name)

# ---------------------------------------------------------------- 1. onboarding picker
def onboarding():
    im, d = canvas(ACC)
    text(d, (30, 66), "←", 40, "Bold")
    text(d, (92, 58), "Which apps do you use?", 32, "Bold")
    text(d, (92, 102), "Pick everything you run. You can add more later.", 19, "Medium", ACC)
    groups = [("MEDIA MANAGERS", ["Sonarr", "Radarr", "Lidarr", "Readarr"]), ("INDEXERS", ["Prowlarr", "Jackett"]),
              ("DOWNLOAD CLIENTS", ["qBittorrent", "Transmission", "Deluge", "ruTorrent", "SABnzbd", "NZBGet"]),
              ("REQUESTS & MONITORING", ["Overseerr", "Tautulli"]), ("SERVER", ["Unraid"])]
    blurb = {"Sonarr": "TV shows", "Radarr": "Movies", "Lidarr": "Music", "Readarr": "Books", "Prowlarr": "Indexer manager", "Jackett": "Torrent search",
             "qBittorrent": "Torrent client", "Transmission": "Torrent client", "Deluge": "Torrent client", "ruTorrent": "rTorrent web", "SABnzbd": "Usenet client",
             "NZBGet": "Usenet client", "Overseerr": "Media requests", "Tautulli": "Plex stats", "Unraid": "Array & Docker"}
    chosen = {"Sonarr", "Radarr", "Prowlarr", "qBittorrent", "Overseerr", "Tautulli", "Unraid", "SABnzbd"}
    y = 160
    for g, apps in groups:
        text(d, (34, y), g, 17, "Bold", MUTED); y += 34
        for i, a in enumerate(apps):
            col = i % 2; x = 24 + col * 340
            if col == 0 and i > 0: y += 102
            c = APPS[a][1]; on = a in chosen
            grad_rect(im, [x, y, x + 326, y + 90], mix(SURF, c, .34 if on else .08), mix(SURF, c, .12 if on else .0), 22)
            rr(d, [x, y, x + 326, y + 90], 22, outline=c if on else mix(SURF, c, .3), w=3 if on else 1)
            badge(d, x + 16, y + 17, a, 56)
            text(d, (x + 86, y + 20), a, 22, "Bold"); text(d, (x + 86, y + 50), blurb[a], 17, "Regular", MUTED)
            if on: d.ellipse([x + 290, y + 10, x + 314, y + 34], fill=c)
        y += 116
    grad_rect(im, [30, H - 120, W - 30, H - 44], ACC, ACC2, 22)
    f = font("Bold", 24); s = "Continue with 8 apps"; d.text(((W - d.textlength(s, font=f)) / 2, H - 96), s, font=f, fill=(11, 13, 16))
    save(im, "01-onboarding.png")

# ---------------------------------------------------------------- 2. Movies discovery
def movies():
    im, d = canvas(APPS["Radarr"][1], (249, 115, 22))
    topbar(d, "Movies", "Radarr")
    def header(y, title, c, src):
        rr(d, [26, y, 64, y + 38], 10, fill=mix(BG, c, .55))
        text(d, (78, y + 2), title, 26, "ExtraBold")
        pill(d, W - 120, y + 4, src, c)
    header(150, "Trending movies", ACC, "Trakt")
    cols = [((90, 40, 160), (20, 20, 60)), ((200, 80, 40), (40, 10, 10)), ((30, 120, 140), (8, 30, 40))]
    for i, t in enumerate(["The Silent Orbit", "Paper Harbour"]):
        x = 24 + i * 410
        grad_rect(im, [x, 206, x + 390, 426], cols[i][0], cols[i][1], 26, horizontal=False)
        text(d, (x + 18, 360), t, 28, "ExtraBold", (255, 255, 255))
        pill(d, x + 18, 394, "★ 7.9", WARN); pill(d, x + 110, 394, "1.2k watching", ACC)
    rows = [("Recently downloaded", GOOD, "Radarr", ["Northline", "Glass Season", "Low Tide", "The Ninth Key", "Ember"]),
            ("Recommended for you", ACC2, "TMDB", ["Quiet Engines", "Foxglove", "Second Sun", "Driftwood", "Atlas"]),
            ("Upcoming movies", WARN, "Seerr", ["Red Meridian", "Hollow Bay", "Neon Saints", "Overland", "Kite"])]
    y = 460
    pal = [((70, 90, 170), (20, 20, 40)), ((170, 60, 90), (30, 10, 20)), ((40, 140, 110), (10, 30, 25)), ((180, 130, 40), (40, 25, 5)), ((90, 60, 160), (20, 10, 40))]
    for r, (title, c, src, names) in enumerate(rows):
        header(y, title, c, src)
        for i, n in enumerate(names):
            x = 24 + i * 150
            if x > W: break
            poster(im, d, [x, y + 56, x + 136, y + 260], *pal[(i + r) % 5])
            rr(d, [x, y + 56, x + 136, y + 260], 18, outline=mix(BG, c, .5), w=2)
            if r == 0: pill(d, x + 6, y + 64, "✓", GOOD, 15, True)
            else: pill(d, x + 6, y + 64, f"★ {6.8 + i * .3:.1f}", WARN, 15, True)
            if r == 2: text(d, (x + 8, y + 232), ["Tomorrow", "Fri 27 Nov", "Mon 30 Nov", "3 Dec", "9 Dec"][i], 15, "Bold", (255, 255, 255))
            text(d, (x, y + 268), n, 18, "Bold")
        y += 320
    bottom(im, d, ["Movies", "TV Shows", "Calendar", "Server"], 0, APPS["Radarr"][1])
    save(im, "02-movies.png")

# ---------------------------------------------------------------- 3. Server tab
def server():
    im, d = canvas(ACC)
    topbar(d, "Server", "Good evening")
    rr(d, [40, 150, 112, 156], 3, fill=GOOD); text(d, (40, 166), "Server Info", 40, "Bold")
    panel(im, d, [20, 236, W - 20, 600], GOOD)
    text(d, (110, 262), "Server", 20, "SemiBold", GOOD); text(d, (110, 288), "Disk Space", 32, "Bold")
    rr(d, [44, 270, 84, 304], 6, outline=ON, w=3)
    for i, (p, used, free) in enumerate([("/", .17, "84.2 GB free"), ("/config", .52, "224.6 GB free"), ("/downloads", .26, "23.4 TB free"), ("/media", .25, "23.2 TB free")]):
        y = 350 + i * 62
        text(d, (44, y), p, 22, "Bold", MUTED); bar(d, 44, y + 34, 450, used, GOOD, 9); text(d, (520, y + 24), free, 20, "SemiBold", MUTED)
    panel(im, d, [20, 620, W - 20, 980], WARN)
    text(d, (110, 646), "Download History", 20, "SemiBold", WARN); text(d, (110, 672), "412.6 GB in last 2 weeks", 30, "Bold")
    vals = [8, 22, 14, 0, 31, 40, 12, 18, 26, 9, 34, 21, 15, 28]
    cx0, cy0, cw, ch = 130, 740, 540, 200
    for i in range(5):
        text(d, (40, cy0 - 8 + i * ch / 4), f"{40 - i * 10} GB", 16, "Medium", MUTED)
        d.line([(cx0 + cw * i / 4, cy0), (cx0 + cw * i / 4, cy0 + ch)], fill=(58, 66, 82), width=2)
    d.line([(cx0, cy0 + ch), (cx0 + cw, cy0 + ch)], fill=(58, 66, 82), width=2)
    bw = cw / len(vals)
    for i, v in enumerate(vals):
        if v: grad_rect(im, [cx0 + i * bw + 7, cy0 + ch - ch * v / 40, cx0 + (i + 1) * bw - 7, cy0 + ch], WARN, PINK, 6, horizontal=False)
    panel(im, d, [20, 1000, W - 20, 1130], BAD)
    d.ellipse([44, 1040, 84, 1080], outline=ON, width=3); text(d, (60, 1044), "i", 26, "Bold")
    text(d, (110, 1024), "From the *arrs", 20, "SemiBold", BAD); text(d, (110, 1052), "No Server Issues", 32, "Bold")
    rr(d, [40, 1160, 112, 1166], 3, fill=WARN); text(d, (40, 1176), "External Server Use", 40, "Bold")
    panel(im, d, [20, 1240, W - 20, 1330], APPS["Tautulli"][1]); badge(d, 40, 1262, "Tautulli", 46)
    text(d, (106, 1250), "From Tautulli", 18, "SemiBold", APPS["Tautulli"][1]); text(d, (106, 1276), "2 Active Streams", 28, "Bold")
    bottom(im, d, ["Movies", "TV Shows", "Calendar", "Server"], 3, ACC)
    save(im, "03-server.png")

# ---------------------------------------------------------------- 4. Sonarr library
def sonarr():
    c = APPS["Sonarr"][1]
    im, d = canvas(c, (37, 99, 235))
    topbar(d, "Sonarr", "Library", "Sonarr")
    x = 24
    for i, t in enumerate(["Series", "Calendar", "Missing"]):
        f = font("Bold" if i == 0 else "Medium", 20); tw = d.textlength(t, font=f)
        if i == 0: grad_rect(im, [x, 142, x + tw + 36, 184], c, (37, 99, 235), 21)
        else: rr(d, [x, 142, x + tw + 36, 184], 21, fill=HIGH)
        d.text((x + 18, 151), t, font=f, fill=(11, 13, 16) if i == 0 else MUTED); x += tw + 50
    for i, (lab, val, col) in enumerate([("Series", "184", c), ("Missing", "37", BAD), ("On disk", "18.4 TB", (37, 99, 235))]):
        x0 = 24 + i * 228
        grad_rect(im, [x0, 204, x0 + 216, 284], mix(BG, col, .3), mix(BG, col, .08), 18); rr(d, [x0, 204, x0 + 216, 284], 18, outline=mix(BG, col, .5), w=2)
        text(d, (x0 + 16, 214), val, 28, "Bold", col); text(d, (x0 + 16, 252), lab, 17, "Medium", MUTED)
    chip_row(d, 24, 304, ["All", "Monitored", "Missing", "Complete"], 0, [184, 171, 37, 133])
    names = ["Harbor Lights", "Iron Valley", "The Long Night", "Orbiters", "Paper Crowns", "Saltwater", "Copperline", "Night Shift", "Wildwood"]
    pal = [((40, 110, 170), (10, 25, 40)), ((150, 60, 60), (30, 10, 10)), ((60, 140, 90), (10, 30, 20)), ((120, 70, 170), (25, 10, 40)), ((170, 120, 40), (35, 25, 5))]
    for i, n in enumerate(names):
        col, row = i % 3, i // 3
        x0, y0 = 24 + col * 228, 372 + row * 340
        poster(im, d, [x0, y0, x0 + 212, y0 + 290], *pal[i % 5])
        prog = [1, .8, .4, 1, .65, 1, .2, .9, 1][i]
        colr = GOOD if prog == 1 else (c if i % 2 else BAD)
        rr(d, [x0, y0, x0 + 212, y0 + 290], 18, outline=mix(BG, colr, .6), w=2)
        text(d, (x0 + 10, y0 + 250), f"{int(prog * 48)}/48", 16, "Medium", (255, 255, 255)); bar(d, x0 + 10, y0 + 274, 192, prog, colr, 6)
        text(d, (x0, y0 + 298), n, 19, "Bold")
    grad_rect(im, [W - 120, H - 270, W - 30, H - 180], c, c, 45)
    text(d, (W - 90, H - 256), "+", 48, "Bold", (11, 13, 16))
    bottom(im, d, ["Library", "Queue", "Add", "History", "Server"], 0, c)
    save(im, "04-sonarr.png")

# ---------------------------------------------------------------- 5. qBittorrent
def qbit():
    c = APPS["qBittorrent"][1]
    im, d = canvas(c, (6, 182, 212))
    topbar(d, "qBittorrent", "↓ 38.4 MB/s   ↑ 4.1 MB/s   · 2.1 TB free", "qBittorrent")
    panel(im, d, [20, 150, W - 20, 330], c)
    pts = [5, 9, 7, 14, 12, 20, 18, 27, 24, 31, 29, 36, 33, 38, 35, 40]
    poly = [(20 + i * (W - 40) / (len(pts) - 1), 330 - p * 3.6) for i, p in enumerate(pts)]
    d.line(poly, fill=c, width=4)
    text(d, (44, 170), "↓ 38.4 MB/s", 38, "Bold", c); text(d, (44, 220), "Download", 17, "Medium", MUTED)
    text(d, (470, 176), "↑ 4.1 MB/s", 28, "Bold", GOOD)
    chip_row(d, 24, 350, ["All", "Downloading", "Seeding", "Completed", "Paused"], 1, [42, 6, 30, 31, 3])
    chip_row(d, 24, 404, ["Any category", "tv-sonarr", "radarr", "manual"], 0, [42, 18, 16, 8])
    rows = [("Harbor.Lights.S02E07.1080p.WEB.h264", .82, "Downloading", c, "12.4 MB/s", "1.4 GB", "tv-sonarr", True),
            ("Paper.Harbour.2026.2160p.UHD.BluRay", .41, "Downloading", c, "21.9 MB/s", "58.2 GB", "radarr", True),
            ("Iron.Valley.S01.COMPLETE.720p", .97, "Downloading", c, "4.1 MB/s", "9.8 GB", "tv-sonarr", False),
            ("Lowtide.OST.FLAC", .12, "Stalled", WARN, "0 B/s", "620 MB", "manual", False),
            ("Northline.2025.1080p.WEB-DL", 1.0, "Seeding", GOOD, "↑ 1.2 MB/s", "6.1 GB", "radarr", False)]
    y = 466
    for n, p, st, col, sp, sz, cat, sel in rows:
        box = [20, y, W - 20, y + 150]
        grad_rect(im, box, mix(SURF, col, .30 if sel else .16), SURF, 22)
        rr(d, box, 22, outline=mix(SURF, col, .5), w=2)
        text(d, (40, y + 16), n[:30] + ("…" if len(n) > 30 else ""), 21, "Bold")
        text(d, (W - 110, y + 16), f"{int(p * 100)}%", 21, "Bold", col)
        bar(d, 40, y + 56, W - 140, p, col, 11)
        nx = pill(d, 40, y + 80, st, col); text(d, (nx, y + 84), sz, 17, "Regular", MUTED)
        text(d, (40, y + 116), sp, 17, "Medium", col if sp[0] != "↑" else GOOD); pill(d, 180, y + 112, cat, MUTED, 15)
        if sel: d.ellipse([W - 76, y + 60, W - 40, y + 96], fill=c)
        else: d.ellipse([W - 76, y + 60, W - 40, y + 96], outline=MUTED, width=3)
        y += 166
    bottom(im, d, ["Torrents", "RSS", "Server"], 0, c)
    save(im, "05-qbittorrent.png")

# ---------------------------------------------------------------- 6. Unraid
def ring(im, d, cx, cy, r, v, c, label, txt):
    d.arc([cx - r, cy - r, cx + r, cy + r], 0, 360, fill=HIGH, width=16)
    d.arc([cx - r, cy - r, cx + r, cy + r], -90, -90 + 360 * v, fill=c, width=16)
    f = font("Bold", 30); tw = d.textlength(txt, font=f); d.text((cx - tw / 2, cy - 18), txt, font=f, fill=ON)
    f = font("Medium", 18); tw = d.textlength(label, font=f); d.text((cx - tw / 2, cy + r + 14), label, font=f, fill=MUTED)

def unraid():
    c = APPS["Unraid"][1]
    im, d = canvas(c, (225, 29, 72))
    topbar(d, "Unraid", "Dashboard", "Unraid")
    panel(im, d, [20, 150, W - 20, 560], c)
    d.ellipse([44, 182, 62, 200], fill=GOOD)
    text(d, (76, 170), "tower", 34, "Bold"); text(d, (76, 214), "Unraid 7.2.0 · Ryzen 9 5950X · 16C/32T", 18, "Regular", MUTED)
    ring(im, d, 150, 380, 78, .23, c, "CPU", "23%"); ring(im, d, 360, 380, 78, .61, ACC2, "RAM", "61%"); ring(im, d, 570, 380, 78, .72, GOOD, "Array", "72%")
    for i, (lab, val, col) in enumerate([("Array", "Started", GOOD), ("Docker", "31/34", ACC), ("VMs", "2/3", ACC2)]):
        x0 = 20 + i * 232
        grad_rect(im, [x0, 580, x0 + 220, 664], mix(BG, col, .3), mix(BG, col, .08), 18); rr(d, [x0, 580, x0 + 220, 664], 18, outline=mix(BG, col, .5), w=2)
        text(d, (x0 + 16, 590), val, 28, "Bold", col); text(d, (x0 + 16, 628), lab, 17, "Medium", MUTED)
    text(d, (40, 690), "DOCKER", 18, "Bold")
    conts = [("sonarr", True), ("radarr", True), ("qbittorrentvpn", True), ("prowlarr", True), ("plex", True), ("tautulli", False)]
    y = 730
    for n, run in conts:
        col = GOOD if run else BAD
        grad_rect(im, [20, y, W - 20, y + 84], mix(SURF, col, .16), SURF, 20); rr(d, [20, y, W - 20, y + 84], 20, outline=mix(SURF, col, .45), w=2)
        d.ellipse([42, y + 34, 58, y + 50], fill=col)
        text(d, (76, y + 14), n, 22, "Bold"); text(d, (76, y + 48), "Up 3 days" if run else "Exited (0) 2 hours ago", 16, "Regular", MUTED)
        if run:
            icon_restart(d, W - 150, y + 28)
            rr(d, [W - 86, y + 30, W - 60, y + 56], 4, fill=BAD)
        else:
            d.polygon([(W - 86, y + 26), (W - 86, y + 60), (W - 58, y + 43)], fill=GOOD)
        y += 96
    bottom(im, d, ["Dashboard", "Array", "Docker", "VMs", "Shares"], 0, c)
    save(im, "06-unraid.png")

# ---------------------------------------------------------------- 7. Settings
def settings():
    im, d = canvas(ACC)
    text(d, (30, 66), "←", 40, "Bold"); text(d, (92, 62), "General Settings", 36, "Bold")
    def group(y, title, rows):
        text(d, (40, y), title, 20, "Bold", MUTED); y += 40
        h = sum(118 if r[2] is not None else 108 for r in rows)
        rr(d, [20, y, W - 20, y + h], 30, fill=mix(HIGH, BG, .15))
        for i, (t, s, on) in enumerate(rows):
            text(d, (44, y + 20), t, 24, "Bold"); text(d, (44, y + 56), s, 18, "Regular", MUTED)
            if on is not None:
                rr(d, [W - 130, y + 32, W - 44, y + 78], 23, fill=GOOD if on else (70, 76, 90))
                cx = W - 67 if on else W - 107
                d.ellipse([cx - 17, y + 38, cx + 17, y + 72], fill=(17, 19, 24) if on else MUTED)
            else: text(d, (W - 70, y + 34), "›", 34, "Bold", GOOD)
            if i < len(rows) - 1: d.line([(44, y + (118 if on is not None else 108)), (W - 20, y + (118 if on is not None else 108))], fill=BG, width=2)
            y += 118 if on is not None else 108
        return y + 30
    y = group(150, "NAVIGATION", [("Volume rocker navigation", "Volume keys switch between your apps", True), ("Gesture navigation settings", "Side-menu swipe, swipe actions, back", None)])
    y = group(y, "INTERFACE", [("Friendly service names", "\"Movies, TV Shows\" instead of Radarr, Sonarr", False), ("Show speeds as bits", "500 Mbps instead of 62 MB/s", False),
                               ("Auto-clear searches", "Clear the filter after opening an item", True)])
    y = group(y, "GENERAL ACTIONS", [("Require authentication", "Fingerprint / PIN to open settings", False), ("Clear image cache", "Fix wrong posters or logos", None)])
    save(im, "07-settings.png")

# ---------------------------------------------------------------- 8. RSS
def rss():
    c = APPS["qBittorrent"][1]
    im, d = canvas(c, (6, 182, 212))
    topbar(d, "qBittorrent", "RSS", "qBittorrent")
    x = 24
    for i, t in enumerate(["Articles", "Feeds", "Rules"]):
        f = font("Bold" if i == 0 else "Medium", 20); tw = d.textlength(t, font=f)
        if i == 0: grad_rect(im, [x, 142, x + tw + 36, 184], c, (6, 182, 212), 21)
        else: rr(d, [x, 142, x + tw + 36, 184], 21, fill=HIGH)
        d.text((x + 18, 151), t, font=f, fill=(11, 13, 16) if i == 0 else MUTED); x += tw + 50
    rr(d, [24, 202, W - 24, 262], 16, outline=(58, 66, 82), w=2); icon_search(d, 46, 218); text(d, (92, 218), "Search articles", 21, "Regular", MUTED)
    chip_row(d, 24, 282, ["All feeds", "TorrentLeech", "IPTorrents", "Anime"], 0, [24, 11, 9, 4])
    arts = [("Harbor.Lights.S02E08.1080p.WEB.H264", "TorrentLeech", "Today, 16:42", 0, True), ("Iron.Valley.S01E11.2160p.WEB", "IPTorrents", "Today, 15:10", 1, True),
            ("Paper.Crowns.S03E01.720p.HDTV", "TorrentLeech", "Today, 12:03", 0, False), ("Frieren.S02E05.1080p.WEB", "Anime", "Yesterday, 22:18", 2, True),
            ("Saltwater.S01E04.1080p.WEB-DL", "IPTorrents", "Yesterday, 19:44", 1, False)]
    y = 346
    for t, feed, when, fi, unread in arts:
        col = RAINBOW[fi]
        grad_rect(im, [20, y, W - 20, y + 128], mix(SURF, col, .16), SURF, 22); rr(d, [20, y, W - 20, y + 128], 22, outline=mix(SURF, col, .45), w=2)
        if unread: d.ellipse([40, y + 26, 54, y + 40], fill=col)
        text(d, (66, y + 18), t[:32], 21, "Bold" if unread else "Regular", ON if unread else MUTED)
        nx = pill(d, 66, y + 64, feed, col); text(d, (nx, y + 68), when, 17, "Regular", MUTED)
        icon_down(d, W - 76, y + 42)
        y += 144
    bottom(im, d, ["Torrents", "RSS", "Server"], 1, c)
    save(im, "08-rss.png")

# ---------------------------------------------------------------- banner
def banner():
    BW, BH = 1600, 640
    im = Image.new("RGB", (BW, BH), BG)
    d = ImageDraw.Draw(im)
    grad_rect(im, [0, 0, BW, BH], mix(BG, ACC, .35), mix(BG, ACC2, .35), 0)
    over = Image.new("RGB", (BW, BH)); grad_rect(over, [0, 0, BW, BH], mix(BG, ACC2, .25), mix(BG, PINK, .28), 0)
    mask = Image.linear_gradient("L").resize((BW, BH))
    im = Image.composite(over, im, mask).filter(ImageFilter.GaussianBlur(20)); d = ImageDraw.Draw(im)
    text(d, (90, 150), "Arrmada360", 110, "Black", (255, 255, 255))
    text(d, (96, 290), "Your whole media server in one Android app.", 40, "SemiBold", (230, 235, 245))
    text(d, (96, 350), "Every feature unlocked — free and open source.", 34, "Regular", (210, 218, 232))
    x, y = 96, 450
    for i, a in enumerate(APPS):
        badge(d, x, y, a, 64); x += 80
        if x > 1500: x, y = 96, y + 80
    im.save(f"{OUT}/banner.png", optimize=True); print("wrote banner.png")

for fn in (onboarding, movies, server, sonarr, qbit, unraid, settings, rss, banner): fn()
