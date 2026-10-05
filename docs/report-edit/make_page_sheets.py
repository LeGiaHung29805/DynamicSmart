from pathlib import Path
import sys
from PIL import Image, ImageDraw

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r"E:\dynamicmart\docs\report-edit\render-final")
OUT = Path(sys.argv[2]) if len(sys.argv) > 2 else Path(r"E:\dynamicmart\docs\report-edit\qa-sheets")
OUT.mkdir(exist_ok=True)
pages = sorted(ROOT.glob("page-*.png"), key=lambda p: int(p.stem.split("-")[-1]))
per_sheet, cols = 12, 3
cell_w, cell_h = 440, 620
for start in range(0, len(pages), per_sheet):
    group = pages[start:start + per_sheet]
    rows = (len(group) + cols - 1) // cols
    sheet = Image.new("RGB", (cell_w * cols, cell_h * rows), "#d0d0d0")
    draw = ImageDraw.Draw(sheet)
    for idx, path in enumerate(group):
        image = Image.open(path).convert("RGB")
        image.thumbnail((cell_w - 20, cell_h - 42))
        x0 = (idx % cols) * cell_w + (cell_w - image.width) // 2
        y0 = (idx // cols) * cell_h + 32
        sheet.paste(image, (x0, y0))
        draw.text(((idx % cols) * cell_w + 10, (idx // cols) * cell_h + 8), f"Trang {start + idx + 1}", fill="black")
    target = OUT / f"pages-{start + 1:02d}-{start + len(group):02d}.png"
    sheet.save(target)
    print(target)
