from PIL import Image, ImageDraw, ImageFont

W,H = 1024,512
img = Image.new("RGBA",(W,H),(0,0,0,0))
d = ImageDraw.Draw(img)
fontpath = r"C:\Windows\Fonts\segoeuib.ttf"
text = "ZerOS I"
size = 260
font = ImageFont.truetype(fontpath, size)
bbox = d.textbbox((0,0), text, font=font)
tw = bbox[2]-bbox[0]; th = bbox[3]-bbox[1]
# fit width to 880
scale = 880.0/tw
size = int(size*scale)
font = ImageFont.truetype(fontpath, size)
bbox = d.textbbox((0,0), text, font=font)
tw = bbox[2]-bbox[0]; th = bbox[3]-bbox[1]
x = (W-tw)//2 - bbox[0]
y = (H-th)//2 - bbox[1] - 20
d.text((x,y), text, font=font, fill=(255,255,255,255))
# underline
bar_y = y + bbox[3] + 26
bar_h = max(4, size//28)
d.rectangle([x+bbox[0], bar_y, x+bbox[0]+tw, bar_y+bar_h], fill=(255,255,255,255))
img.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "logo_lineage.png"))
print("font", size, "text", tw, th, "saved")
