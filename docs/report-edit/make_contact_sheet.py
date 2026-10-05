from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

MEDIA = Path(r"E:\dynamicmart\docs\report-edit\source-unpacked\content\word\media")
OUT = Path(r"E:\dynamicmart\docs\report-edit\media-contact-sheet.png")
files = sorted(MEDIA.glob("image*"), key=lambda p: int("".join(filter(str.isdigit, p.stem)) or 0))
cell_w, cell_h, cols = 420, 300, 3
rows = (len(files) + cols - 1) // cols
sheet = Image.new("RGB", (cell_w * cols, cell_h * rows), "white")
draw = ImageDraw.Draw(sheet)
for i, path in enumerate(files):
    image = Image.open(path).convert("RGB")
    image.thumbnail((cell_w - 24, cell_h - 48))
    x = (i % cols) * cell_w + (cell_w - image.width) // 2
    y = (i // cols) * cell_h + 34
    sheet.paste(image, (x, y))
    draw.text(((i % cols) * cell_w + 10, (i // cols) * cell_h + 8), path.name, fill="black")
sheet.save(OUT)
print(OUT)
