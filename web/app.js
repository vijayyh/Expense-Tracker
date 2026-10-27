'use strict';
/* =========================================================
   Expense Tracker (web) — same features as the Android app.
   Data is saved on this device with localStorage.
   ========================================================= */

const EXPENSE = 'expense';
const INCOME = 'income';

const CATEGORIES = [
  { key: 'food', label: 'Food', emoji: '🍔', color: '#FF8A65', type: EXPENSE },
  { key: 'travel', label: 'Travel', emoji: '🚌', color: '#4FC3F7', type: EXPENSE },
  { key: 'clothes', label: 'Clothes', emoji: '👕', color: '#BA68C8', type: EXPENSE },
  { key: 'groceries', label: 'Groceries', emoji: '🛒', color: '#81C784', type: EXPENSE },
  { key: 'education', label: 'Education', emoji: '📚', color: '#FFD54F', type: EXPENSE },
  { key: 'health', label: 'Health', emoji: '💊', color: '#F06292', type: EXPENSE },
  { key: 'bills', label: 'Bills', emoji: '💡', color: '#4DB6AC', type: EXPENSE },
  { key: 'fun', label: 'Fun', emoji: '🎬', color: '#9575CD', type: EXPENSE },
  { key: 'other', label: 'Other', emoji: '📦', color: '#90A4AE', type: EXPENSE },
  { key: 'pocket', label: 'Pocket Money', emoji: '💵', color: '#66BB6A', type: INCOME },
  { key: 'parttime', label: 'Part-time Job', emoji: '💼', color: '#26A69A', type: INCOME },
  { key: 'scholarship', label: 'Scholarship', emoji: '🎓', color: '#5C6BC0', type: INCOME },
  { key: 'gift', label: 'Gift', emoji: '🎁', color: '#EC407A', type: INCOME },
  { key: 'other_income', label: 'Other', emoji: '💰', color: '#8D6E63', type: INCOME },
];
const EXPENSE_CATS = CATEGORIES.filter(c => c.type === EXPENSE);
const INCOME_CATS = CATEGORIES.filter(c => c.type === INCOME);
const findCat = key => CATEGORIES.find(c => c.key === key) || EXPENSE_CATS[EXPENSE_CATS.length - 1];

// ---------------- storage ----------------
const store = {
  get(key, fallback) { try { const v = localStorage.getItem(key); return v === null ? fallback : v; } catch (e) { return fallback; } },
  set(key, value) { try { localStorage.setItem(key, value); } catch (e) { /* storage unavailable */ } },
};
let txs = (() => { try { return JSON.parse(store.get('et.tx', '[]')) || []; } catch (e) { return []; } })();
const saveTxs = () => store.set('et.tx', JSON.stringify(txs));
const addTx = t => { txs.push(t); saveTxs(); };
const removeTx = id => { txs = txs.filter(t => t.id !== id); saveTxs(); };
const sorted = list => [...list].sort((a, b) => b.time - a.time || b.id - a.id);
const total = (list, type) => list.filter(t => !type || t.type === type).reduce((s, t) => s + t.amount, 0);
const sumByCategory = type => {
  const map = {};
  txs.filter(t => t.type === type).forEach(t => { map[t.category] = (map[t.category] || 0) + t.amount; });
  return map;
};
const countByCategory = () => {
  const map = {};
  txs.forEach(t => { map[t.category] = (map[t.category] || 0) + 1; });
  return map;
};
let nextId = txs.reduce((m, t) => Math.max(m, t.id), 0) + 1;

// ---------------- formatting ----------------
const nf = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 });
const money = v => (v < 0 ? '-' : '') + '₹' + nf.format(Math.abs(v));
const DAY = 24 * 60 * 60 * 1000;
const startOfDay = t => { const d = new Date(t); d.setHours(0, 0, 0, 0); return d.getTime(); };
const dayLabel = t => {
  const today = startOfDay(Date.now());
  const day = startOfDay(t);
  if (day === today) return 'Today';
  if (day === startOfDay(today - DAY / 2)) return 'Yesterday';
  return new Date(t).toLocaleDateString('en-GB', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
};
const timeStr = t => new Date(t).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
const shortDate = t => new Date(t).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }) + ', ' + timeStr(t);
const greeting = () => { const h = new Date().getHours(); return h < 12 ? 'Good morning,' : h < 17 ? 'Good afternoon,' : 'Good evening,'; };
const esc = s => String(s).replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
const plural = n => n + (n === 1 ? ' transaction' : ' transactions');
const $ = sel => document.querySelector(sel);

// ---------------- motion helpers ----------------
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/** Counts a money label from its previous value (or 0) up to the new value. */
function countUp(el, target, fromZero) {
  const from = fromZero ? 0 : (el._value ?? 0);
  el._value = target;
  cancelAnimationFrame(el._raf);
  if (from === target || reduceMotion) { el.textContent = money(target); return; }
  const start = performance.now();
  const step = now => {
    const p = Math.min(1, (now - start) / 1000);
    const eased = 1 - Math.pow(1 - p, 3);
    el.textContent = p < 1 ? money(Math.round(from + (target - from) * eased)) : money(target);
    if (p < 1) el._raf = requestAnimationFrame(step);
  };
  el._raf = requestAnimationFrame(step);
}

/** Replays the slide-up entrance on every direct child, one after another. */
function stagger(container) {
  [...container.children].forEach((el, i) => {
    el.style.animation = 'none';
    void el.offsetWidth; // restart the animation
    el.style.animation = '';
    el.style.animationDelay = (i * 60) + 'ms';
  });
}

function shake(el) {
  el.classList.remove('shake');
  void el.offsetWidth;
  el.classList.add('shake');
  if (navigator.vibrate) navigator.vibrate(40);
}

// ---------------- HTML templates ----------------
const catSmall = (c, amount, i) => `
  <button class="clay cat-small" style="--c:${c.color};animation-delay:${i * 50}ms" data-cat="${c.key}">
    <span class="emoji-circle">${c.emoji}</span>
    <span class="cat-name">${c.label}</span>
    <span class="cat-amt">${money(amount)}</span>
  </button>`;

const catGrid = (c, amount, sum, count, i) => {
  const pct = sum > 0 ? Math.round(amount * 100 / sum) : 0;
  return `
  <button class="clay cat-grid" style="--c:${c.color};animation-delay:${i * 50}ms" data-cat="${c.key}">
    <span class="row"><span class="emoji-circle">${c.emoji}</span><span class="pct">${pct}%</span></span>
    <span class="cat-name">${c.label}</span>
    <span class="cat-amt">${money(amount)}</span>
    <span class="cat-count">${plural(count)}</span>
    <span class="bar"><i data-pct="${pct}"></i></span>
  </button>`;
};

const txRow = (t, grouped, i) => {
  const c = findCat(t.category);
  const when = grouped ? timeStr(t.time) : shortDate(t.time);
  const sign = t.type === INCOME ? '+ ' : '− ';
  return `
  <div class="tx-wrap" data-id="${t.id}" style="animation-delay:${Math.min(i, 12) * 45}ms">
    <div class="tx-bg"><span>🗑 Delete</span><span>Delete 🗑</span></div>
    <div class="glass tx">
      <span class="clay tx-icon" style="--c:${c.color}">${c.emoji}</span>
      <div class="tx-main"><div class="tx-title">${esc(t.title)}</div><div class="tx-sub">${c.label}  •  ${when}</div></div>
      <div class="tx-amt ${t.type}">${sign}${money(t.amount)}</div>
    </div>
  </div>`;
};

/** List grouped by day with a header showing the day's net amount. */
function groupedHtml(list) {
  let html = '';
  let label = null;
  let rows = [];
  let net = 0;
  let i = 0;
  const flush = () => {
    if (label === null) return;
    html += `<div class="day-head"><span>${label}</span><span>${net >= 0 ? '+ ' : '− '}${money(Math.abs(net))}</span></div>` + rows.join('');
  };
  sorted(list).forEach(t => {
    const l = dayLabel(t.time);
    if (l !== label) { flush(); label = l; rows = []; net = 0; }
    rows.push(txRow(t, true, i++));
    net += t.type === INCOME ? t.amount : -t.amount;
  });
  flush();
  return html;
}

/** Animates the progress bars in a container to their percentages. */
function fillBars(container) {
  requestAnimationFrame(() => requestAnimationFrame(() => {
    container.querySelectorAll('.bar i').forEach(bar => { bar.style.width = bar.dataset.pct + '%'; });
  }));
}

// ---------------- screens ----------------
let currentTab = 'home';
let historyFilter = 0;

function renderHome(fromZero) {
  const income = total(txs, INCOME);
  const expense = total(txs, EXPENSE);
  $('#greeting').textContent = greeting();
  $('#userName').textContent = store.get('et.name', 'Student') + ' 👋';
  countUp($('#balance'), income - expense, fromZero);
  countUp($('#incomeTotal'), income, fromZero);
  countUp($('#expenseTotal'), expense, fromZero);

  const spent = sumByCategory(EXPENSE);
  $('#homeCats').innerHTML = EXPENSE_CATS.map((c, i) => catSmall(c, spent[c.key] || 0, i)).join('');

  const recent = sorted(txs).slice(0, 5);
  $('#recentList').innerHTML = recent.map((t, i) => txRow(t, false, i)).join('');
  $('#recentEmpty').hidden = recent.length > 0;
}

function renderCategories(fromZero) {
  const spent = sumByCategory(EXPENSE);
  const counts = countByCategory();
  const spentTotal = total(txs, EXPENSE);
  countUp($('#donutTotal'), spentTotal, fromZero);
  drawDonut(EXPENSE_CATS.map(c => ({ value: spent[c.key] || 0, color: c.color })));

  $('#catGrid').innerHTML = EXPENSE_CATS.map((c, i) => catGrid(c, spent[c.key] || 0, spentTotal, counts[c.key] || 0, i)).join('');
  fillBars($('#catGrid'));

  const received = sumByCategory(INCOME);
  $('#incomeCats').innerHTML = INCOME_CATS.map((c, i) => catSmall(c, received[c.key] || 0, i)).join('');
}

/** Ring chart drawn with SVG circles; each slice grows from 0 with a CSS transition. */
function drawDonut(slices) {
  const svg = $('#donut');
  const r = 80;
  const circumference = 2 * Math.PI * r;
  const sum = slices.reduce((s, x) => s + x.value, 0);
  const nonZero = slices.filter(x => x.value > 0).length;
  const gap = nonZero > 1 ? 3 : 0;
  let html = `<circle class="track" cx="100" cy="100" r="${r}"></circle>`;
  let offset = 0;
  if (sum > 0) {
    slices.forEach(s => {
      if (s.value <= 0) return;
      const len = s.value / sum * circumference;
      html += `<circle cx="100" cy="100" r="${r}" stroke="${s.color}" stroke-dasharray="0 ${circumference}"
               stroke-dashoffset="${-(offset + gap / 2)}" data-len="${Math.max(0, len - gap)}"></circle>`;
      offset += len;
    });
  }
  svg.innerHTML = html;
  requestAnimationFrame(() => requestAnimationFrame(() => {
    svg.querySelectorAll('circle[data-len]').forEach(c => c.setAttribute('stroke-dasharray', `${c.dataset.len} ${circumference}`));
  }));
}

function renderHistory(fromZero) {
  const list = historyFilter === 1 ? txs.filter(t => t.type === INCOME)
    : historyFilter === 2 ? txs.filter(t => t.type === EXPENSE) : txs;
  countUp($('#moneyIn'), total(list, INCOME), fromZero);
  countUp($('#moneyOut'), total(list, EXPENSE), fromZero);
  $('#historyList').innerHTML = groupedHtml(list);
  $('#historyEmpty').hidden = list.length > 0;
}

function renderProfile(fromZero) {
  const name = store.get('et.name', 'Student');
  $('#profileName').textContent = name;
  $('#avatar').textContent = name.charAt(0).toUpperCase();

  const monthStart = new Date(); monthStart.setDate(1); monthStart.setHours(0, 0, 0, 0);
  const month = total(txs.filter(t => t.time >= monthStart.getTime()), EXPENSE);
  countUp($('#statMonth'), month, fromZero);
  countUp($('#statAvg'), month / new Date().getDate(), fromZero);
  $('#statCount').textContent = txs.length;

  const spent = sumByCategory(EXPENSE);
  const topKey = Object.keys(spent).sort((a, b) => spent[b] - spent[a])[0];
  const top = topKey ? findCat(topKey) : null;
  $('#statTop').textContent = top ? top.label : '—';
  $('#statTopEmoji').textContent = top ? top.emoji : '🏆';
  setSeg($('#themeSeg'), ['light', 'dark', 'system'].indexOf(store.get('et.theme', 'system')));
}

const RENDER = { home: renderHome, categories: renderCategories, history: renderHistory, profile: renderProfile };

/** Re-draws every screen after data changes; numbers animate to their new values. */
function refreshAll() {
  Object.values(RENDER).forEach(fn => fn(false));
  if (detailKey) renderDetail(false);
}

function selectTab(name) {
  if (!RENDER[name]) return;
  currentTab = name;
  document.querySelectorAll('.tab').forEach(s => { s.hidden = s.id !== 'tab-' + name; });
  document.querySelectorAll('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
  stagger($('#tab-' + name));
  RENDER[name](true);
  window.scrollTo({ top: 0 });
}

// ---------------- segmented control ----------------
function setSeg(seg, index) {
  if (index < 0) index = 0;
  seg._index = index;
  seg.querySelector('.seg-ind').style.transform = `translateX(${index * 100}%)`;
  seg.querySelectorAll('button').forEach(b => b.classList.toggle('on', Number(b.dataset.i) === index));
}
function onSeg(seg, handler) {
  seg.addEventListener('click', e => {
    const b = e.target.closest('button');
    if (!b) return;
    const i = Number(b.dataset.i);
    if (i === seg._index) return;
    setSeg(seg, i);
    handler(i);
  });
}

// ---------------- theme ----------------
const darkQuery = window.matchMedia('(prefers-color-scheme: dark)');
function applyTheme() {
  const pref = store.get('et.theme', 'system');
  const dark = pref === 'dark' || (pref === 'system' && darkQuery.matches);
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
  $('#metaTheme').setAttribute('content', dark ? '#0E0B1F' : '#F3EEFF');
  $('#themeIcon').textContent = dark ? '☀️' : '🌙';
}
darkQuery.addEventListener?.('change', applyTheme);

// ---------------- category detail page ----------------
let detailKey = null;

function openDetail(key) {
  detailKey = key;
  const c = findCat(key);
  const page = $('#detail');
  $('#detailCard').style.setProperty('--c', c.color);
  $('#detailKind').textContent = c.type === INCOME ? 'Income source' : 'Category';
  $('#detailEmoji').textContent = c.emoji;
  $('#detailName').textContent = c.label;
  $('#detailLabel').textContent = c.type === INCOME ? 'Total received' : 'Total spent';
  const add = $('#detailAdd');
  add.textContent = '＋  Add to ' + c.label;
  add.classList.toggle('income', c.type === INCOME);
  renderDetail(true);
  page.hidden = false;
  page.scrollTop = 0;
  requestAnimationFrame(() => requestAnimationFrame(() => page.classList.add('open')));
  history.pushState({ detail: key }, '');
}

function closeDetail() {
  const page = $('#detail');
  page.classList.remove('open');
  detailKey = null;
  setTimeout(() => { if (!detailKey) page.hidden = true; }, 400);
}

function renderDetail(fromZero) {
  const list = txs.filter(t => t.category === detailKey);
  const sum = total(list);
  countUp($('#detailTotal'), sum, fromZero);
  $('#detailStats').textContent = list.length ? `${plural(list.length)}  •  Avg ${money(sum / list.length)}` : plural(0);
  $('#detailList').innerHTML = groupedHtml(list);
  $('#detailEmpty').hidden = list.length > 0;
}

window.addEventListener('popstate', () => { if (detailKey) closeDetail(); });

// ---------------- add sheet ----------------
const sheetState = { type: EXPENSE, category: null };

function todayInputValue(t = Date.now()) {
  const d = new Date(t);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function openSheet(type = EXPENSE, category = null) {
  sheetState.type = type;
  sheetState.category = category;
  $('#amountInput').value = '';
  $('#titleInput').value = '';
  $('#noteInput').value = '';
  $('#amountError').hidden = true;
  const date = $('#dateInput');
  date.value = todayInputValue();
  date.max = todayInputValue();
  setSeg($('#typeSeg'), type === INCOME ? 1 : 0);
  applySheetType();

  const fab = $('#fab');
  fab.classList.toggle('spin');
  $('#sheetBackdrop').hidden = false;
  $('#sheet').hidden = false;
  requestAnimationFrame(() => requestAnimationFrame(() => {
    $('#sheetBackdrop').classList.add('show');
    $('#sheet').classList.add('show');
  }));
}

function closeSheet() {
  $('#sheetBackdrop').classList.remove('show');
  $('#sheet').classList.remove('show');
  document.activeElement?.blur();
  setTimeout(() => { $('#sheetBackdrop').hidden = true; $('#sheet').hidden = true; }, 450);
}

function applySheetType() {
  const income = sheetState.type === INCOME;
  $('#typeSeg').classList.toggle('income', income);
  $('#amountBox').classList.toggle('income', income);
  const save = $('#saveBtn');
  save.textContent = income ? 'Add Income' : 'Add Expense';
  save.classList.toggle('income', income);

  const cats = income ? INCOME_CATS : EXPENSE_CATS;
  if (!cats.some(c => c.key === sheetState.category)) sheetState.category = cats[0].key;
  $('#chips').innerHTML = cats.map((c, i) => `
    <button class="chip ${c.key === sheetState.category ? 'on' : ''} ${income ? 'income' : ''}" data-key="${c.key}"
            style="animation-delay:${i * 35}ms">${c.emoji}  ${c.label}</button>`).join('');
}

function saveSheet() {
  const amount = parseFloat($('#amountInput').value.replace(/,/g, ''));
  if (!(amount > 0)) {
    $('#amountError').hidden = false;
    shake($('#amountBox'));
    return;
  }
  const c = findCat(sheetState.category);
  const title = $('#titleInput').value.trim() || c.label;  // title is optional
  const note = $('#noteInput').value.trim();

  // Use the picked date with the current time of day
  const now = new Date();
  const [y, m, d] = ($('#dateInput').value || todayInputValue()).split('-').map(Number);
  const when = new Date(y, m - 1, d, now.getHours(), now.getMinutes(), now.getSeconds()).getTime();

  const t = { id: nextId++, type: sheetState.type, title, amount, category: c.key, note: note || null, time: Math.min(when, Date.now()) };
  addTx(t);
  if (navigator.vibrate) navigator.vibrate(15);
  closeSheet();
  refreshAll();
  snack((t.type === INCOME ? '💰 Income added: ' : '✅ Expense added: ') + money(t.amount));
}

// ---------------- swipe to delete ----------------
function enableSwipe(container) {
  let row = null, startX = 0, startY = 0, dx = 0, dragging = false;

  container.addEventListener('pointerdown', e => {
    const tx = e.target.closest('.tx');
    if (!tx) return;
    row = tx; startX = e.clientX; startY = e.clientY; dx = 0; dragging = false;
  });
  container.addEventListener('pointermove', e => {
    if (!row) return;
    const mx = e.clientX - startX;
    const my = e.clientY - startY;
    if (!dragging) {
      if (Math.abs(mx) > 10 && Math.abs(mx) > Math.abs(my)) {
        dragging = true;
        row.style.transition = 'none';
        try { row.setPointerCapture(e.pointerId); } catch (err) { /* ignore */ }
      } else if (Math.abs(my) > 10) { row = null; return; }
    }
    if (dragging) {
      dx = mx;
      row.style.transform = `translateX(${dx}px)`;
      row.previousElementSibling.style.opacity = Math.min(1, Math.abs(dx) / 120);
    }
  });
  const end = () => {
    if (!row) return;
    const r = row;
    row = null;
    r.style.transition = '';
    if (dragging && Math.abs(dx) > r.offsetWidth * 0.33) {
      r.style.transform = `translateX(${dx > 0 ? 110 : -110}%)`;
      const id = Number(r.parentElement.dataset.id);
      setTimeout(() => deleteWithUndo(id), 220);
    } else {
      r.style.transform = '';
      r.previousElementSibling.style.opacity = 0;
    }
    dragging = false;
  };
  container.addEventListener('pointerup', end);
  container.addEventListener('pointercancel', end);
}

function deleteWithUndo(id) {
  const t = txs.find(x => x.id === id);
  if (!t) return;
  removeTx(id);
  refreshAll();
  snack(`Deleted "${t.title}"`, 'UNDO', () => { addTx(t); refreshAll(); });
}

// ---------------- snackbar + dialog ----------------
let snackTimer = null;
function snack(text, actionText, action) {
  const bar = $('#snackbar');
  $('#snackText').textContent = text;
  const btn = $('#snackAction');
  btn.hidden = !actionText;
  btn.textContent = actionText || '';
  btn.onclick = () => { bar.classList.remove('show'); action && action(); };
  bar.classList.toggle('above-btn', !!detailKey);
  bar.classList.add('show');
  clearTimeout(snackTimer);
  snackTimer = setTimeout(() => bar.classList.remove('show'), actionText ? 4500 : 2500);
}

/** Small promise-based dialog. Resolves with true/false, or the typed text when `input` is given. */
function dialog({ title, message = '', input = null, okText = 'OK', danger = false }) {
  return new Promise(resolve => {
    const box = $('#dialog');
    $('#dialogTitle').textContent = title;
    $('#dialogMsg').textContent = message;
    $('#dialogMsg').hidden = !message;
    const field = $('#dialogInput');
    field.hidden = input === null;
    field.value = input || '';
    const ok = $('#dialogOk');
    ok.textContent = okText;
    ok.classList.toggle('danger', danger);
    box.hidden = false;
    requestAnimationFrame(() => box.classList.add('show'));
    if (input !== null) setTimeout(() => { field.focus(); field.select(); }, 100);

    const close = value => {
      box.classList.remove('show');
      setTimeout(() => { box.hidden = true; }, 250);
      ok.onclick = $('#dialogCancel').onclick = box.onclick = null;
      resolve(value);
    };
    ok.onclick = () => close(input === null ? true : field.value.trim());
    $('#dialogCancel').onclick = () => close(input === null ? false : null);
    box.onclick = e => { if (e.target === box) close(input === null ? false : null); };
  });
}

// ---------------- sample data ----------------
function loadSample() {
  const rows = [
    [20, 10, INCOME, 'pocket', 'Monthly pocket money', 6000], [18, 13, EXPENSE, 'food', 'Canteen lunch', 120],
    [17, 9, EXPENSE, 'travel', 'Bus pass', 450], [16, 18, EXPENSE, 'clothes', 'New T-shirt', 799],
    [15, 11, EXPENSE, 'education', 'Notebooks & pens', 260], [14, 20, EXPENSE, 'fun', 'Movie night', 350],
    [12, 17, INCOME, 'parttime', 'Tutoring payment', 2500], [12, 19, EXPENSE, 'groceries', 'Fruits & snacks', 340],
    [10, 8, EXPENSE, 'food', 'Breakfast', 80], [9, 15, EXPENSE, 'bills', 'Mobile recharge', 299],
    [8, 12, EXPENSE, 'health', 'Medicines', 180], [7, 14, INCOME, 'scholarship', 'Merit scholarship', 3000],
    [6, 21, EXPENSE, 'food', 'Pizza with friends', 420], [5, 10, EXPENSE, 'travel', 'Auto rickshaw', 90],
    [4, 16, EXPENSE, 'education', 'Online course', 499], [3, 13, EXPENSE, 'food', 'Canteen lunch', 110],
    [2, 18, EXPENSE, 'clothes', 'Sneakers', 1499], [2, 20, INCOME, 'gift', 'Birthday gift', 1000],
    [1, 9, EXPENSE, 'travel', 'Metro card top-up', 200], [1, 19, EXPENSE, 'groceries', 'Milk & bread', 95],
    [0, 9, EXPENSE, 'food', 'Coffee', 60], [0, 13, EXPENSE, 'other', 'Photocopies', 40],
  ];
  rows.forEach(([daysAgo, hour, type, category, title, amount]) => {
    const d = new Date();
    d.setDate(d.getDate() - daysAgo);
    d.setHours(hour, Math.floor(Math.random() * 59), 0, 0);
    txs.push({ id: nextId++, type, title, amount, category, note: null, time: Math.min(d.getTime(), Date.now()) });
  });
  saveTxs();
}

// ---------------- wiring ----------------
document.addEventListener('click', e => {
  const tabBtn = e.target.closest('[data-tab]');
  if (tabBtn) { selectTab(tabBtn.dataset.tab); return; }
  const catBtn = e.target.closest('[data-cat]');
  if (catBtn) { openDetail(catBtn.dataset.cat); return; }
  const chip = e.target.closest('.chip');
  if (chip) {
    sheetState.category = chip.dataset.key;
    document.querySelectorAll('.chip').forEach(c => c.classList.toggle('on', c === chip));
  }
});

$('#fab').addEventListener('click', () => openSheet(EXPENSE));
$('#sheetBackdrop').addEventListener('click', closeSheet);
$('#saveBtn').addEventListener('click', saveSheet);
$('#amountInput').addEventListener('input', () => { $('#amountError').hidden = true; });
onSeg($('#typeSeg'), i => { sheetState.type = i === 1 ? INCOME : EXPENSE; sheetState.category = null; applySheetType(); });
onSeg($('#historyFilter'), i => { historyFilter = i; renderHistory(false); });
onSeg($('#themeSeg'), i => { store.set('et.theme', ['light', 'dark', 'system'][i]); setTimeout(applyTheme, 300); });

$('#themeToggle').addEventListener('click', () => {
  const dark = document.documentElement.dataset.theme === 'dark';
  store.set('et.theme', dark ? 'light' : 'dark');
  $('#themeIcon').style.transform = 'rotate(360deg) scale(.4)';
  setTimeout(() => { applyTheme(); $('#themeIcon').style.transform = ''; renderProfile(false); }, 350);
});

$('#detailBack').addEventListener('click', () => history.back());
$('#detailAdd').addEventListener('click', () => { const c = findCat(detailKey); openSheet(c.type, c.key); });

const editName = async () => {
  const name = await dialog({ title: 'Your name', input: store.get('et.name', 'Student'), okText: 'Save' });
  if (name) { store.set('et.name', name); refreshAll(); }
};
$('#editName').addEventListener('click', editName);
$('#avatar').addEventListener('click', editName);

$('#loadSample').addEventListener('click', () => {
  loadSample();
  refreshAll();
  snack('🧪 Sample data added — check Home!');
});
$('#clearAll').addEventListener('click', async () => {
  const ok = await dialog({ title: 'Clear all data?', message: "This deletes every income and expense. It can't be undone.", okText: 'Delete all', danger: true });
  if (ok) { txs = []; saveTxs(); refreshAll(); snack('All data cleared'); }
});

['#recentList', '#historyList', '#detailList'].forEach(sel => enableSwipe($(sel)));

// ---------------- start ----------------
applyTheme();
document.querySelectorAll('.tab').forEach(tab => {
  [...tab.children].forEach((el, i) => { el.style.animationDelay = (i * 60) + 'ms'; });
});
setSeg($('#historyFilter'), 0);
renderCategories(true);
renderHistory(true);
renderProfile(true);
renderHome(true);

// Offline support: cache the app so it opens without internet
if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  window.addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
}
