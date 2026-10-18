"""Pressão Contida — arte de fundo do login do Valtis. Renderiza em 2x e reduz (antialias)."""
import math
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

FONTES = sys.argv[1]
SAIDA = sys.argv[2]

W, H = 2400, 1500
K = 2  # supersampling
w, h = W * K, H * K
cx, cy = w / 2, h / 2

# Fundo: degradê radial azul-noite
fundo = Image.new("RGB", (w, h), (4, 11, 28))
px = fundo.load()
grad = Image.new("L", (w // 8, h // 8))
gp = grad.load()
for y in range(h // 8):
    for x in range(w // 8):
        dx = (x * 8 - cx) / w
        dy = (y * 8 - cy) / h
        d = math.sqrt(dx * dx * 1.0 + dy * dy * 1.6)
        gp[x, y] = int(max(0, 255 * (1 - d * 1.6)))
grad = grad.resize((w, h), Image.BICUBIC)
centro = Image.new("RGB", (w, h), (11, 28, 64))
fundo = Image.composite(centro, fundo, grad)

camada = Image.new("RGBA", (w, h), (0, 0, 0, 0))
d = ImageDraw.Draw(camada)

# Grade de pontos (papel milimetrado)
passo = 48 * K
for y in range(passo, h, passo):
    for x in range(passo, w, passo):
        r = 1.1 * K
        d.ellipse([x - r, y - r, x + r, y + r], fill=(125, 165, 225, 26))

# Linhas de fluxo: densas à esquerda (alta pressão), estreitam no centro, saem espaçadas (baixa pressão)
N = 120
garganta = 0.20  # quanto o fluxo aperta no centro
largura_garganta = 0.13 * w


def y_linha(t, x):
    """t em [-1, 1]: posição relativa da linha; x em px."""
    u = (x - cx) / largura_garganta
    aperto = 1 - (1 - garganta) * math.exp(-u * u)
    # saída levemente mais aberta que a entrada (a pressão caiu)
    abertura = 0.42 + 0.07 * (0.5 + 0.5 * math.tanh((x - cx) / (0.14 * w)))
    return cy + t * abertura * h * aperto


for i in range(N):
    t = -1 + 2 * i / (N - 1)
    pts = [(x, y_linha(t, x)) for x in range(-20, w + 20, 6 * K)]
    # alta pressão: todas as linhas fortes; baixa pressão: uma em cada três segue forte
    destaque_saida = i % 3 == 0
    ciano = i % 17 == 8
    for j in range(len(pts) - 1):
        x0, y0 = pts[j]
        x1, y1 = pts[j + 1]
        u = (x0 - cx) / w
        # transição suave entre alta e baixa pressão (sem degrau no centro)
        s_ = 0.5 + 0.5 * math.tanh(u / 0.05)
        alta = 70 + 40 * (1 - min(1, abs(u) * 2))
        baixa = (70 if destaque_saida else 18) * (1 - max(0, u) * 0.9)
        a = alta * (1 - s_) + baixa * s_
        borda = min(1, min(x0, w - x0) / (0.06 * w))  # some nas laterais
        a *= borda
        cor = (125, 211, 252) if ciano else (94, 156, 233)
        if ciano:
            a = min(255, a * 1.9)
        d.line([x0, y0, x1, y1], fill=cor + (int(max(0, a)),), width=int(1.3 * K))

# Brilho suave nas linhas ciano
brilho = camada.filter(ImageFilter.GaussianBlur(6 * K))
fundo = fundo.convert("RGBA")
fundo = Image.alpha_composite(fundo, brilho)
fundo = Image.alpha_composite(fundo, camada)

# Anel de 12 marcações (uma volta = um ciclo de 12 meses)
anel = Image.new("RGBA", (w, h), (0, 0, 0, 0))
a = ImageDraw.Draw(anel)
R = 0.39 * h
a.ellipse([cx - R, cy - R, cx + R, cy + R], outline=(125, 165, 225, 40), width=int(1 * K))
for m in range(60):
    ang = -math.pi / 2 + m * 2 * math.pi / 60
    maior = m % 5 == 0
    r0 = R + (4 if maior else 6) * K
    r1 = R + (22 if maior else 12) * K
    cor = (125, 211, 252, 200) if m == 0 else (125, 165, 225, 110 if maior else 45)
    a.line(
        [cx + r0 * math.cos(ang), cy + r0 * math.sin(ang), cx + r1 * math.cos(ang), cy + r1 * math.sin(ang)],
        fill=cor,
        width=int((1.6 if maior else 1) * K),
    )
# arco ciano de progresso (~10 de 12 meses)
a.arc([cx - R, cy - R, cx + R, cy + R], start=-90, end=-90 + 300, fill=(56, 189, 248, 120), width=int(2 * K))
fundo = Image.alpha_composite(fundo, anel)

# Rótulos clínicos
t = ImageDraw.Draw(fundo)
mono = ImageFont.truetype(f"{FONTES}/GeistMono-Regular.ttf", 13 * K)
mono_p = ImageFont.truetype(f"{FONTES}/GeistMono-Regular.ttf", 11 * K)
cor_r = (163, 179, 203, 150)
cor_c = (125, 211, 252, 190)
m = 64 * K


def rotulo(x, y, texto, fonte=mono, cor=cor_r, ancora="la"):
    t.text((x, y), texto, font=fonte, fill=cor, anchor=ancora)


rotulo(m, m, "P1  ·  ENTRADA")
rotulo(m, m + 22 * K, "ALTA PRESSÃO", mono_p, (132, 150, 176, 120))
rotulo(w - m, m, "P2  ·  SAÍDA", ancora="ra")
rotulo(w - m, m + 22 * K, "PRESSÃO REGULADA", mono_p, (132, 150, 176, 120), "ra")
rotulo(m, h - m, "FIG. 01 — CONTENÇÃO", mono_p, (132, 150, 176, 120), "ld")
rotulo(w - m, h - m, "CICLO 12M", mono_p, cor_c, "rd")
# marcação dos 12 meses no anel (só "00" e "06", discretas)
rotulo(cx, cy - R - 34 * K, "00", mono_p, cor_c, "md")
rotulo(cx, cy + R + 34 * K, "06", mono_p, (132, 150, 176, 120), "ma")

# Vinheta
vin = Image.new("L", (w // 8, h // 8))
vp = vin.load()
for y in range(h // 8):
    for x in range(w // 8):
        dx = (x * 8 - cx) / (w / 2)
        dy = (y * 8 - cy) / (h / 2)
        dd = math.sqrt(dx * dx + dy * dy)
        vp[x, y] = int(min(255, max(0, (dd - 0.75) * 300)))
vin = vin.resize((w, h), Image.BICUBIC)
escuro = Image.new("RGBA", (w, h), (3, 8, 20, 255))
fundo = Image.composite(escuro, fundo, vin.point(lambda v: int(v * 0.7)))

fundo = fundo.convert("RGB").resize((W, H), Image.LANCZOS)
fundo.save(SAIDA, optimize=True)
print("ok", SAIDA)
