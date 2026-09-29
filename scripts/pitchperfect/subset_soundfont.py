#!/usr/bin/env python3
"""Builds Pitch Perfect's instrument SoundFont from GeneralUser GS.

Copies only the General MIDI presets Pitch Perfect offers, with their
instruments, generators, modulators and samples unchanged, so each keeps the
envelopes, filters and loops its author programmed. Presets keep their GM
program numbers. Writes shared/pitchperfect/PitchPerfectInstruments.sf2.

    python3 scripts/pitchperfect/subset_soundfont.py [--source GeneralUser-GS.sf2]
"""

import argparse
import os
import struct
import tempfile
import urllib.request

SOUNDFONT_URL = "https://github.com/mrbumpy409/GeneralUser-GS/raw/main/GeneralUser-GS.sf2"
REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUTPUT = os.path.join(REPO, "shared", "pitchperfect", "PitchPerfectInstruments.sf2")

# The GM programs behind the Sound setting; see docs/pitchperfect-note-sounds.md.
PROGRAMS = [0, 4, 6, 11, 19, 21, 24, 46, 48, 52, 56, 71, 73]

GEN_INSTRUMENT = 41
GEN_SAMPLE_ID = 53
SAMPLE_PADDING = 46  # zero frames after each sample, as the spec requires


def chunks(data, start, end, out):
    i = start
    while i < end:
        cid = data[i:i + 4].decode("latin1")
        size = struct.unpack_from("<I", data, i + 4)[0]
        if cid == "LIST":
            chunks(data, i + 12, i + 8 + size, out)
        else:
            out[cid] = data[i + 8:i + 8 + size]
        i += 8 + size + (size & 1)
    return out


def records(raw, fmt):
    size = struct.calcsize(fmt)
    return [struct.unpack_from(fmt, raw, i) for i in range(0, len(raw), size)]


def riff(cid, payload):
    pad = b"\0" if len(payload) & 1 else b""
    return cid.encode("latin1") + struct.pack("<I", len(payload)) + payload + pad


def riff_list(kind, parts):
    payload = kind.encode("latin1") + b"".join(parts)
    return riff("LIST", payload)


def subset(source):
    data = open(source, "rb").read()
    c = chunks(data, 12, len(data), {})
    phdr = records(c["phdr"], "<20sHHHIII")
    pbag = records(c["pbag"], "<HH")
    pmod = [c["pmod"][i:i + 10] for i in range(0, len(c["pmod"]), 10)]
    pgen = records(c["pgen"], "<HH")
    inst = records(c["inst"], "<20sH")
    ibag = records(c["ibag"], "<HH")
    imod = [c["imod"][i:i + 10] for i in range(0, len(c["imod"]), 10)]
    igen = records(c["igen"], "<HH")
    shdr = records(c["shdr"], "<20sIIIIIBbHH")
    smpl = c["smpl"]

    presets = sorted(
        (i for i, h in enumerate(phdr[:-1]) if h[2] == 0 and h[1] in PROGRAMS),
        key=lambda i: phdr[i][1])
    missing = set(PROGRAMS) - {phdr[i][1] for i in presets}
    assert not missing, f"programs not in the bank: {missing}"

    # Instruments the presets use, in first-use order, then their samples
    # (and each stereo partner).
    inst_order, sample_order = [], []

    def use(order, value):
        if value not in order:
            order.append(value)
        return order.index(value)

    for p in presets:
        for b in range(phdr[p][3], phdr[p + 1][3]):
            for g in range(pbag[b][0], pbag[b + 1][0]):
                if pgen[g][0] == GEN_INSTRUMENT:
                    use(inst_order, pgen[g][1])
    for i in inst_order:
        for b in range(inst[i][1], inst[i + 1][1]):
            for g in range(ibag[b][0], ibag[b + 1][0]):
                if igen[g][0] == GEN_SAMPLE_ID:
                    s = igen[g][1]
                    use(sample_order, s)
                    if shdr[s][9] in (2, 4, 8):  # right/left/linked: keep the partner
                        use(sample_order, shdr[s][8])

    out = {k: [] for k in ("phdr", "pbag", "pmod", "pgen", "inst", "ibag", "imod", "igen", "shdr")}
    for p in presets:
        name, program, bank, bag0, library, genre, morphology = phdr[p]
        out["phdr"].append(struct.pack("<20sHHHIII", name, program, bank, len(out["pbag"]), library, genre, morphology))
        for b in range(bag0, phdr[p + 1][3]):
            out["pbag"].append(struct.pack("<HH", len(out["pgen"]), len(out["pmod"])))
            out["pmod"].extend(pmod[pbag[b][1]:pbag[b + 1][1]])
            for g in range(pbag[b][0], pbag[b + 1][0]):
                op, amount = pgen[g]
                if op == GEN_INSTRUMENT:
                    amount = inst_order.index(amount)
                out["pgen"].append(struct.pack("<HH", op, amount))
    out["phdr"].append(struct.pack("<20sHHHIII", b"EOP", 0, 0, len(out["pbag"]), 0, 0, 0))
    out["pbag"].append(struct.pack("<HH", len(out["pgen"]), len(out["pmod"])))
    out["pmod"].append(b"\0" * 10)
    out["pgen"].append(b"\0" * 4)

    for i in inst_order:
        out["inst"].append(struct.pack("<20sH", inst[i][0], len(out["ibag"])))
        for b in range(inst[i][1], inst[i + 1][1]):
            out["ibag"].append(struct.pack("<HH", len(out["igen"]), len(out["imod"])))
            out["imod"].extend(imod[ibag[b][1]:ibag[b + 1][1]])
            for g in range(ibag[b][0], ibag[b + 1][0]):
                op, amount = igen[g]
                if op == GEN_SAMPLE_ID:
                    amount = sample_order.index(amount)
                out["igen"].append(struct.pack("<HH", op, amount))
    out["inst"].append(struct.pack("<20sH", b"EOI", len(out["ibag"])))
    out["ibag"].append(struct.pack("<HH", len(out["igen"]), len(out["imod"])))
    out["imod"].append(b"\0" * 10)
    out["igen"].append(b"\0" * 4)

    samples = bytearray()
    for s in sample_order:
        name, start, end, loop_start, loop_end, rate, pitch, correction, link, kind = shdr[s]
        base = len(samples) // 2
        samples += smpl[start * 2:end * 2] + b"\0" * (SAMPLE_PADDING * 2)
        new_link = sample_order.index(link) if kind in (2, 4, 8) else 0
        out["shdr"].append(struct.pack(
            "<20sIIIIIBbHH", name, base, base + end - start, base + loop_start - start,
            base + loop_end - start, rate, pitch, correction, new_link, kind))
    out["shdr"].append(struct.pack("<20sIIIIIBbHH", b"EOS", 0, 0, 0, 0, 0, 0, 0, 0, 0))

    info = [riff(k, c[k]) for k in ("ifil", "isng") if k in c]
    info.append(riff("INAM", b"Pitch Perfect Instruments\0\0\0"))
    info += [riff(k, c[k]) for k in ("ICRD", "IENG", "ICOP") if k in c]
    info.append(riff("ICMT", b"Subset of GeneralUser GS v2.0.3 by S. Christian Collins "
                             b"(www.schristiancollins.com), made by scripts/pitchperfect/subset_soundfont.py\0"))
    body = (b"sfbk"
            + riff_list("INFO", info)
            + riff_list("sdta", [riff("smpl", bytes(samples))])
            + riff_list("pdta", [riff(k, b"".join(out[k])) for k in
                                 ("phdr", "pbag", "pmod", "pgen", "inst", "ibag", "imod", "igen", "shdr")]))
    return riff("RIFF", body), [phdr[p][0].split(b"\0")[0].decode() for p in presets]


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--source", help="GeneralUser-GS.sf2; downloaded when omitted")
    args = parser.parse_args()
    source = args.source
    if not source:
        source = os.path.join(tempfile.gettempdir(), "GeneralUser-GS.sf2")
        if not os.path.exists(source):
            print("downloading GeneralUser GS…")
            urllib.request.urlretrieve(SOUNDFONT_URL, source)
    font, names = subset(source)
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with open(OUTPUT, "wb") as f:
        f.write(font)
    print(f"{OUTPUT}: {len(font) / 1e6:.1f} MB, presets: {', '.join(names)}")


if __name__ == "__main__":
    main()
