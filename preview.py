import json, math
from PIL import Image, ImageDraw, ImageFont

import os
HERE = os.path.dirname(os.path.abspath(__file__))
data = json.load(open(os.path.join(HERE, "..", "app/src/main/assets/exercises.json")))
EX = data["exercises"]

BONES = [("head","shoulder"),("shoulder","hip"),("shoulder","elbow"),("elbow","hand"),
         ("hip","knee"),("knee","foot")]
FAR = [("shoulder","elbow2"),("elbow2","hand2"),("hip","knee2"),("knee2","foot2")]

CELL = 150
COLS = 6
FR = 3  # frames sampled per exercise

def lerp(a,b,t): return a+(b-a)*t

def sample(frames, t):
    for i in range(len(frames)-1):
        t0,p0 = frames[i]; t1,p1 = frames[i+1]
        if t0 <= t <= t1:
            u = 0 if t1==t0 else (t-t0)/(t1-t0)
            u = u*u*(3-2*u)
            keys = set(p0)|set(p1)
            out={}
            for k in keys:
                a = p0.get(k,p1.get(k)); b = p1.get(k,p0.get(k))
                out[k]=[lerp(a[0],b[0],u), lerp(a[1],b[1],u)]
            return out
    return frames[-1][1]

rows = math.ceil(len(EX)*FR/COLS)
img = Image.new("RGB",(COLS*CELL, rows*CELL+0),(22,24,28))
d = ImageDraw.Draw(img)
try: font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 10)
except: font = ImageFont.load_default()

i=0
for e in EX:
    for fi in range(FR):
        cx = (i%COLS)*CELL; cy=(i//COLS)*CELL
        i+=1
        d.rectangle([cx,cy,cx+CELL-1,cy+CELL-1], outline=(45,48,54))
        pose = sample(e["frames"], fi/(FR-1) * 0.5)  # 0 .. mid
        def pt(k):
            p = pose.get(k)
            if not p: return None
            return (cx+8+p[0]/100*(CELL-16), cy+18+p[1]/100*(CELL-26))
        # floor
        fy = cy+18+92/100*(CELL-26)
        d.line([cx+4,fy,cx+CELL-4,fy], fill=(60,64,72))
        def XY(x,y): return (cx+8+x/100*(CELL-16), cy+18+y/100*(CELL-26))
        for pr in e.get("props",[]):
            if pr["kind"]=="box":
                a=XY(pr["x"],pr["y"]); b=XY(pr["x"]+pr["w"],pr["y"]+pr["h"])
                d.rectangle([a,b], outline=(80,120,180), width=2)
            elif pr["kind"]=="wall":
                a=XY(pr["x"],4); b=XY(pr["x"],92)
                d.line([a,b], fill=(80,120,180), width=3)
            elif pr["kind"]=="bar":
                d.line([XY(pr["x1"],pr["y1"]),XY(pr["x2"],pr["y2"])], fill=(80,120,180), width=3)
            elif pr["kind"]=="strap":
                h2=pose.get("hand")
                if h2: d.line([XY(h2[0],h2[1]),XY(pr["x"],pr["y"])], fill=(80,120,180), width=2)
        for a,b in FAR:
            pa,pb = pt(a),pt(b)
            if pa and pb: d.line([pa,pb], fill=(90,96,110), width=2)
        for a,b in BONES:
            pa,pb = pt(a),pt(b)
            if pa and pb: d.line([pa,pb], fill=(235,238,242), width=3)
        h = pt("head")
        if h: d.ellipse([h[0]-6,h[1]-6,h[0]+6,h[1]+6], outline=(235,238,242), width=3)
        if fi==0:
            d.text((cx+5,cy+3), e["id"][:26], fill=(150,160,175), font=font)

img.save("preview.png")
print("rows",rows,"cells",i)
