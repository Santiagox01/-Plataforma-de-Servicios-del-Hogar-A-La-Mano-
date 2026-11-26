/* ==========================================================================
   PIXELSTACK VOXEL 3D ENGINE + STUDIO AUDIO FX (Portado de ProyectoYT 2)
   Implementación pura Canvas 2D: VoxelGrid (box, carveBox, cylinder,
   carveCylinder, cone, wedge, dither), cel-shading de 4 tonos, contorno 1px,
   drawScene Y-sorted, haz volumétrico y partículas pixel-art.
   ========================================================================== */

(function (global) {
  'use strict';

  /* ----------------------------------------------------------------------
     1. STUDIO WEB AUDIO SYNTH (Inspirado en chiptune-synth.ts)
     ---------------------------------------------------------------------- */
  const StudioSFX = (() => {
    let ctx = null;
    let enabled = true;

    function getCtx() {
      if (!enabled) return null;
      if (!ctx) {
        const AC = global.AudioContext || global.webkitAudioContext;
        if (!AC) return null;
        ctx = new AC();
      }
      if (ctx.state === 'suspended') {
        ctx.resume().catch(() => {});
      }
      return ctx;
    }

    function tone(freq, dur, type = 'triangle', gainPeak = 0.032, delay = 0) {
      try {
        const c = getCtx();
        if (!c) return;
        const now = c.currentTime + delay;
        const osc = c.createOscillator();
        const g = c.createGain();
        osc.type = type;
        osc.frequency.setValueAtTime(freq, now);
        g.gain.setValueAtTime(0.0001, now);
        g.gain.exponentialRampToValueAtTime(gainPeak, now + 0.012);
        g.gain.exponentialRampToValueAtTime(0.0001, now + dur);
        osc.connect(g);
        g.connect(c.destination);
        osc.start(now);
        osc.stop(now + dur + 0.02);
      } catch (_) {}
    }

    return {
      isEnabled: () => enabled,
      setEnabled: (v) => { enabled = Boolean(v); if (enabled) tone(587.3, 0.07, 'sine', 0.028); },
      slide: () => tone(440, 0.05, 'sine', 0.02),
      tap: () => tone(660, 0.045, 'triangle', 0.024),
      alert: () => { tone(330, 0.08, 'triangle', 0.028, 0); tone(493.9, 0.09, 'sine', 0.028, 0.06); },
      explode: (open) => {
        if (open) {
          tone(392, 0.08, 'triangle', 0.028, 0);
          tone(523.25, 0.09, 'triangle', 0.028, 0.055);
          tone(659.25, 0.12, 'sine', 0.028, 0.11);
        } else {
          tone(659.25, 0.07, 'triangle', 0.026, 0);
          tone(440, 0.1, 'sine', 0.026, 0.06);
        }
      },
      pass: () => {
        tone(523.25, 0.08, 'triangle', 0.03, 0);
        tone(659.25, 0.08, 'triangle', 0.03, 0.07);
        tone(783.99, 0.12, 'triangle', 0.032, 0.14);
        tone(1046.5, 0.18, 'sine', 0.032, 0.21);
      },
      lock: (locked) => {
        if (locked) {
          tone(220, 0.09, 'sawtooth', 0.02, 0);
          tone(164.8, 0.12, 'triangle', 0.024, 0.06);
        } else {
          tone(493.88, 0.08, 'triangle', 0.026, 0);
          tone(659.25, 0.11, 'sine', 0.028, 0.07);
        }
      }
    };
  })();

  /* ----------------------------------------------------------------------
     2. CORE COLOR, RGBA BUFFER & GEOMETRY HELPERS
     ---------------------------------------------------------------------- */
  const scratchBytes = new Uint8Array(4);
  const scratchU32 = new Uint32Array(scratchBytes.buffer);
  scratchBytes[0] = 1; scratchBytes[1] = 2; scratchBytes[2] = 3; scratchBytes[3] = 4;
  const IS_LE = scratchU32[0] === 0x04030201;

  function packRgba(r, g, b, a = 255) {
    scratchBytes[0] = r; scratchBytes[1] = g; scratchBytes[2] = b; scratchBytes[3] = a;
    return scratchU32[0];
  }

  function extractAlpha(px) {
    return IS_LE ? (px >>> 24) & 0xff : px & 0xff;
  }

  function hexToRgba(hex, a = 255) {
    const clean = hex.startsWith('#') ? hex.slice(1) : hex;
    const full = clean.length === 3 ? `${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}` : clean;
    const num = parseInt(full, 16);
    return [(num >> 16) & 0xff, (num >> 8) & 0xff, num & 0xff, a];
  }

  function hexToU32(hex, a = 255) {
    const [r, g, b, al] = hexToRgba(hex, a);
    return packRgba(r, g, b, al);
  }

  function clampByte(v) { return Math.max(0, Math.min(255, Math.round(v))); }

  const OUTLINE_COLOR = hexToU32('#14111d');

  function makeRamp(baseHex, opts) {
    if (opts && opts.emissive) {
      const lit = hexToU32(baseHex);
      return { top: lit, sideLight: lit, sideMid: lit, sideDark: lit, emissive: true };
    }
    const [r, g, b] = hexToRgba(baseHex);
    return {
      top: packRgba(clampByte(r * 1.07 + 10), clampByte(g * 1.06 + 8), clampByte(b * 1.03 + 4)),
      sideLight: packRgba(clampByte(r * 0.94), clampByte(g * 0.94), clampByte(b * 0.95)),
      sideMid: packRgba(clampByte(r * 0.79 - 2), clampByte(g * 0.81), clampByte(b * 0.87 + 4)),
      sideDark: packRgba(clampByte(r * 0.62 - 4), clampByte(g * 0.65 - 2), clampByte(b * 0.76 + 8))
    };
  }

  function hashVoxel(x, y, z, seed) {
    let h = (x * 374761393 + y * 668265263 + z * 2147483647 + seed * 1597334677) | 0;
    h = ((h ^ (h >>> 13)) * 1274126177) | 0;
    return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
  }

  function rotateOffset(dx, dy, turn, squash) {
    const cos = Math.cos(turn);
    const sin = Math.sin(turn);
    return {
      x: dx * cos - dy * sin,
      y: (dx * sin + dy * cos) * squash
    };
  }

  function voxelToScreen(dx, dy, z, anchorX, anchorY, turn, squash, spacing) {
    const off = rotateOffset(dx, dy, turn, squash);
    return {
      x: anchorX + off.x,
      y: anchorY - Math.round(z * spacing) + off.y
    };
  }

  /* ----------------------------------------------------------------------
     3. VOXEL GRID BUILDER (Full ProyectoYT 2 Primitives)
     ---------------------------------------------------------------------- */
  class VoxelGrid {
    constructor(sx, sy, sz) {
      this.sx = sx; this.sy = sy; this.sz = sz;
      this.data = new Uint8Array(sx * sy * sz);
    }
    inBounds(x, y, z) { return x >= 0 && x < this.sx && y >= 0 && y < this.sy && z >= 0 && z < this.sz; }
    get(x, y, z) {
      if (!this.inBounds(x, y, z)) return 0;
      return this.data[(z * this.sy + y) * this.sx + x];
    }
    set(x, y, z, idx) {
      const ix = Math.round(x), iy = Math.round(y), iz = Math.round(z);
      if (this.inBounds(ix, iy, iz)) this.data[(iz * this.sy + iy) * this.sx + ix] = idx;
      return this;
    }
    applyAt(x, y, z, idx, mode = 'fill') {
      if (!this.inBounds(x, y, z)) return;
      const p = (z * this.sy + y) * this.sx + x;
      if (mode === 'carve') this.data[p] = 0;
      else if (mode === 'paint') { if (this.data[p] !== 0) this.data[p] = idx; }
      else this.data[p] = idx;
    }
    box(x0, y0, z0, x1, y1, z1, idx, mode = 'fill') {
      const ax = Math.max(0, Math.round(Math.min(x0, x1)));
      const bx = Math.min(this.sx - 1, Math.round(Math.max(x0, x1)));
      const ay = Math.max(0, Math.round(Math.min(y0, y1)));
      const by = Math.min(this.sy - 1, Math.round(Math.max(y0, y1)));
      const az = Math.max(0, Math.round(Math.min(z0, z1)));
      const bz = Math.min(this.sz - 1, Math.round(Math.max(z0, z1)));
      for (let z = az; z <= bz; z++)
        for (let y = ay; y <= by; y++)
          for (let x = ax; x <= bx; x++) this.applyAt(x, y, z, idx, mode);
      return this;
    }
    carveBox(x0, y0, z0, x1, y1, z1) {
      return this.box(x0, y0, z0, x1, y1, z1, 0, 'carve');
    }
    cylinder(cx, cy, z0, z1, rx, ry, idx, mode = 'fill') {
      if (rx <= 0 || ry <= 0) return this;
      const az = Math.max(0, Math.round(Math.min(z0, z1)));
      const bz = Math.min(this.sz - 1, Math.round(Math.max(z0, z1)));
      const ax = Math.max(0, Math.floor(cx - rx)), bx = Math.min(this.sx - 1, Math.ceil(cx + rx));
      const ay = Math.max(0, Math.floor(cy - ry)), by = Math.min(this.sy - 1, Math.ceil(cy + ry));
      for (let z = az; z <= bz; z++)
        for (let y = ay; y <= by; y++) {
          const ny = (y - cy) / ry;
          for (let x = ax; x <= bx; x++) {
            const nx = (x - cx) / rx;
            if (nx * nx + ny * ny <= 1.0001) this.applyAt(x, y, z, idx, mode);
          }
        }
      return this;
    }
    carveCylinder(cx, cy, z0, z1, rx, ry) {
      return this.cylinder(cx, cy, z0, z1, rx, ry, 0, 'carve');
    }
    cone(cx, cy, z0, z1, rB, rT, idx, mode = 'fill') {
      const az = Math.max(0, Math.round(Math.min(z0, z1)));
      const bz = Math.min(this.sz - 1, Math.round(Math.max(z0, z1)));
      const span = Math.max(1, bz - az);
      for (let z = az; z <= bz; z++) {
        const t = bz === az ? 0 : (z - az) / span;
        const r = rB + (rT - rB) * t;
        if (r <= 0.15) { this.applyAt(Math.round(cx), Math.round(cy), z, idx, mode); continue; }
        const rSq = r * r + 0.15;
        for (let y = Math.max(0, Math.floor(cy - r)); y <= Math.min(this.sy - 1, Math.ceil(cy + r)); y++) {
          const dy = y - cy;
          for (let x = Math.max(0, Math.floor(cx - r)); x <= Math.min(this.sx - 1, Math.ceil(cx + r)); x++) {
            const dx = x - cx;
            if (dx * dx + dy * dy <= rSq) this.applyAt(x, y, z, idx, mode);
          }
        }
      }
      return this;
    }
    wedge(x0, y0, z0, x1, y1, z1, slopeDir, idx) {
      const ax = Math.max(0, Math.round(Math.min(x0, x1)));
      const bx = Math.min(this.sx - 1, Math.round(Math.max(x0, x1)));
      const ay = Math.max(0, Math.round(Math.min(y0, y1)));
      const by = Math.min(this.sy - 1, Math.round(Math.max(y0, y1)));
      const az = Math.max(0, Math.round(Math.min(z0, z1)));
      const bz = Math.min(this.sz - 1, Math.round(Math.max(z0, z1)));
      const spanX = Math.max(1, bx - ax), spanY = Math.max(1, by - ay), spanZ = bz - az;
      for (let y = ay; y <= by; y++) {
        for (let x = ax; x <= bx; x++) {
          let f = 1;
          if (slopeDir === '+x') f = (x - ax) / spanX;
          else if (slopeDir === '-x') f = (bx - x) / spanX;
          else if (slopeDir === '+y') f = (y - ay) / spanY;
          else if (slopeDir === '-y') f = (by - y) / spanY;
          const topZ = az + Math.round(spanZ * f);
          for (let z = az; z <= topZ; z++) this.applyAt(x, y, z, idx, 'fill');
        }
      }
      return this;
    }
    dither(targetIdx, altIdx, prob, seed, zRange) {
      const zMin = zRange ? Math.max(0, zRange[0]) : 0;
      const zMax = zRange ? Math.min(this.sz - 1, zRange[1]) : this.sz - 1;
      for (let z = zMin; z <= zMax; z++) {
        for (let y = 0; y < this.sy; y++) {
          const off = (z * this.sy + y) * this.sx;
          for (let x = 0; x < this.sx; x++) {
            if (this.data[off + x] === targetIdx && hashVoxel(x, y, z, seed) < prob) {
              this.data[off + x] = altIdx;
            }
          }
        }
      }
      return this;
    }
  }

  /* ----------------------------------------------------------------------
     4. SLICER & PIXEL-STACK RENDERER
     ---------------------------------------------------------------------- */
  function createBuffer(width, height) {
    const data = new Uint8ClampedArray(width * height * 4);
    return { width, height, data, u32: new Uint32Array(data.buffer) };
  }

  function bakeSliceSheet(grid, palette) {
    const { sx, sy, sz } = grid;
    const width = sx * sz;
    const data = new Uint8ClampedArray(width * sy * 4);
    const u32 = new Uint32Array(data.buffer);

    for (let z = 0; z < sz; z++) {
      const xOff = z * sx;
      for (let y = 0; y < sy; y++) {
        const rowOff = y * width + xOff;
        for (let x = 0; x < sx; x++) {
          const idx = grid.get(x, y, z);
          if (!idx) continue;
          const ramp = palette[idx];
          if (!ramp) continue;
          if (ramp.emissive) { u32[rowOff + x] = ramp.top; continue; }
          if (grid.get(x, y, z + 1) === 0) {
            const shadow = grid.get(x - 1, y, z + 1) !== 0 || grid.get(x, y - 1, z + 1) !== 0;
            u32[rowOff + x] = shadow ? ramp.sideLight : ramp.top;
            continue;
          }
          const nx = (grid.get(x + 1, y, z) === 0 ? 1 : 0) - (grid.get(x - 1, y, z) === 0 ? 1 : 0);
          const ny = (grid.get(x, y + 1, z) === 0 ? 1 : 0) - (grid.get(x, y - 1, z) === 0 ? 1 : 0);
          if (nx === 0 && ny === 0) { u32[rowOff + x] = ramp.sideMid; continue; }
          const cx = nx / Math.hypot(nx, ny);
          u32[rowOff + x] = cx > 0.45 ? ramp.sideLight : (cx < -0.45 ? ramp.sideDark : ramp.sideMid);
        }
      }
    }

    const sliceBounds = new Array(sz);
    for (let z = 0; z < sz; z++) {
      const xOff = z * sx;
      let minX = sx, minY = sy, maxX = -1, maxY = -1;
      for (let y = 0; y < sy; y++) {
        const rs = y * width + xOff;
        for (let x = 0; x < sx; x++) {
          if (extractAlpha(u32[rs + x]) > 0) {
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
          }
        }
      }
      sliceBounds[z] = maxX < minX ? { empty: true } : { minX, minY, maxX, maxY, empty: false };
    }
    return { width, height: sy, sliceWidth: sx, sliceHeight: sy, count: sz, data, u32, sliceBounds };
  }

  function snapZero(n) { return Math.abs(n) < 1e-12 ? 0 : n; }

  function drawStack(target, sheet, pos, turn, squash = 0.75, spacing = 1) {
    const { count, sliceWidth: w, sliceHeight: h, width: sheetW, u32: srcU32, sliceBounds } = sheet;
    const px = Math.round(pos.x), py = Math.round(pos.y);
    const cos = snapZero(Math.cos(turn)), sin = snapZero(Math.sin(turn));
    const halfW = w / 2, halfH = h / 2;
    const dstW = target.width, dstH = target.height, dstU32 = target.u32;

    for (let z = 0; z < count; z++) {
      const sb = sliceBounds[z];
      if (sb.empty) continue;
      const ay = py - Math.round(z * spacing);
      const lx0 = sb.minX - halfW, lx1 = sb.maxX + 1 - halfW;
      const ly0 = sb.minY - halfH, ly1 = sb.maxY + 1 - halfH;
      const c0x = lx0 * cos - ly0 * sin, c0y = (lx0 * sin + ly0 * cos) * squash;
      const c1x = lx1 * cos - ly0 * sin, c1y = (lx1 * sin + ly0 * cos) * squash;
      const c2x = lx0 * cos - ly1 * sin, c2y = (lx0 * sin + ly1 * cos) * squash;
      const c3x = lx1 * cos - ly1 * sin, c3y = (lx1 * sin + ly1 * cos) * squash;

      const sxMin = Math.max(0, Math.floor(px + Math.min(c0x, c1x, c2x, c3x)) - 1);
      const sxMax = Math.min(dstW - 1, Math.ceil(px + Math.max(c0x, c1x, c2x, c3x)) + 1);
      const syMin = Math.max(0, Math.floor(ay + Math.min(c0y, c1y, c2y, c3y)) - 1);
      const syMax = Math.min(dstH - 1, Math.ceil(ay + Math.max(c0y, c1y, c2y, c3y)) + 1);
      if (sxMin > sxMax || syMin > syMax) continue;

      const xOff = z * w;
      for (let y = syMin; y <= syMax; y++) {
        const ry = (y + 0.5 - ay) / squash;
        const ryS = ry * sin + halfW + 1e-9;
        const ryC = ry * cos + halfH + 1e-9;
        const dstRow = y * dstW;
        for (let x = sxMin; x <= sxMax; x++) {
          const tx = x + 0.5 - px;
          const u = Math.floor(tx * cos + ryS);
          if (u < 0 || u >= w) continue;
          const v = Math.floor(-tx * sin + ryC);
          if (v < 0 || v >= h) continue;
          const srcPx = srcU32[v * sheetW + (xOff + u)];
          if (srcPx !== 0) dstU32[dstRow + x] = srcPx;
        }
      }
    }
  }

  function drawScene(target, items, squash = 0.75, defaultSpacing = 1) {
    if (!items.length) return;
    const sorted = items
      .map((item, idx) => ({ item, idx, gy: item.groundY ?? item.pos.y }))
      .sort((a, b) => (a.gy !== b.gy ? a.gy - b.gy : a.idx - b.idx));
    for (let i = 0; i < sorted.length; i++) {
      const { item } = sorted[i];
      const sp = item.spacing ?? defaultSpacing;
      const drawY = Math.round(item.pos.y) - Math.round((item.zOffset || 0) * sp);
      drawStack(target, item.sheet, { x: Math.round(item.pos.x), y: drawY }, item.turn, squash, sp);
    }
  }

  function applyOutline(src, dst) {
    const w = src.width, h = src.height, sU32 = src.u32, dU32 = dst.u32, sd = src.data;
    for (let y = 0; y < h; y++) {
      const row = y * w;
      for (let x = 0; x < w; x++) {
        const idx = row + x;
        if (sd[idx * 4 + 3] >= 128) { dU32[idx] = sU32[idx]; continue; }
        const n =
          (x > 0 && sd[(idx - 1) * 4 + 3] >= 128) ||
          (x + 1 < w && sd[(idx + 1) * 4 + 3] >= 128) ||
          (y > 0 && sd[(idx - w) * 4 + 3] >= 128) ||
          (y + 1 < h && sd[(idx + w) * 4 + 3] >= 128);
        dU32[idx] = n ? OUTLINE_COLOR : 0;
      }
    }
  }

  function blendPixel(target, x, y, r, g, b, a) {
    const ix = Math.round(x), iy = Math.round(y);
    if (ix < 0 || ix >= target.width || iy < 0 || iy >= target.height || a <= 0) return;
    const db4 = (iy * target.width + ix) * 4;
    const d = target.data;
    const dstA = d[db4 + 3];
    if (a >= 255 || dstA === 0) {
      d[db4] = r; d[db4 + 1] = g; d[db4 + 2] = b; d[db4 + 3] = Math.min(255, a);
      return;
    }
    const invA = 255 - a;
    d[db4] = (r * a + d[db4] * invA + 127) >> 8;
    d[db4 + 1] = (g * a + d[db4 + 1] * invA + 127) >> 8;
    d[db4 + 2] = (b * a + d[db4 + 2] * invA + 127) >> 8;
    d[db4 + 3] = Math.min(255, a + ((dstA * invA + 127) >> 8));
  }

  function drawLightBeam(target, ox, oy, angle, length, halfSpread, squash, r, g, b, maxAlpha) {
    const ix = Math.round(ox), iy = Math.round(oy);
    const cosA = Math.cos(angle), sinA = Math.sin(angle), tanH = Math.tan(halfSpread);
    const rx = Math.ceil(length) + 2, ry = Math.ceil(length * squash) + 2;
    const minX = Math.max(0, ix - rx), maxX = Math.min(target.width - 1, ix + rx);
    const minY = Math.max(0, iy - ry), maxY = Math.min(target.height - 1, iy + ry);
    for (let py = minY; py <= maxY; py++) {
      const wy = (py - oy) / squash;
      for (let px = minX; px <= maxX; px++) {
        const wx = px - ox;
        const along = wx * cosA + wy * sinA;
        if (along <= 1 || along > length) continue;
        const perp = Math.abs(-wx * sinA + wy * cosA);
        const maxPerp = along * tanH + 0.7;
        if (perp > maxPerp) continue;
        const al = Math.round(maxAlpha * (1 - (along / length) * 0.78) * (1 - Math.min(1, perp / maxPerp) * 0.45));
        if (al > 4) blendPixel(target, px, py, r, g, b, al);
      }
    }
  }

  /* ----------------------------------------------------------------------
     5. 3 HERO DIORAMAS DETALLADOS (Nivel Studio ProyectoYT 2)
     ---------------------------------------------------------------------- */

  // 1) CASA INTELIGENTE A LA MANO (Slide 1 Hero, 38x38x34)
  function buildCasaDiorama() {
    const pal = {
      1: makeRamp('#64748b'), // acera piedra
      2: makeRamp('#475569'), // sombra adoquines
      3: makeRamp('#3b9c54'), // prado medellin
      4: makeRamp('#2d7a40'), // dither cesped
      5: makeRamp('#f3efe6'), // muro estuco blanco calido
      6: makeRamp('#e2d9c8'), // ladrillo detalle
      7: makeRamp('#d93838'), // techo carmesi A La Mano
      8: makeRamp('#8f1426'), // cumbrera oscura
      9: makeRamp('#334155'), // puerta / chimenea
      10: makeRamp('#ffe375', { emissive: true }), // luz interior calida
      11: makeRamp('#f59e0b'), // caja herramientas tecnico
      12: makeRamp('#38bdf8', { emissive: true }), // drone / parabrisas cyan
      13: makeRamp('#1e293b') // llantas van
    };
    const g = new VoxelGrid(38, 38, 34);
    const cx = 18.5, cy = 18.5;

    // Base piedra dither + cesped dither
    g.cylinder(cx, cy, 0, 1, 17, 17, 1).dither(1, 2, 0.22, 101, [0, 1]);
    g.cylinder(cx, cy, 2, 2, 15.5, 15.5, 3).dither(3, 4, 0.2, 202, [2, 2]);
    g.box(14, 22, 2, 19, 34, 2, 1).dither(1, 2, 0.16, 303, [2, 2]); // sendero acceso

    // Casa principal ahuecada con interior luminoso
    g.box(9, 8, 3, 24, 21, 13, 5).carveBox(11, 10, 4, 22, 19, 11);
    g.box(12, 10, 4, 21, 12, 10, 10); // pared luminosa interior visible por puerta/ventanas
    // Zocalo ladrillo y columnas
    g.box(9, 8, 3, 24, 21, 4, 6);
    g.carveBox(15, 20, 4, 18, 21, 9); // vano puerta frontal
    g.box(16, 20, 4, 17, 20, 9, 9); // hoja puerta semiabierta
    // Ventanas calidas
    g.box(11, 21, 7, 13, 21, 9, 10);
    g.box(20, 21, 7, 22, 21, 9, 10);
    g.box(9, 13, 7, 9, 16, 9, 10);
    g.box(24, 13, 7, 24, 16, 9, 10);

    // Techo dos aguas A La Mano carmesi + buhardilla + chimenea
    g.wedge(8, 7, 14, 25, 14, 21, '+y', 7);
    g.wedge(8, 15, 14, 25, 22, 21, '-y', 7);
    g.box(8, 14, 20, 25, 15, 21, 8);
    g.box(20, 10, 16, 22, 12, 25, 9); // chimenea ladrillo oscuro

    // Caja herramientas tecnico junto a entrada
    g.box(10, 23, 3, 13, 26, 5, 11);
    g.box(11, 24, 6, 12, 25, 6, 7);

    // Pequeña camioneta blanca de servicio A La Mano estacionada frente derecho
    g.box(21, 24, 3, 27, 29, 7, 5);
    g.box(21, 25, 6, 23, 28, 7, 12); // parabrisas
    g.box(22, 25, 8, 26, 28, 8, 7); // franja techo carmesi
    g.set(22, 24, 3, 13).set(26, 24, 3, 13).set(22, 29, 3, 13).set(26, 29, 3, 13);

    const houseSheet = bakeSliceSheet(g, pal);

    // Drone Cloud Railway orbitando arriba
    const dg = new VoxelGrid(11, 11, 6);
    dg.box(4, 4, 2, 6, 6, 4, 9);
    dg.box(5, 5, 1, 5, 5, 3, 12);
    dg.set(2, 2, 3, 7).set(8, 2, 3, 7).set(2, 8, 3, 7).set(8, 8, 3, 7);
    const droneSheet = bakeSliceSheet(dg, pal);

    return {
      sliceCount: 34,
      renderMain(target, p) {
        const angle = p.time * 1.15;
        const dronePos = {
          x: p.anchorX + Math.cos(angle) * 32,
          y: p.anchorY - Math.round(28 * p.spacing) + Math.sin(angle) * 16 * p.squash + Math.sin(p.time * 3.2) * 2
        };
        drawScene(target, [
          { sheet: houseSheet, pos: { x: p.anchorX, y: p.anchorY }, turn: p.turn, groundY: p.anchorY },
          { sheet: droneSheet, pos: dronePos, turn: -angle, groundY: dronePos.y }
        ], p.squash, p.spacing);
      },
      renderOverlay(target, p) {
        // Humo suave de chimenea
        const chim = voxelToScreen(2.5, -7.5, 25, p.anchorX, p.anchorY, p.turn, p.squash, p.spacing);
        for (let i = 0; i < 6; i++) {
          const life = (p.time * 0.45 + i / 6) % 1;
          const sx = chim.x + Math.sin(p.time * 2.5 + i * 1.3) * 3;
          const sy = chim.y - life * 16;
          const al = Math.round(185 * (1 - life));
          blendPixel(target, sx, sy, 235, 240, 248, al);
          blendPixel(target, sx + 1, sy, 215, 225, 240, Math.round(al * 0.7));
        }
      }
    };
  }

  // 2) FARO-BÓVEDA DE SEGURIDAD HMAC (Slide 7, inspirado en lighthouse.ts, 36x36x36)
  function buildFaroDiorama() {
    const pal = {
      1: makeRamp('#334155'), // mar / perimetro oscuro
      2: makeRamp('#1e293b'), // dither perimetro
      3: makeRamp('#64748b'), // roca islote
      4: makeRamp('#475569'), // sombra roca
      5: makeRamp('#e2e8f0'), // torre blindada blanca
      6: makeRamp('#e11d48'), // bandas carmesi A La Mano
      7: makeRamp('#0f172a'), // hierro galeria y boveda
      8: makeRamp('#34d399', { emissive: true }), // faro HMAC verde esmeralda
      9: makeRamp('#fde047', { emissive: true }), // cerradura frontal oro
      10: makeRamp('#94a3b8') // acero boveda frontal
    };
    const g = new VoxelGrid(36, 36, 36);
    const cx = 17.5, cy = 17.5;

    g.cylinder(cx, cy, 0, 1, 15.5, 15.5, 1).dither(1, 2, 0.25, 404, [0, 1]);
    g.cone(cx, cy, 2, 5, 11.5, 8.8, 3).dither(3, 4, 0.28, 505, [2, 5]);

    // Boveda blindada anexa
    g.box(10, 17, 5, 24, 25, 12, 10);
    g.box(15, 25, 6, 19, 25, 10, 9); // cerradura HMAC iluminada
    g.box(16, 25, 7, 18, 25, 9, 7);

    // Torre faro con bandas anchas de alto contraste
    g.cone(cx, cy, 6, 11, 5.2, 4.6, 5);
    g.cone(cx, cy, 12, 16, 4.5, 4.0, 6);
    g.cone(cx, cy, 17, 21, 3.9, 3.4, 5);
    g.cone(cx, cy, 22, 25, 3.4, 3.0, 6);

    // Galeria de hierro + camara esmeralda + cupula
    g.cylinder(cx, cy, 26, 26, 4.4, 4.4, 7);
    g.cylinder(cx, cy, 27, 27, 4.4, 4.4, 7).carveCylinder(cx, cy, 27, 27, 3.3, 3.3);
    g.cylinder(cx, cy, 27, 30, 2.5, 2.5, 8);
    g.set(cx - 2, cy - 2, 28, 7).set(cx + 2, cy - 2, 28, 7).set(cx - 2, cy + 2, 28, 7).set(cx + 2, cy + 2, 28, 7);
    g.cone(cx, cy, 31, 35, 3.4, 0.7, 6);

    const sheet = bakeSliceSheet(g, pal);
    let flashTimer = 0;

    return {
      sliceCount: 36,
      flashShield: () => { flashTimer = 0.9; },
      renderMain(target, p) {
        if (flashTimer > 0) flashTimer = Math.max(0, flashTimer - 0.02);
        drawStack(target, sheet, { x: p.anchorX, y: p.anchorY }, p.turn, p.squash, p.spacing);
      },
      renderOverlay(target, p) {
        const lanternY = p.anchorY - Math.round(28.5 * p.spacing);
        const beamAngle = p.turn + p.time * 1.1;
        const alpha = flashTimer > 0 ? 140 : 90;
        drawLightBeam(target, p.anchorX, lanternY, beamAngle, 58, 0.25, p.squash, 52, 211, 153, alpha);
        if (flashTimer > 0) {
          const r = 18 + (1 - flashTimer) * 32;
          const pts = 24;
          const al = Math.round(flashTimer * 220);
          for (let i = 0; i < pts; i++) {
            const ang = (i / pts) * Math.PI * 2;
            blendPixel(target, p.anchorX + Math.cos(ang) * r, (p.anchorY - 14 * p.spacing) + Math.sin(ang) * r * p.squash, 52, 211, 153, al);
          }
        }
      }
    };
  }

  // 3) COHETE RELEASE V2 — 3 DE DICIEMBRE (Fiel a rocket.ts de ProyectoYT 2, 39x39x40)
  function buildCoheteDiorama() {
    const pal = {
      1: makeRamp('#7b828c'), // concreto pad
      2: makeRamp('#334155'), // foso quemado
      3: makeRamp('#b84539'), // torre acero rojo
      4: makeRamp('#eff3f8'), // fuselaje plata blanco
      5: makeRamp('#e11d48'), // ojiva y aletas carmesi
      6: makeRamp('#44d8f2', { emissive: true }), // escotilla cyan
      7: makeRamp('#2f343d'), // tobera
      8: makeRamp('#ff6c24', { emissive: true }), // llama exterior
      9: makeRamp('#ffe866', { emissive: true })  // nucleo llama
    };
    const g = new VoxelGrid(39, 39, 40);
    const cx = 19, cy = 19;

    g.box(5, 5, 0, 33, 33, 2, 1).carveBox(cx - 3, cy - 3, 1, cx + 3, cy + 14, 2);
    g.box(cx - 3, cy - 3, 0, cx + 3, cy + 14, 0, 2);

    // Torre lanzamiento acero
    g.box(8, 8, 3, 12, 12, 32, 3).carveBox(9, 9, 4, 11, 11, 30);
    for (let gz = 6; gz <= 30; gz += 6) g.box(8, 8, gz, 12, 12, gz, 3);
    g.box(12, 11, 24, cx - 3, cy - 2, 25, 3);

    // 4 aletas + tobera + fuselaje + ojiva
    g.box(cx - 6, cy, 5, cx + 6, cy, 11, 5).box(cx, cy - 6, 5, cx, cy + 6, 11, 5);
    g.carveBox(cx - 6, cy - 6, 9, cx - 5, cy + 6, 11).carveBox(cx + 5, cy - 6, 9, cx + 6, cy + 6, 11);
    g.cone(cx, cy, 6, 8, 2.5, 1.8, 7);
    g.cylinder(cx, cy, 9, 27, 4.0, 4.0, 4);
    g.cylinder(cx, cy, 15, 17, 4.05, 4.05, 5); // franja carmesi
    g.box(cx - 1, cy + 4, 19, cx + 1, cy + 4, 21, 6); // escotilla
    g.cone(cx, cy, 28, 37, 4.0, 0.5, 5);

    const mainSheet = bakeSliceSheet(g, pal);
    const flameFrames = [0, 1, 2].map((f) => {
      const fg = new VoxelGrid(7, 7, 6);
      fg.cone(3, 3, 1 - (f % 2), 5, 0.6, 2.1, 8).box(3, 3, 3, 3, 3, 5, 9);
      return bakeSliceSheet(fg, pal);
    });

    return {
      sliceCount: 40,
      renderMain(target, p) {
        const f = Math.floor(p.time * 14) % flameFrames.length;
        const rumble = Math.round(Math.sin(p.time * 28) * 0.6);
        drawScene(target, [
          { sheet: mainSheet, pos: { x: p.anchorX, y: p.anchorY }, turn: p.turn, groundY: p.anchorY },
          { sheet: flameFrames[f], pos: { x: p.anchorX, y: p.anchorY }, turn: p.turn, zOffset: 1 + rumble, groundY: p.anchorY + 0.1 }
        ], p.squash, p.spacing);
      },
      renderOverlay(target, p) {
        for (let i = 0; i < 8; i++) {
          const life = (p.time * 0.65 + i / 8) % 1;
          const st = voxelToScreen((i % 3 - 1) * 2.5, 3 + life * 11, 2 + life * 2.5, p.anchorX, p.anchorY, p.turn, p.squash, p.spacing);
          const al = Math.round(190 * (1 - life));
          blendPixel(target, st.x, st.y, 240, 244, 250, al);
          blendPixel(target, st.x + 1, st.y, 215, 224, 236, Math.round(al * 0.75));
        }
      }
    };
  }

  const VOXEL_BUILDERS = {
    casa: buildCasaDiorama,
    faro: buildFaroDiorama,
    cohete: buildCoheteDiorama
  };

  /* ----------------------------------------------------------------------
     6. MOUNT & RAF LOOP (Ticks only for visible active slide)
     ---------------------------------------------------------------------- */
  const mountedWidgets = [];

  function mountVoxelCanvas(opts) {
    const canvas = document.getElementById(opts.canvasId);
    const explodeBtn = opts.explodeBtnId ? document.getElementById(opts.explodeBtnId) : null;
    if (!canvas) return null;

    const ctx = canvas.getContext('2d');
    const bufW = opts.bufW || 148;
    const bufH = opts.bufH || 164;
    const sceneBuf = createBuffer(bufW, bufH);
    const outlinedBuf = createBuffer(bufW, bufH);

    const off = document.createElement('canvas');
    off.width = bufW;
    off.height = bufH;
    const offCtx = off.getContext('2d');
    const imgData = offCtx.createImageData(bufW, bufH);

    const model = VOXEL_BUILDERS[opts.model]();
    const state = {
      turn: opts.initialTurn || 0.52,
      vel: 0.4,
      spacing: 1,
      slicesMode: false,
      dragging: false,
      lastX: 0,
      lastTime: 0,
      time: 0
    };

    function syncSize() {
      const rect = canvas.getBoundingClientRect();
      if (rect.width < 10 || rect.height < 10) return;
      const dpr = Math.min(global.devicePixelRatio || 1, 2);
      const w = Math.round(rect.width * dpr);
      const h = Math.round(rect.height * dpr);
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }
    }

    function toggleExplode() {
      state.slicesMode = !state.slicesMode;
      if (explodeBtn) explodeBtn.classList.toggle('active', state.slicesMode);
      StudioSFX.explode(state.slicesMode);
    }

    if (explodeBtn) explodeBtn.addEventListener('click', toggleExplode);

    canvas.addEventListener('pointerdown', (e) => {
      state.dragging = true;
      state.lastX = e.clientX;
      state.lastTime = performance.now();
      state.vel = 0;
      canvas.setPointerCapture(e.pointerId);
    });

    canvas.addEventListener('pointermove', (e) => {
      if (!state.dragging) return;
      const now = performance.now();
      const dt = Math.max(0.008, (now - state.lastTime) / 1000);
      const dx = e.clientX - state.lastX;
      const dTurn = dx * 0.015;
      state.turn += dTurn;
      state.vel = dTurn / dt;
      state.lastX = e.clientX;
      state.lastTime = now;
    });

    canvas.addEventListener('pointerup', () => { state.dragging = false; });
    canvas.addEventListener('pointercancel', () => { state.dragging = false; });
    canvas.addEventListener('dblclick', toggleExplode);

    const widget = {
      slideIndex: opts.slideIndex,
      model,
      state,
      renderTick: (dt, currentSlideIdx) => {
        if (currentSlideIdx !== opts.slideIndex) return;
        syncSize();
        if (canvas.width === 0 || canvas.height === 0) return;

        state.time += dt;
        if (!state.dragging) {
          state.turn += state.vel * dt;
          state.vel += (0.4 - state.vel) * Math.min(1, dt * 3.2);
        }
        const targetSp = state.slicesMode ? 1.95 : 1.0;
        state.spacing += (targetSp - state.spacing) * Math.min(1, dt * 9);

        sceneBuf.u32.fill(0);
        const anchorX = Math.round(bufW / 2);
        const anchorY = Math.round(bufH / 2 + ((model.sliceCount - 1) * state.spacing) * 0.42);
        const params = { anchorX, anchorY, turn: state.turn, squash: 0.75, spacing: state.spacing, time: state.time };
        model.renderMain(sceneBuf, params);
        applyOutline(sceneBuf, outlinedBuf);
        if (model.renderOverlay) model.renderOverlay(outlinedBuf, params);

        imgData.data.set(outlinedBuf.data);
        offCtx.putImageData(imgData, 0, 0);

        ctx.clearRect(0, 0, canvas.width, canvas.height);

        const grad = ctx.createRadialGradient(canvas.width / 2, canvas.height * 0.56, 8, canvas.width / 2, canvas.height * 0.56, canvas.width * 0.46);
        grad.addColorStop(0, opts.glowColor || 'rgba(225, 29, 72, 0.24)');
        grad.addColorStop(1, 'rgba(6, 10, 20, 0)');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        const fitScale = Math.max(1, Math.floor(Math.min(canvas.width / 74, canvas.height / (state.slicesMode ? 78 : 54))));
        const dw = bufW * fitScale;
        const dh = bufH * fitScale;
        const dx = Math.round((canvas.width - dw) / 2);
        const dy = Math.round((canvas.height - dh) / 2);

        ctx.imageSmoothingEnabled = false;
        ctx.drawImage(off, dx, dy, dw, dh);
      }
    };

    mountedWidgets.push(widget);
    return widget;
  }

  let loopRunning = false;
  function startVoxelLoop(getActiveSlideIdx) {
    if (loopRunning) return;
    loopRunning = true;
    let last = performance.now();
    function tick(now) {
      const dt = Math.min(0.05, Math.max(0.001, (now - last) / 1000));
      last = now;
      const cur = getActiveSlideIdx();
      for (let i = 0; i < mountedWidgets.length; i++) {
        mountedWidgets[i].renderTick(dt, cur);
      }
      requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }

  global.VoxelFX = { StudioSFX, mountVoxelCanvas, startVoxelLoop };
})(window);
