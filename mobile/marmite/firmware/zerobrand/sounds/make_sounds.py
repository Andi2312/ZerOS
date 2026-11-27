import numpy as np, os, soundfile as sf

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "ui")
os.makedirs(OUT, exist_ok=True)

def fade(x, ms=4):
    n = int(SR*ms/1000)
    if n*2 >= len(x): n = max(1, len(x)//4)
    w = np.linspace(0,1,n)
    x[:n] *= w
    x[-n:] *= w[::-1]
    return x

def tone(freq, dur, decay=18.0, amp=1.0, harmonics=((2,0.25),(3,0.12))):
    t = np.arange(int(SR*dur))/SR
    y = np.sin(2*np.pi*freq*t)
    for k,a in harmonics:
        y += a*np.sin(2*np.pi*freq*k*t)
    y *= np.exp(-decay*t)
    return amp*y

def sweep(f0, f1, dur, decay=14.0, amp=1.0):
    n = int(SR*dur)
    t = np.arange(n)/SR
    f = np.linspace(f0, f1, n)
    ph = 2*np.pi*np.cumsum(f)/SR
    y = np.sin(ph) * np.exp(-decay*t)
    return amp*y

def noise(dur, amp=1.0, smooth=0.5):
    n = int(SR*dur)
    x = np.random.RandomState(7).randn(n)
    # simple low-pass
    y = np.zeros(n)
    acc = 0.0
    a = smooth
    for i in range(n):
        acc = a*acc + (1-a)*x[i]
        y[i] = acc
    return amp*y

def click(freq=2400, dur=0.02, amp=0.8):
    y = tone(freq, dur, decay=90, amp=amp, harmonics=((2,0.15),))
    nz = noise(dur, amp*0.3, 0.15)
    y += nz
    return y

def seq(parts, gap=0.0):
    out = []
    for p in parts:
        out.append(p)
        if gap: out.append(np.zeros(int(SR*gap)))
    return np.concatenate(out)

def mix(*arrs):
    n = max(len(a) for a in arrs)
    y = np.zeros(n)
    for a in arrs:
        y[:len(a)] += a
    return y

def save(name, y, peak=0.35):
    y = fade(np.asarray(y, dtype=np.float64), 4)
    m = np.max(np.abs(y))
    if m > 0: y = y/m*peak
    path = os.path.join(OUT, name)
    sf.write(path, y.astype(np.float32), SR, format='OGG', subtype='VORBIS')
    print(f"{name:24s} {len(y)/SR*1000:6.1f} ms")

save("Effect_Tick.ogg", click(2400, 0.018, 0.7), 0.30)
save("KeypressStandard.ogg", mix(click(1700, 0.028, 0.55), tone(3400,0.02,120,0.12)), 0.30)
save("KeypressSpacebar.ogg", mix(click(1100, 0.035, 0.6), tone(2200,0.025,110,0.1)), 0.30)
save("KeypressDelete.ogg", sweep(1500, 900, 0.035, 60, 0.6), 0.28)
save("KeypressReturn.ogg", seq([tone(880,0.03,70,0.5), tone(1320,0.04,60,0.45)]), 0.28)
save("KeypressInvalid.ogg", seq([sweep(190,150,0.10,22,0.7), sweep(150,120,0.06,25,0.5)]), 0.26)
save("camera_click.ogg", seq([mix(noise(0.006,0.9,0.1), tone(1900,0.010,150,0.4)),
                              mix(noise(0.009,0.6,0.08), tone(1200,0.012,140,0.3))], 0.018), 0.32)
save("camera_focus.ogg", tone(2000, 0.04, 70, 0.4), 0.22)
save("VideoRecord.ogg", seq([sweep(700,1050,0.09,16,0.5)]), 0.28)
save("VideoStop.ogg", seq([sweep(1050,700,0.09,16,0.5)]), 0.28)
save("Lock.ogg", seq([tone(1250,0.045,55,0.45), tone(830,0.07,38,0.5)], 0.01), 0.28)
save("Unlock.ogg", seq([tone(830,0.045,55,0.45), tone(1250,0.07,38,0.5)], 0.01), 0.28)
save("ChargingStarted.ogg", seq([tone(660,0.06,40,0.4), tone(880,0.06,40,0.4), tone(1320,0.12,30,0.42)], 0.02), 0.30)
save("LowBattery.ogg", seq([tone(740,0.11,18,0.45), tone(554,0.16,14,0.45)], 0.05), 0.30)
save("Dock.ogg", seq([sweep(300,180,0.07,45,0.5)]), 0.26)
save("Undock.ogg", seq([sweep(180,300,0.07,45,0.5)]), 0.26)
