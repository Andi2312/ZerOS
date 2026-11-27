import numpy as np, os, soundfile as sf

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "ui")
NOTIF = os.path.join(HERE, "notifications")
RING = os.path.join(HERE, "ringtones")
ALARM = os.path.join(HERE, "alarms")
for d in (OUT, NOTIF, RING, ALARM):
    os.makedirs(d, exist_ok=True)

def fade(x, ms=4):
    n = int(SR*ms/1000)
    if n*2 >= len(x): n = max(1, len(x)//4)
    w = np.linspace(0,1,n)
    x[:n] *= w
    x[-n:] *= w[::-1]
    return x

def attack(x, ms=6):
    n = min(int(SR*ms/1000), len(x)//2)
    if n > 1:
        x[:n] *= np.linspace(0,1,n)
    return x

def tone(freq, dur, decay=18.0, amp=1.0, harmonics=((2,0.25),(3,0.12)), attack_ms=0):
    t = np.arange(int(SR*dur))/SR
    y = np.sin(2*np.pi*freq*t)
    for k,a in harmonics:
        y += a*np.sin(2*np.pi*freq*k*t)
    y *= np.exp(-decay*t)
    if attack_ms:
        y = attack(y, attack_ms)
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

def save(name, y, peak=0.35, folder=None, fade_ms=4, quality=0.6):
    y = np.asarray(y, dtype=np.float64)
    if fade_ms:
        y = fade(y, fade_ms)
    m = np.max(np.abs(y))
    if m > 0: y = y/m*peak
    path = os.path.join(folder or OUT, name)
    # sf.write падает на длинных файлах (libsndfile, ~>11 c) - пишем кусками
    y = y.astype(np.float32)
    with sf.SoundFile(path, 'w', samplerate=SR, channels=1,
                      format='OGG', subtype='VORBIS',
                      compression_level=quality) as f:
        for i in range(0, len(y), 65536):
            f.write(y[i:i+65536])
    print(f"{name:24s} {len(y)/SR*1000:6.1f} ms")

def bell(freq, dur=0.75, decay=7.0):
    t = np.arange(int(SR*dur))/SR
    y = np.sin(2*np.pi*freq*t) * np.exp(-decay*t)
    y += 0.28*np.sin(2*np.pi*freq*2.76*t) * np.exp(-decay*1.6*t)
    y += 0.12*np.sin(2*np.pi*freq*5.40*t) * np.exp(-decay*2.3*t)
    return attack(fade(y, 30), 3)

def at(offset, y):
    out = np.zeros(int(SR*offset) + len(y))
    out[int(SR*offset):] = y
    return out

# --- петли: хвосты заворачиваются в начало, поэтому стык без щелчка

def loop_add(buf, off, y):
    i = int(round(off*SR)) % len(buf)
    if i + len(y) <= len(buf):
        buf[i:i+len(y)] += y
        return
    k = len(buf) - i
    buf[i:] += y[:k]
    rest = y[k:]
    while len(rest):
        m = min(len(rest), len(buf))
        buf[:m] += rest[:m]
        rest = rest[m:]

def pad(freqs, dur, amp=0.10, attack_s=0.6, release_s=1.2):
    n = int(SR*dur)
    t = np.arange(n)/SR
    y = np.zeros(n)
    for f in freqs:
        ph = np.random.RandomState(int(f)).rand()*2*np.pi
        y += np.sin(2*np.pi*f*t + ph)
    y /= len(freqs)
    env = np.ones(n)
    a = min(int(SR*attack_s), n//2)
    r = min(int(SR*release_s), n//2)
    if a > 1: env[:a] = np.linspace(0, 1, a)**1.5
    if r > 1: env[-r:] *= np.linspace(1, 0, r)**1.5
    return y*env*amp

def pluck(freq, dur=1.1, decay=4.5, amp=0.30):
    t = np.arange(int(SR*dur))/SR
    y = np.sin(2*np.pi*freq*t) * np.exp(-decay*t)
    y += 0.25*np.sin(2*np.pi*2*freq*t) * np.exp(-decay*1.7*t)
    y += 0.08*np.sin(2*np.pi*3*freq*t) * np.exp(-decay*2.4*t)
    return attack(y, 5)*amp

def marimba(freq, dur=1.0, amp=1.0):
    t = np.arange(int(SR*dur))/SR
    y = np.sin(2*np.pi*freq*t) * np.exp(-5.0*t)
    y += 0.30*np.sin(2*np.pi*4*freq*t) * np.exp(-9.0*t)
    y += 0.08*np.sin(2*np.pi*10*freq*t) * np.exp(-14.0*t)
    return attack(y, 4)*amp

save("Effect_Tick.ogg", click(2400, 0.018, 0.7), 0.30)
save("KeypressStandard.ogg", mix(click(1700, 0.028, 0.55), tone(3400,0.02,120,0.12)), 0.30)
save("KeypressSpacebar.ogg", mix(click(1100, 0.035, 0.6), tone(2200,0.025,110,0.1)), 0.30)
save("KeypressDelete.ogg", sweep(1500, 900, 0.035, 60, 0.6), 0.28)
save("KeypressReturn.ogg", seq([tone(880,0.03,70,0.5), tone(1320,0.04,60,0.45)]), 0.28)
save("KeypressInvalid.ogg", seq([sweep(190,150,0.10,22,0.7), sweep(150,120,0.06,25,0.5)]), 0.26)
save("camera_click.ogg", seq([mix(noise(0.006,0.9,0.1), tone(1900,0.010,150,0.4)),
                              mix(noise(0.009,0.6,0.08), tone(1200,0.012,140,0.3))], 0.018), 0.32)
save("camera_focus.ogg", tone(2000, 0.04, 70, 0.4), 0.22)
save("VideoRecord.ogg", seq([tone(659.25,0.075,12,0.8,attack_ms=8),
                             tone(987.77,0.115,11,0.9,attack_ms=10)], 0.012), 0.20)
save("VideoStop.ogg", seq([tone(987.77,0.075,12,0.8,attack_ms=8),
                           tone(659.25,0.115,11,0.9,attack_ms=10)], 0.012), 0.20)
save("Lock.ogg", seq([tone(1250,0.045,55,0.45), tone(830,0.07,38,0.5)], 0.01), 0.28)
save("Unlock.ogg", seq([tone(830,0.045,55,0.45), tone(1250,0.07,38,0.5)], 0.01), 0.28)
save("ChargingStarted.ogg", seq([tone(660,0.06,40,0.4), tone(880,0.06,40,0.4), tone(1320,0.12,30,0.42)], 0.02), 0.30)
save("ChargingStopped.ogg", seq([tone(1320,0.05,42,0.4), tone(880,0.06,40,0.4), tone(660,0.12,30,0.42)], 0.02), 0.26)
save("LowBattery.ogg", seq([tone(740,0.11,18,0.45), tone(554,0.16,14,0.45)], 0.05), 0.30)
save("Dock.ogg", seq([sweep(300,180,0.07,45,0.5)]), 0.26)
save("Undock.ogg", seq([sweep(180,300,0.07,45,0.5)]), 0.26)

# --- уведомление Glint: короткий блик

def build_glint():
    freq, dur = 1568.0, 0.42
    t = np.arange(int(SR*dur))/SR
    y = np.sin(2*np.pi*freq*t) * np.exp(-11.0*t)
    y += 0.30*np.sin(2*np.pi*freq*2.01*t) * np.exp(-16.0*t)
    y += 0.10*np.sin(2*np.pi*freq*3.98*t) * np.exp(-22.0*t)
    return attack(fade(y, 25), 2)

# --- рингтон First Light: 8 тактов, 90 BPM, A - E/G# - F#m - D

def build_first_light():
    eighth = 60.0/90/2
    bar = 8*eighth           # 4/4 = 8 восьмых
    n = int(round(8*bar*SR))
    buf = np.zeros(n)

    chords = [
        (0, [110.00, 164.81, 220.00, 277.18, 329.63]),   # A
        (2, [ 82.41, 123.47, 164.81, 207.65, 246.94]),   # E/G#
        (4, [ 92.50, 138.59, 185.00, 220.00, 277.18]),   # F#m
        (6, [146.83, 220.00, 293.66, 369.99]),           # D
    ]
    for b, freqs in chords:
        loop_add(buf, b*bar, pad(freqs, 2*bar, 0.10, 0.8, 1.4))

    melody = [
        (0, 440.00), (2, 554.37), (4, 659.25), (6, 659.25),
        (8, 554.37), (10, 659.25), (12, 880.00), (14, 659.25),
        (16, 493.88), (18, 659.25), (20, 830.61), (22, 659.25),
        (24, 493.88), (26, 659.25), (28, 987.77), (30, 830.61),
        (32, 554.37), (34, 739.99), (36, 880.00), (38, 739.99),
        (40, 554.37), (42, 739.99), (44, 1108.73), (46, 880.00),
        (48, 587.33), (50, 739.99), (52, 880.00), (54, 739.99),
        (56, 587.33), (58, 880.00), (60, 739.99), (62, 587.33),
    ]
    for e, f in melody:
        loop_add(buf, e*eighth, pluck(f, 1.1, 4.5, 0.30))

    buf += 0.18*np.roll(buf, int(0.375*SR))
    return buf

# --- будильник Sunrise: восходящая фраза, поднимается по полутонам

def build_sunrise():
    beat = 60.0/84.0
    phrase = 4*beat
    cycles = 7
    n = int(round(cycles*phrase*SR))
    buf = np.zeros(n)
    notes = [523.25, 659.25, 783.99, 1046.50]        # C5 E5 G5 C6
    transpose = [0, 0, 2, 2, 4, 4, 5]

    for c in range(cycles):
        k = 2**(transpose[c]/12.0)
        for i, f in enumerate(notes):
            off = c*phrase + i*beat
            loop_add(buf, off, marimba(f*k, 1.0, 0.26 + 0.012*c))
            if c >= 4:
                loop_add(buf, off, marimba(f*k*2, 0.8, 0.05*(c - 3)))
        loop_add(buf, c*phrase,
                 pad([130.81*k, 196.00*k, 261.63*k], phrase, 0.05, 0.4, 0.9))

    buf += 0.15*np.roll(buf, int(0.25*SR))
    return buf

save("Glint.ogg", build_glint(), 0.50, NOTIF)
save("FirstLight.ogg", build_first_light(), 0.85, RING, fade_ms=0, quality=0.2)
save("Sunrise.ogg", build_sunrise(), 0.80, ALARM, fade_ms=0, quality=0.2)
