"""Artes das opções de login: B (isóbaras/topografia de pressão) e D (planta técnica clara)."""
import math
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

FONTES, SAIDA = sys.argv[1], sys.argv[2]
K = 2


def fonte(nome, tam):
    return ImageFont.truetype(f"{FONTES}/{nome}", tam * K)


# ---------- B: Isóbaras (painel de marca, retrato 1200x1500) ----------
def isobaras():
    W, H = 1200, 1500
    w, h = W * K, H * K
    img = Image.new("RGB", (w, h), (8, 24, 58))
    # degradê vertical
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        c = (int(14 - 8 * t), int(42 - 26 * t), int(92 - 58 * t))
        d.line([(0, y), (w, y)], fill=c)
    camada = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    c = ImageDraw.Draw(camada)
    # campo de pressão: dois centros (alta e baixa) — curvas de nível
    fontes = [(0.30, 0.38, 1.0), (0.78, 0.72, -0.8), (0.15, 0.85, 0.35)]

    def campo(x, y):
        v = 0
        for fx, fy, s in fontes:
            dx, dy = x / w - fx, (y / h - fy) * 0.8
            v += s * math.exp(-(dx * dx + dy * dy) / 0.06)
        return v

    passo = 6 * K
    nx, ny = w // passo + 1, h // passo + 1
    grade = [[campo(i * passo, j * passo) for i in range(nx)] for j in range(ny)]
    niveis = [-0.7 + k * 0.07 for k in range(26)]
    for n, nivel in enumerate(niveis):
        forte = n % 5 == 0
        cor = (125, 211, 252, 150) if forte else (94, 156, 233, 70)
        for j in range(ny - 1):
            for i in range(nx - 1):
                a, b = grade[j][i], grade[j][i + 1]
                cc, dd = grade[j + 1][i], grade[j + 1][i + 1]
                pts = []
                x0, y0 = i * passo, j * passo
                for (v1, v2, p1, p2) in (
                    (a, b, (x0, y0), (x0 + passo, y0)),
                    (b, dd, (x0 + passo, y0), (x0 + passo, y0 + passo)),
                    (cc, dd, (x0, y0 + passo), (x0 + passo, y0 + passo)),
                    (a, cc, (x0, y0), (x0, y0 + passo)),
                ):
                    if (v1 - nivel) * (v2 - nivel) < 0:
                        t = (nivel - v1) / (v2 - v1)
                        pts.append((p1[0] + t * (p2[0] - p1[0]), p1[1] + t * (p2[1] - p1[1])))
                if len(pts) >= 2:
                    c.line([pts[0], pts[1]], fill=cor, width=int((1.6 if forte else 1.0) * K))
    brilho = camada.filter(ImageFilter.GaussianBlur(5 * K))
    img = Image.alpha_composite(img.convert("RGBA"), brilho)
    img = Image.alpha_composite(img, camada)
    t = ImageDraw.Draw(img)
    mono = fonte("GeistMono-Regular.ttf", 12)
    m = 56 * K
    # marcadores de alta/baixa
    for fx, fy, s in fontes[:2]:
        x, y = fx * w, fy * h
        r = 5 * K
        t.ellipse([x - r, y - r, x + r, y + r], outline=(125, 211, 252, 230), width=int(1.5 * K))
        t.text((x + 14 * K, y - 8 * K), "A" if s > 0 else "B", font=mono, fill=(186, 230, 253, 220))
    t.text((m, h - m), "ISÓBARAS · kgf/cm²", font=mono, fill=(163, 179, 203, 140), anchor="ld")
    img.convert("RGB").resize((W, H), Image.LANCZOS).save(f"{SAIDA}/b-isobaras.jpg", quality=86, optimize=True)


# ---------- D: Planta técnica clara (paisagem 2400x1500) ----------
def planta():
    W, H = 2400, 1500
    w, h = W * K, H * K
    img = Image.new("RGB", (w, h), (241, 245, 250))
    d = ImageDraw.Draw(img, "RGBA")
    # grade de papel técnico
    for x in range(0, w, 24 * K):
        forte = (x // (24 * K)) % 5 == 0
        d.line([(x, 0), (x, h)], fill=(31, 95, 191, 26 if forte else 12), width=K)
    for y in range(0, h, 24 * K):
        forte = (y // (24 * K)) % 5 == 0
        d.line([(0, y), (w, y)], fill=(31, 95, 191, 26 if forte else 12), width=K)

    azul = (31, 95, 191, 150)
    azul_f = (31, 95, 191, 70)
    lw = int(2.2 * K)
    cy = h * 0.5
    # tubulação principal
    def tubo(x0, x1, y):
        d.line([(x0, y - 14 * K), (x1, y - 14 * K)], fill=azul, width=lw)
        d.line([(x0, y + 14 * K), (x1, y + 14 * K)], fill=azul, width=lw)

    # esquema na metade esquerda: o cartão de login fica à direita
    xin, xrg1, xfy, xvrp, xp2, xrg2, xout = (f * w for f in (0.03, 0.10, 0.19, 0.30, 0.39, 0.47, 0.60))
    tubo(xin, xrg1 - 34 * K, cy)
    tubo(xrg1 + 34 * K, xfy - 36 * K, cy)
    tubo(xfy + 36 * K, xrg2 - 34 * K, cy)
    tubo(xrg2 + 34 * K, xout, cy)

    def registro(x):  # válvula de bloqueio: gravata
        s = 28 * K
        d.polygon([(x - s, cy - s * 0.8), (x - s, cy + s * 0.8), (x + s, cy - s * 0.8), (x + s, cy + s * 0.8)], outline=azul, width=lw)
        d.line([(x, cy), (x, cy - 56 * K)], fill=azul, width=lw)
        d.line([(x - 20 * K, cy - 56 * K), (x + 20 * K, cy - 56 * K)], fill=azul, width=lw)

    def filtro_y(x):
        s = 36 * K
        d.rectangle([x - s, cy - 22 * K, x + s, cy + 22 * K], outline=azul, width=lw)
        d.line([(x - 10 * K, cy + 22 * K), (x + 30 * K, cy + 80 * K)], fill=azul, width=lw)
        d.line([(x + 10 * K, cy + 22 * K), (x + 50 * K, cy + 80 * K)], fill=azul, width=lw)

    def vrp(x):  # redutora: dois triângulos, haste, diafragma e mola
        s = 62 * K
        d.polygon([(x - s, cy - s * 0.7), (x - s, cy + s * 0.7), (x, cy)], fill=(241, 245, 250), outline=azul, width=lw)
        d.polygon([(x + s, cy - s * 0.7), (x + s, cy + s * 0.7), (x, cy)], fill=(241, 245, 250), outline=azul, width=lw)
        d.line([(x, cy), (x, cy - 110 * K)], fill=azul, width=lw)
        d.chord([x - 56 * K, cy - 150 * K, x + 56 * K, cy - 70 * K], 180, 360, outline=azul, width=lw)
        pts = [(x + (14 if k % 2 else -14) * K, cy - 150 * K - k * 9 * K) for k in range(9)]
        d.line(pts, fill=azul, width=lw)
        # linha de pilotagem tracejada até a jusante
        yp = cy - 210 * K
        for xx in range(int(x), int(xrg2 - 60 * K), 18 * K):
            d.line([(xx, yp), (xx + 9 * K, yp)], fill=azul_f, width=K * 2)
        d.line([(xrg2 - 60 * K, yp), (xrg2 - 60 * K, cy - 14 * K)], fill=azul_f, width=K * 2)

    def manometro(x, y0):
        r = 30 * K
        d.line([(x, cy - 14 * K), (x, y0 + r)], fill=azul, width=lw)
        d.ellipse([x - r, y0 - r, x + r, y0 + r], fill=(241, 245, 250), outline=azul, width=lw)
        d.line([(x, y0), (x + r * 0.6, y0 - r * 0.5)], fill=azul, width=lw)

    registro(xrg1)
    filtro_y(xfy)
    vrp(xvrp)
    registro(xrg2)
    manometro((xfy + xvrp) / 2 + 20 * K, cy - 170 * K)
    manometro(xp2, cy - 170 * K)

    # setas de fluxo
    for x in (xin + 60 * K, xout - 140 * K):
        d.line([(x, cy), (x + 60 * K, cy)], fill=azul, width=lw)
        d.polygon([(x + 60 * K, cy - 9 * K), (x + 78 * K, cy), (x + 60 * K, cy + 9 * K)], fill=azul)

    mono = fonte("GeistMono-Regular.ttf", 13)
    peq = fonte("GeistMono-Regular.ttf", 11)
    cor = (31, 79, 163, 170)
    def rot(x, y, s, f=peq, anc="ma"):
        d.text((x, y), s, font=f, fill=cor, anchor=anc)
    rot(xrg1, cy + 60 * K, "RG-01")
    rot(xfy, cy + 100 * K, "FY-01")
    rot(xvrp, cy + 70 * K, "VRP-01", mono)
    rot(xrg2, cy + 60 * K, "RG-02")
    rot((xfy + xvrp) / 2 + 20 * K, cy - 248 * K, "P1")
    rot(xp2, cy - 248 * K, "P2")
    m = 64 * K
    # carimbo (legenda técnica)
    bx, by = m, h - m - 120 * K
    d.rectangle([bx, by, bx + 420 * K, h - m], outline=azul_f, width=K * 2)
    d.line([(bx, by + 40 * K), (bx + 420 * K, by + 40 * K)], fill=azul_f, width=K * 2)
    d.line([(bx + 260 * K, by + 40 * K), (bx + 260 * K, h - m)], fill=azul_f, width=K * 2)
    rot(bx + 16 * K, by + 12 * K, "ESTAÇÃO REDUTORA DE PRESSÃO — ESQUEMA", peq, "la")
    rot(bx + 16 * K, by + 56 * K, "MANUTEC VÁLVULAS", peq, "la")
    rot(bx + 16 * K, by + 82 * K, "RECIFE / PE", peq, "la")
    rot(bx + 276 * K, by + 56 * K, "FOLHA 01/01", peq, "la")
    rot(bx + 276 * K, by + 82 * K, "CICLO 12M", peq, "la")
    img.resize((W, H), Image.LANCZOS).save(f"{SAIDA}/d-planta.jpg", quality=86, optimize=True)


planta()
print("ok")
