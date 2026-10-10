// Lots-by-stage page. Reads GET /api/reports/stage-summary and reloads it every few seconds while the tab
// is visible, since the counts move as lots are scanned. Clicking a stage opens a modal listing its lots
// from GET /api/reports/stage-summary/{stage}/lots, which reloads along with the counts while open.
// Stage and lot ids come from other apps, so everything goes in via textContent, never innerHTML.

const API = '/api/reports/stage-summary';
const REFRESH_MS = 10_000;

const els = {
  total: document.getElementById('total'),
  updated: document.getElementById('updated'),
  refresh: document.getElementById('refresh'),
  status: document.getElementById('status'),
  tableWrap: document.getElementById('table-wrap'),
  rows: document.getElementById('rows'),
  empty: document.getElementById('empty'),
  dialog: document.getElementById('lots-dialog'),
  lotsTitle: document.getElementById('lots-title'),
  lotsCount: document.getElementById('lots-count'),
  lotsClose: document.getElementById('lots-close'),
  lotsStatus: document.getElementById('lots-status'),
  lotsTableWrap: document.getElementById('lots-table-wrap'),
  lotRows: document.getElementById('lot-rows'),
  lotsEmpty: document.getElementById('lots-empty'),
};

/** { stages: [{ stage, activeLots }], totalActiveLots }, or null until the first load. */
let data = null;
/** True while a request is running, so a timer tick doesn't overlap a manual refresh. */
let busy = false;
/** The stage shown in the modal, or null when it's closed; responses for any other stage are dropped. */
let openStage = null;

const timeFormat = new Intl.DateTimeFormat(undefined, { timeStyle: 'medium' });
const eventTimeFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'short', timeStyle: 'short' });
const numberFormat = new Intl.NumberFormat();

function el(tag, props = {}, children = []) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (key === 'class') node.className = value;
    else if (key === 'text') node.textContent = value;
    else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
    else if (key in node) node[key] = value;
    else node.setAttribute(key, value);
  }
  node.append(...children);
  return node;
}

function setStatus(target, message, isError = false) {
  target.className = isError ? 'status status--error' : 'status';
  target.replaceChildren(...(message ? [message] : []));
}

function plural(count, one, many) {
  return `${numberFormat.format(count)} ${count === 1 ? one : many}`;
}

/** Keeps the last table on screen when a load fails, so a blip doesn't blank the report. */
async function load() {
  if (busy) return;
  setBusy(true);
  try {
    const res = await fetch(API);
    if (!res.ok) throw new Error(await errorMessage(res));
    data = await res.json();
    setStatus(els.status);
    els.updated.textContent = `Updated ${timeFormat.format(new Date())}`;
    render();
  } catch (err) {
    setStatus(els.status, `Could not load the report: ${networkAware(err)}.`, true);
  } finally {
    setBusy(false);
  }
  if (openStage) loadLots(openStage);
}

function render() {
  const { stages, totalActiveLots } = data;
  els.tableWrap.hidden = stages.length === 0;
  els.empty.hidden = stages.length > 0;
  els.total.textContent = `${plural(totalActiveLots, 'active lot', 'active lots')}`
    + ` across ${plural(stages.length, 'stage', 'stages')}`;
  const max = Math.max(1, ...stages.map((s) => s.activeLots));
  els.rows.replaceChildren(...stages.map((s) => row(s, max)));
}

function row({ stage, activeLots }, max) {
  return el('tr', {}, [
    el('td', { class: 'stage-name', 'data-label': 'Stage' }, [
      el('button', {
        type: 'button',
        class: 'link',
        text: stage,
        title: `Show the lots at ${stage}`,
        onclick: () => openLots(stage),
      }),
    ]),
    el('td', { class: 'num', 'data-label': 'Active lots', text: numberFormat.format(activeLots) }),
    el('td', { class: 'bar-cell' }, [
      el('span', { class: 'bar', 'aria-hidden': 'true', style: `width: ${(activeLots / max) * 100}%` }),
    ]),
  ]);
}

function setBusy(value) {
  busy = value;
  els.refresh.disabled = value;
}

// Lots modal

function openLots(stage) {
  openStage = stage;
  els.lotsTitle.textContent = `Lots at ${stage}`;
  els.lotsCount.textContent = '';
  els.lotsTableWrap.hidden = true;
  els.lotsEmpty.hidden = true;
  els.lotRows.replaceChildren();
  setStatus(els.lotsStatus, 'Loading…');
  els.dialog.showModal();
  loadLots(stage);
}

async function loadLots(stage) {
  try {
    const res = await fetch(`${API}/${encodeURIComponent(stage)}/lots`);
    if (!res.ok) throw new Error(await errorMessage(res));
    const { lots } = await res.json();
    if (stage !== openStage) return;
    setStatus(els.lotsStatus);
    renderLots(lots);
  } catch (err) {
    if (stage !== openStage) return;
    setStatus(els.lotsStatus, `Could not load the lots: ${networkAware(err)}.`, true);
  }
}

function renderLots(lots) {
  els.lotsTableWrap.hidden = lots.length === 0;
  els.lotsEmpty.hidden = lots.length > 0;
  const held = lots.filter((lot) => lot.onHold).length;
  els.lotsCount.textContent = plural(lots.length, 'active lot', 'active lots')
    + (held > 0 ? `, ${numberFormat.format(held)} on hold` : '');
  els.lotRows.replaceChildren(...lots.map(lotRow));
}

function lotRow({ lotId, wipLocation, onHold, updatedAt }) {
  return el('tr', {}, [
    el('td', { class: 'stage-name', 'data-label': 'Lot' }, [el('span', { class: 'mono', text: lotId })]),
    el('td', { 'data-label': 'WIP location', text: wipLocation || '—' }),
    el('td', { 'data-label': 'Hold' }, onHold ? [el('span', { class: 'chip chip--hold', text: 'On hold' })] : []),
    el('td', {
      class: 'nowrap',
      'data-label': 'Last event',
      text: updatedAt ? eventTimeFormat.format(new Date(updatedAt)) : '—',
    }),
  ]);
}

// Errors

function networkAware(err) {
  return err instanceof TypeError ? 'the server could not be reached' : err.message;
}

async function errorMessage(res) {
  try {
    const body = await res.json();
    return body.detail || body.message || `Server returned ${res.status}`;
  } catch {
    return `Server returned ${res.status}`;
  }
}

els.refresh.addEventListener('click', () => load());
els.lotsClose.addEventListener('click', () => els.dialog.close());
// A click on the dialog element itself, rather than its contents, is a click on the backdrop.
els.dialog.addEventListener('click', (event) => {
  if (event.target === els.dialog) els.dialog.close();
});
els.dialog.addEventListener('close', () => {
  openStage = null;
});
setInterval(() => {
  if (document.visibilityState === 'visible') load();
}, REFRESH_MS);
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') load();
});

load();
