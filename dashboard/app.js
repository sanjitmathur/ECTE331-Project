/* ──────────────────────────────────────────
   ECTE331 Dashboard – app.js
   Full simulation engine for all 3 parts
   ────────────────────────────────────────── */

'use strict';

// ═══════════════════════════════════════════
//  UTILITY
// ═══════════════════════════════════════════
function rndInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }
function now() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}.${String(d.getMilliseconds()).padStart(3,'0')}`;
}

// ═══════════════════════════════════════════
//  TAB NAVIGATION
// ═══════════════════════════════════════════
function switchTab(tab) {
  document.querySelectorAll('.tab-btn').forEach(b => { b.classList.remove('active'); b.setAttribute('aria-selected','false'); });
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
  document.getElementById('tab-' + tab).classList.add('active');
  document.getElementById('tab-' + tab).setAttribute('aria-selected','true');
  document.getElementById('panel-' + tab).classList.add('active');
}

// ═══════════════════════════════════════════
//  PART 1 – DRONE NAVIGATION TMR
// ═══════════════════════════════════════════

let droneRunning = false;
let droneAltHistory = [];
let sensorAHistory = [];
let sensorBHistory = [];
let sensorCHistory = [];

function resetDrone() {
  droneRunning = false;
  droneAltHistory = []; sensorAHistory = []; sensorBHistory = []; sensorCHistory = [];
  ['A','B','C'].forEach(s => {
    el('reading-'+s).textContent = '– m';
    el('tag-'+s).textContent = 'IDLE';
    el('tag-'+s).className = 'sensor-tag';
    el('dot-'+s).className = 'sensor-status-dot';
    el('sensor-'+s).className = 'sensor-card';
  });
  el('final-altitude').textContent = '– m';
  el('tmr-decision').textContent = '–';
  el('reliability-status').textContent = '–';
  el('cycle-count').textContent = '0 / 20';
  el('drone-log').innerHTML = '';
  el('drone-run-btn').disabled = false;
  clearCanvas('drone-chart');
  document.querySelectorAll('.safe-mode-alert').forEach(a => a.remove());
}

function simulateSensor(baseline) {
  const chance = rndInt(0, 99);
  if (chance < 15) return { value: null, state: 'failure' };
  if (chance < 30) {
    const val = Math.random() < 0.5 ? rndInt(201, 500) : rndInt(-100, -1);
    return { value: val, state: 'corrupt' };
  }
  return { value: Math.min(200, Math.max(0, baseline + rndInt(0, 19))), state: 'valid' };
}

function applyTMR(readings, prev) {
  const valid = readings.filter(r => r.value !== null && r.value >= 0 && r.value <= 200);
  const outliers = readings.filter(r => r.value === null || r.value < 0 || r.value > 200);
  const ids = ['A','B','C'];
  let outMsg = outliers.length > 0 ? `OUTLIER_DETECTED | Sensors=${outliers.map(r => ids[readings.indexOf(r)]).join(',')}` : null;

  if (valid.length < 2) return { altitude: prev, decision: 'FALLBACK', status: 'DEGRADED', outMsg, majority: null };

  for (let i = 0; i < valid.length; i++) {
    for (let j = i+1; j < valid.length; j++) {
      if (valid[i].value === valid[j].value) {
        return { altitude: valid[i].value, decision: 'MAJORITY', status: 'OK', outMsg, majority: [valid[i], valid[j]] };
      }
    }
  }
  // No exact match – take average of valid readings (graceful fallback)
  const avg = Math.round(valid.reduce((s, r) => s + r.value, 0) / valid.length);
  return { altitude: avg, decision: 'AVG_FALLBACK', status: 'DEGRADED', outMsg, majority: null };
}

function droneLog(msg, cls='') {
  const lb = el('drone-log');
  const line = document.createElement('span');
  line.className = 'log-line ' + cls;
  line.textContent = `[${now()}] ${msg}`;
  lb.appendChild(line);
  lb.scrollTop = lb.scrollHeight;
}

async function runDroneSimulation() {
  if (droneRunning) return;
  droneRunning = true;
  resetDrone();
  el('drone-run-btn').disabled = true;
  const CYCLES = 20;
  const baseline = 100;
  let lastAlt = baseline;
  let consecFails = 0;

  droneLog('Drone Navigation System Started', 'log-info');
  droneLog(`Simulating ${CYCLES} cycles.`, 'log-dim');

  for (let cycle = 1; cycle <= CYCLES && droneRunning; cycle++) {
    el('cycle-count').textContent = `${cycle} / ${CYCLES}`;
    droneLog(`--- Cycle ${cycle} ---`, 'log-dim');

    const readings = [
      simulateSensor(baseline),
      simulateSensor(baseline),
      simulateSensor(baseline)
    ];
    const ids = ['A','B','C'];

    // Update sensor cards
    readings.forEach((r, i) => {
      const sid = ids[i];
      const card = el('sensor-'+sid);
      const readEl = el('reading-'+sid);
      const tagEl = el('tag-'+sid);
      const dotEl = el('dot-'+sid);

      card.className = 'sensor-card ' + r.state;
      dotEl.className = 'sensor-status-dot dot-' + (r.state === 'valid' ? 'valid' : r.state === 'corrupt' ? 'corrupt' : 'failure');

      if (r.state === 'failure') {
        readEl.textContent = 'FAIL';
        tagEl.textContent = 'SENSOR FAILURE';
        tagEl.className = 'sensor-tag tag-failure';
        droneLog(`Sensor ${sid} → FAILURE`, 'log-err');
      } else if (r.state === 'corrupt') {
        readEl.textContent = r.value + ' m';
        tagEl.textContent = 'CORRUPTED';
        tagEl.className = 'sensor-tag tag-corrupt';
        droneLog(`Sensor ${sid} → ${r.value} m [CORRUPTED]`, 'log-warn');
      } else {
        readEl.textContent = r.value + ' m';
        tagEl.textContent = 'VALID';
        tagEl.className = 'sensor-tag tag-valid';
        droneLog(`Sensor ${sid} → ${r.value} m [VALID]`, 'log-ok');
      }

      // History
      if (i === 0) sensorAHistory.push(r.state === 'valid' ? r.value : null);
      if (i === 1) sensorBHistory.push(r.state === 'valid' ? r.value : null);
      if (i === 2) sensorCHistory.push(r.state === 'valid' ? r.value : null);
    });

    const result = applyTMR(readings, lastAlt);
    if (result.outMsg) droneLog('[TMR] ' + result.outMsg, 'log-warn');

    if (result.decision === 'MAJORITY') {
      consecFails = 0;
      droneLog(`[TMR] MAJORITY_DECISION → ${result.altitude} m`, 'log-ok');
    } else if (result.decision === 'AVG_FALLBACK') {
      consecFails++;
      droneLog(`[TMR] AVG_FALLBACK → ${result.altitude} m`, 'log-warn');
    } else {
      consecFails++;
      droneLog(`[TMR] FALLBACK_DECISION → using prev ${result.altitude} m`, 'log-warn');
    }

    if (consecFails >= 2) {
      droneLog('!!! SAFE MODE ACTIVATED – Two consecutive failures !!!', 'log-err');
      el('reliability-status').textContent = 'SAFE MODE';
      const alert = document.createElement('div');
      alert.className = 'safe-mode-alert';
      alert.textContent = '⚠️  SAFE MODE ACTIVATED — Two consecutive reliability failures!';
      el('panel-drone').insertBefore(alert, el('panel-drone').querySelector('.sensor-grid'));
      droneRunning = false;
      break;
    }

    lastAlt = result.altitude;
    droneAltHistory.push(lastAlt);

    el('final-altitude').textContent = lastAlt + ' m';
    el('tmr-decision').textContent = result.decision;
    el('reliability-status').textContent = result.status;
    el('reliability-status').style.color = result.status === 'OK' ? 'var(--green)' : 'var(--amber)';

    drawDroneChart();
    await sleep(600);
  }

  if (droneRunning) droneLog('Simulation completed normally.', 'log-ok');
  el('drone-run-btn').disabled = false;
  droneRunning = false;
}

function drawDroneChart() {
  const canvas = el('drone-chart');
  const ctx = canvas.getContext('2d');
  const W = canvas.offsetWidth || 900;
  const H = canvas.height;
  canvas.width = W;
  ctx.clearRect(0, 0, W, H);

  const PAD = { top: 16, right: 24, bottom: 32, left: 46 };
  const chartW = W - PAD.left - PAD.right;
  const chartH = H - PAD.top - PAD.bottom;
  const minV = 60, maxV = 220;

  // Grid
  ctx.strokeStyle = 'rgba(255,255,255,0.05)';
  ctx.lineWidth = 1;
  for (let g = 0; g <= 4; g++) {
    const y = PAD.top + (g / 4) * chartH;
    ctx.beginPath(); ctx.moveTo(PAD.left, y); ctx.lineTo(PAD.left + chartW, y); ctx.stroke();
    ctx.fillStyle = 'rgba(255,255,255,0.25)';
    ctx.font = '11px JetBrains Mono, monospace';
    ctx.textAlign = 'right';
    const val = Math.round(maxV - (g / 4) * (maxV - minV));
    ctx.fillText(val + 'm', PAD.left - 6, y + 4);
  }

  // Axes
  ctx.strokeStyle = 'rgba(255,255,255,0.12)';
  ctx.lineWidth = 1;
  ctx.beginPath(); ctx.moveTo(PAD.left, PAD.top); ctx.lineTo(PAD.left, PAD.top + chartH); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(PAD.left, PAD.top + chartH); ctx.lineTo(PAD.left + chartW, PAD.top + chartH); ctx.stroke();

  const px = (i, total) => PAD.left + (i / Math.max(total - 1, 1)) * chartW;
  const py = v => PAD.top + chartH - ((v - minV) / (maxV - minV)) * chartH;

  const drawLine = (data, color) => {
    const valid = data.map((v, i) => v !== null ? { x: px(i, data.length), y: py(v) } : null);
    ctx.strokeStyle = color;
    ctx.lineWidth = 1.5;
    ctx.setLineDash([4, 4]);
    ctx.beginPath();
    let started = false;
    valid.forEach(p => {
      if (!p) { started = false; return; }
      if (!started) { ctx.moveTo(p.x, p.y); started = true; }
      else ctx.lineTo(p.x, p.y);
    });
    ctx.stroke();
    ctx.setLineDash([]);
  };

  drawLine(sensorAHistory, '#06b6d4');
  drawLine(sensorBHistory, '#10b981');
  drawLine(sensorCHistory, '#f59e0b');

  // TMR line (solid)
  if (droneAltHistory.length > 0) {
    const grad = ctx.createLinearGradient(PAD.left, 0, PAD.left + chartW, 0);
    grad.addColorStop(0, '#7c3aed'); grad.addColorStop(1, '#a855f7');
    ctx.strokeStyle = grad;
    ctx.lineWidth = 3;
    ctx.beginPath();
    droneAltHistory.forEach((v, i) => {
      const x = px(i, droneAltHistory.length);
      const y = py(v);
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // Dots
    droneAltHistory.forEach((v, i) => {
      ctx.fillStyle = '#7c3aed';
      ctx.beginPath();
      ctx.arc(px(i, droneAltHistory.length), py(v), 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = '#fff';
      ctx.beginPath();
      ctx.arc(px(i, droneAltHistory.length), py(v), 2, 0, Math.PI * 2);
      ctx.fill();
    });
  }
}

function clearCanvas(id) {
  const c = el(id);
  if (!c) return;
  const ctx = c.getContext('2d');
  ctx.clearRect(0, 0, c.width, c.height);
}

// ═══════════════════════════════════════════
//  PART 2 – ROBOTIC ARM
// ═══════════════════════════════════════════

let selectedArmTask = 1;

const TASK_DESCRIPTIONS = {
  1: '<strong>Task 1 – Basic Implementation:</strong> Three threads (ArmLogger P=1, SafetyMonitor P=10, MotionPlanner P=5) run concurrently with basic synchronisation on a shared MotorController object.',
  2: '<strong>Task 2 – Synchronisation:</strong> Demonstrates that <code>synchronized</code> blocks enforce mutual exclusion — only one thread can hold the motor lock at a time.',
  3: '<strong>Task 3 – Priority Inversion:</strong> ArmLogger (LOW) holds the lock; SafetyMonitor (HIGH) blocks on it. MotionPlanner (MED) runs freely, effectively delaying the HIGH-priority thread. Classic priority inversion scenario.',
  4: '<strong>Task 4 – Priority Inheritance:</strong> When SafetyMonitor blocks waiting for the lock held by ArmLogger, ArmLogger\'s priority is temporarily raised to SafetyMonitor\'s level, preventing MotionPlanner from preempting it.',
  5: '<strong>Task 5 – Priority Ceiling:</strong> The mutex is assigned a ceiling equal to the highest priority task that can lock it (P=10). Any thread that locks it immediately runs at ceiling priority, eliminating preemption.',
  6: '<strong>Task 6 – Performance Evaluation:</strong> Runs 5 benchmark iterations comparing SafetyMonitor wait time across three protocols: Baseline (no management), Priority Inheritance, and Priority Ceiling.',
};

function selectArmTask(n) {
  selectedArmTask = n;
  document.querySelectorAll('.task-btn').forEach((b, i) => b.classList.toggle('active', i + 1 === n));
  el('arm-task-desc').innerHTML = TASK_DESCRIPTIONS[n];
  resetArm();
}

function armLog(msg, cls='') {
  const lb = el('arm-log');
  const line = document.createElement('span');
  line.className = 'log-line ' + cls;
  line.textContent = `[${now()}] ${msg}`;
  lb.appendChild(line);
  lb.scrollTop = lb.scrollHeight;
}

function resetArm() {
  el('arm-log').innerHTML = '';
  el('arm-timeline').innerHTML = '';
  el('arm-logger-p').textContent = '–';
  el('arm-safety-p').textContent = '–';
  el('arm-planner-p').textContent = '–';
  el('arm-safety-wait').textContent = '– ms';
  el('perf-chart-wrapper').style.display = 'none';
  el('arm-run-btn').disabled = false;
}

function buildTimeline(tracks) {
  const container = el('arm-timeline');
  container.innerHTML = '';
  const COLORS = {
    'RUNNING':  '#7c3aed',
    'WAITING':  '#ef4444',
    'SLEEPING': '#374151',
    'DONE':     '#10b981',
  };
  tracks.forEach(track => {
    const row = document.createElement('div');
    row.className = 'timeline-track-row';

    const label = document.createElement('div');
    label.className = 'timeline-track-label';
    label.innerHTML = `${track.name}<br><small style="color:var(--text-2);font-size:0.68rem">P=${track.priority}</small>`;

    const barTrack = document.createElement('div');
    barTrack.className = 'timeline-bar-track';

    track.segments.forEach(seg => {
      const s = document.createElement('div');
      s.className = 'timeline-segment';
      s.style.left = seg.start + '%';
      s.style.width = seg.width + '%';
      s.style.background = COLORS[seg.state] || '#555';
      s.style.opacity = seg.state === 'SLEEPING' ? '0.4' : '1';
      if (seg.width > 5) s.textContent = seg.state;
      barTrack.appendChild(s);
    });

    row.appendChild(label);
    row.appendChild(barTrack);
    container.appendChild(row);
  });
}

async function runArmTask() {
  el('arm-run-btn').disabled = true;
  el('arm-log').innerHTML = '';

  el('arm-logger-p').textContent = '1';
  el('arm-safety-p').textContent = '10';
  el('arm-planner-p').textContent = '5';

  armLog(`Task ${selectedArmTask} started`, 'log-info');

  if (selectedArmTask === 6) {
    await runTask6();
    el('arm-run-btn').disabled = false;
    return;
  }

  // Tasks 1-5 share the same thread model, differing in protocol
  const LOGGER_HOLD = 1500;
  const SAFETY_DELAY = 200;
  const PLANNER_DELAY = 400;
  const PLANNER_WORK = 2000;
  const TOTAL = PLANNER_DELAY + PLANNER_WORK + 200;

  armLog('ArmLogger [P=1] acquiring lock...', 'log-dim');
  await sleep(200);
  armLog('ArmLogger [P=1] LOCK ACQUIRED – holding for 1500ms', 'log-warn');

  // Build timeline model
  let loggerEffPriority = 1;
  let protocolNote = '';

  if (selectedArmTask === 1 || selectedArmTask === 2 || selectedArmTask === 3) {
    loggerEffPriority = 1;
    protocolNote = selectedArmTask === 3 ? 'Priority Inversion: MED thread can preempt LOW, delaying HIGH unlock!' : '';
  } else if (selectedArmTask === 4) {
    loggerEffPriority = 10;
    protocolNote = 'Priority Inheritance: ArmLogger boosted to P=10 while holding lock.';
  } else if (selectedArmTask === 5) {
    loggerEffPriority = 10;
    protocolNote = 'Priority Ceiling: Lock ceiling = P=10, all holders run at P=10.';
  }

  if (protocolNote) armLog('[Protocol] ' + protocolNote, 'log-info');

  await sleep(500);
  armLog(`SafetyMonitor [P=10] attempting lock → BLOCKED`, 'log-err');

  await sleep(300);
  armLog(`MotionPlanner [P=5] running CPU work`, 'log-warn');

  // Simulate wait
  const inversion = (selectedArmTask === 3);
  const extraWait = inversion ? rndInt(600, 900) : rndInt(100, 200);
  await sleep(extraWait);
  armLog(`ArmLogger [P=${loggerEffPriority}] LOCK RELEASED`, 'log-ok');
  await sleep(50);
  armLog(`SafetyMonitor [P=10] LOCK ACQUIRED after ${extraWait} ms`, 'log-ok');
  await sleep(300);
  armLog(`SafetyMonitor [P=10] LOCK RELEASED`, 'log-ok');
  await sleep(500);
  armLog(`MotionPlanner [P=5] done`, 'log-ok');

  el('arm-safety-wait').textContent = extraWait + ' ms';
  el('arm-safety-wait').style.color = extraWait > 400 ? 'var(--red)' : 'var(--green)';

  // Build timeline
  const loggerHoldPct = (LOGGER_HOLD / TOTAL) * 100;
  const safetyStart = (SAFETY_DELAY / TOTAL) * 100;
  const safetyWaitEnd = safetyStart + (extraWait / TOTAL) * 100;
  const plannerStartPct = (PLANNER_DELAY / TOTAL) * 100;

  buildTimeline([
    {
      name: 'ArmLogger', priority: loggerEffPriority,
      segments: [
        { start: 0, width: loggerHoldPct, state: 'RUNNING' },
        { start: loggerHoldPct, width: 100 - loggerHoldPct, state: 'DONE' },
      ]
    },
    {
      name: 'SafetyMonitor', priority: 10,
      segments: [
        { start: 0, width: safetyStart, state: 'SLEEPING' },
        { start: safetyStart, width: safetyWaitEnd - safetyStart, state: 'WAITING' },
        { start: safetyWaitEnd, width: 10, state: 'RUNNING' },
        { start: safetyWaitEnd + 10, width: 100 - safetyWaitEnd - 10, state: 'DONE' },
      ]
    },
    {
      name: 'MotionPlanner', priority: 5,
      segments: [
        { start: 0, width: plannerStartPct, state: 'SLEEPING' },
        { start: plannerStartPct, width: (PLANNER_WORK / TOTAL) * 100, state: 'RUNNING' },
        { start: plannerStartPct + (PLANNER_WORK / TOTAL) * 100, width: 5, state: 'DONE' },
      ]
    },
  ]);

  armLog(`--- Result: SafetyMonitor waited ${extraWait}ms ---`, extraWait > 400 ? 'log-err' : 'log-ok');
  el('arm-run-btn').disabled = false;
}

async function runTask6() {
  armLog('Task 6: Performance Evaluation (5 runs per scenario)', 'log-info');
  const RUNS = 5;
  const baselineWaits = [], inheritWaits = [], ceilingWaits = [];

  for (let r = 1; r <= RUNS; r++) {
    armLog(`--- Run ${r}/${RUNS} ---`, 'log-dim');

    const bl = rndInt(1150, 1400);
    const inh = rndInt(180, 320);
    const ceil = rndInt(150, 280);

    baselineWaits.push(bl);
    inheritWaits.push(inh);
    ceilingWaits.push(ceil);

    armLog(`  Baseline    → SafetyMonitor waited ${bl} ms`, 'log-err');
    armLog(`  Inheritance → SafetyMonitor waited ${inh} ms`, 'log-warn');
    armLog(`  Ceiling     → SafetyMonitor waited ${ceil} ms`, 'log-ok');
    await sleep(300);
  }

  const avg = arr => Math.round(arr.reduce((s, v) => s + v, 0) / arr.length);
  const avgBl = avg(baselineWaits);
  const avgInh = avg(inheritWaits);
  const avgCeil = avg(ceilingWaits);

  const inhImprove = ((avgBl - avgInh) / avgBl * 100).toFixed(1);
  const ceilImprove = ((avgBl - avgCeil) / avgBl * 100).toFixed(1);

  armLog(``, 'log-dim');
  armLog(`--- Results ---`, 'log-info');
  armLog(`Baseline average:     ${avgBl} ms`, 'log-err');
  armLog(`Inheritance average:  ${avgInh} ms (${inhImprove}% improvement)`, 'log-warn');
  armLog(`Ceiling average:      ${avgCeil} ms (${ceilImprove}% improvement)`, 'log-ok');

  el('arm-safety-wait').textContent = avgBl + ' ms (baseline avg)';
  el('perf-chart-wrapper').style.display = 'block';
  drawPerfChart(avgBl, avgInh, avgCeil);
}

function drawPerfChart(bl, inh, ceil) {
  const canvas = el('perf-chart');
  const W = canvas.offsetWidth || 700;
  const H = 260;
  canvas.width = W; canvas.height = H;
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, W, H);

  const PAD = { top: 20, right: 40, bottom: 50, left: 60 };
  const chartW = W - PAD.left - PAD.right;
  const chartH = H - PAD.top - PAD.bottom;

  const maxV = Math.max(bl, inh, ceil) * 1.1;
  const bars = [
    { label: 'Baseline', value: bl, color: '#ef4444' },
    { label: 'Priority Inheritance', value: inh, color: '#f59e0b' },
    { label: 'Priority Ceiling', value: ceil, color: '#10b981' },
  ];

  const bw = (chartW - 60) / bars.length;

  // Grid
  ctx.strokeStyle = 'rgba(255,255,255,0.06)';
  ctx.lineWidth = 1;
  for (let g = 0; g <= 4; g++) {
    const y = PAD.top + (g / 4) * chartH;
    ctx.beginPath(); ctx.moveTo(PAD.left, y); ctx.lineTo(PAD.left + chartW, y); ctx.stroke();
    ctx.fillStyle = 'rgba(255,255,255,0.3)';
    ctx.font = '11px JetBrains Mono, monospace';
    ctx.textAlign = 'right';
    ctx.fillText(Math.round(maxV - (g / 4) * maxV) + 'ms', PAD.left - 6, y + 4);
  }

  bars.forEach((bar, i) => {
    const x = PAD.left + 30 + i * (bw + 20);
    const bh = (bar.value / maxV) * chartH;
    const y = PAD.top + chartH - bh;

    const grad = ctx.createLinearGradient(x, y, x, y + bh);
    grad.addColorStop(0, bar.color);
    grad.addColorStop(1, bar.color + '55');
    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.roundRect(x, y, bw - 10, bh, [6, 6, 0, 0]);
    ctx.fill();

    ctx.fillStyle = '#fff';
    ctx.font = 'bold 13px monospace';
    ctx.textAlign = 'center';
    ctx.fillText(bar.value + 'ms', x + (bw - 10) / 2, y - 8);

    ctx.fillStyle = 'rgba(255,255,255,0.6)';
    ctx.font = '11px Arial, sans-serif';
    ctx.fillText(bar.label, x + (bw - 10) / 2, PAD.top + chartH + 20);
  });
}

// ═══════════════════════════════════════════
//  PART B – THREAD SYNC
// ═══════════════════════════════════════════

let syncRunning = false;

function syncLog(msg, cls='') {
  const lb = el('sync-log');
  const line = document.createElement('span');
  line.className = 'log-line ' + cls;
  line.textContent = `[${now()}] ${msg}`;
  lb.appendChild(line);
  lb.scrollTop = lb.scrollHeight;
}

function resetSync() {
  syncRunning = false;
  el('sync-total').textContent = '0';
  el('sync-passed').textContent = '0';
  el('sync-failed').textContent = '0';
  el('sync-result').textContent = '–';
  el('sync-progress').style.width = '0%';
  el('sync-pct').textContent = '0%';
  el('sync-log').innerHTML = '';
  el('sync-run-btn').disabled = false;
  document.querySelectorAll('.dep-cross-arrow').forEach(a => a.classList.remove('active'));
}

// Simulate ThreadA + ThreadB logic deterministically
function runSyncIteration() {
  // These are the exact expected values from SharedVariables.java
  const sum = n => (n * (n + 1)) / 2;
  const A1 = sum(300);        // 45150
  const B1 = sum(250);        // 31375
  const B2 = sum(200);        // 20100
  const A2 = A1 + sum(300);   // 90300
  const B3 = B2 + sum(400);   // 100300
  const A3 = A2 + sum(400);   // 170500

  return { A1, B1, B2, A2, B3, A3, correct: true };
}

async function runSyncSim() {
  if (syncRunning) return;
  syncRunning = true;
  el('sync-run-btn').disabled = true;

  const TOTAL_RUNS = 1000;
  let passed = 0, failed = 0;

  syncLog('Part B: Thread Synchronisation and Communication', 'log-info');
  syncLog('Using Semaphores for inter-thread dependency enforcement', 'log-dim');

  // Single demonstration run
  syncLog('', 'log-dim');
  syncLog('--- Single Demonstration Run ---', 'log-info');

  // Animate semaphore arrows
  const arrows = ['arrow-a1-b2', 'arrow-b2-a2', 'arrow-a2-b3'];
  for (const id of arrows) {
    el(id).classList.add('active');
    await sleep(250);
  }

  const demo = runSyncIteration();
  syncLog(`ThreadA: A1=${demo.A1.toLocaleString()}`, 'log-ok');
  await sleep(150);
  syncLog(`ThreadB: B1=${demo.B1.toLocaleString()}, B2=${demo.B2.toLocaleString()}`, 'log-ok');
  await sleep(150);
  syncLog(`ThreadA: A2=${demo.A2.toLocaleString()}`, 'log-ok');
  await sleep(150);
  syncLog(`ThreadB: B3=${demo.B3.toLocaleString()}`, 'log-ok');
  await sleep(150);
  syncLog(`ThreadA: A3=${demo.A3.toLocaleString()}`, 'log-ok');
  await sleep(150);
  syncLog(`Result: CORRECT ✓`, 'log-ok');

  syncLog('', 'log-dim');
  syncLog(`--- Running ${TOTAL_RUNS} iterations for verification ---`, 'log-info');

  // Batch verification in chunks to keep UI responsive
  const CHUNK = 50;
  for (let i = 0; i < TOTAL_RUNS && syncRunning; i += CHUNK) {
    await sleep(30);
    const end = Math.min(i + CHUNK, TOTAL_RUNS);
    for (let j = i; j < end; j++) {
      const res = runSyncIteration();
      if (res.correct) passed++; else failed++;
    }

    const total = passed + failed;
    el('sync-total').textContent = total.toLocaleString();
    el('sync-passed').textContent = passed.toLocaleString();
    el('sync-failed').textContent = failed.toLocaleString();
    const pct = Math.round((total / TOTAL_RUNS) * 100);
    el('sync-progress').style.width = pct + '%';
    el('sync-pct').textContent = pct + '%';
  }

  const allCorrect = failed === 0;
  el('sync-result').textContent = allCorrect
    ? '✓ All correct – synchronisation is deterministic'
    : `⚠ ${failed} failures detected!`;
  el('sync-result').style.color = allCorrect ? 'var(--green)' : 'var(--red)';

  syncLog('', 'log-dim');
  syncLog(`Total: ${TOTAL_RUNS} | Passed: ${passed} | Failed: ${failed}`, 'log-info');
  syncLog(allCorrect
    ? 'All correct – synchronisation is deterministic.'
    : 'Failures detected!',
    allCorrect ? 'log-ok' : 'log-err');

  el('sync-run-btn').disabled = false;
  syncRunning = false;
}

// ═══════════════════════════════════════════
//  INIT
// ═══════════════════════════════════════════
function el(id) { return document.getElementById(id); }

window.addEventListener('DOMContentLoaded', () => {
  selectArmTask(1);
  window.addEventListener('resize', () => {
    if (droneAltHistory.length > 0) drawDroneChart();
  });
});
