// Lots-by-stage page. Reads GET /api/reports/stage-summary and reloads it every few seconds while the tab
// is visible, since the counts move as lots are scanned. Stage ids come from the consumer's stage editor,
// so everything goes in via textContent, never innerHTML.

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
};

/** { stages: [{ stage, activeLots }], totalActiveLots }, or null until the first load. */
let data = null;
/** True while a request is running, so a timer tick doesn't overlap a manual refresh. */
let busy = false;

const timeFormat = new Intl.DateTimeFormat(undefined, { timeStyle: 'medium' });
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

function setStatus(message, isError = false) {
  els.status.className = isError ? 'status status--error' : 'status';
  els.status.replaceChildren(...(message ? [message] : []));
}

/** Keeps the last table on screen when a load fails, so a blip doesn't blank the report. */
async function load() {
  if (busy) return;
  setBusy(true);
  try {
    const res = await fetch(API);
    if (!res.ok) throw new Error(await errorMessage(res));
    data = await res.json();
    setStatus();
    els.updated.textContent = `Updated ${timeFormat.format(new Date())}`;
    render();
  } catch (err) {
    setStatus(`Could not load the report: ${networkAware(err)}.`, true);
  } finally {
    setBusy(false);
  }
}

function render() {
  const { stages, totalActiveLots } = data;
  els.tableWrap.hidden = stages.length === 0;
  els.empty.hidden = stages.length > 0;
  els.total.textContent = `${numberFormat.format(totalActiveLots)} active ${totalActiveLots === 1 ? 'lot' : 'lots'}`
    + ` across ${stages.length} ${stages.length === 1 ? 'stage' : 'stages'}`;
  const max = Math.max(1, ...stages.map((s) => s.activeLots));
  els.rows.replaceChildren(...stages.map((s) => row(s, max)));
}

function row({ stage, activeLots }, max) {
  return el('tr', {}, [
    el('td', { class: 'stage-name', 'data-label': 'Stage', text: stage }),
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
setInterval(() => {
  if (document.visibilityState === 'visible') load();
}, REFRESH_MS);
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') load();
});

load();
