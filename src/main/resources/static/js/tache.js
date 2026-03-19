'use strict';

/* =============================================================================
   SUPPRESSION — Confirmation
   ============================================================================= */

function confirmerSuppression(form) {
  return window.confirm(
    'Êtes-vous sûr de vouloir supprimer cette tâche ?\n' +
    'Toutes ses sous-tâches seront également supprimées.'
  );
}

/* =============================================================================
   ALERTES — Disparition automatique
   ============================================================================= */

function initAlertesFlash() {
  document.querySelectorAll('.alert-success').forEach(alerte => {
    setTimeout(() => {
      alerte.style.transition = 'opacity 0.5s ease';
      alerte.style.opacity = '0';
      setTimeout(() => alerte.remove(), 500);
    }, 4000);
  });
}

/* =============================================================================
   FORMULAIRE — Validation légère côté client
   ============================================================================= */

function initValidationFormulaire() {
  const formulaire = document.querySelector('form');
  if (!formulaire) return;

  formulaire.addEventListener('submit', function (e) {
    let valide = true;
    formulaire.querySelectorAll('[required]').forEach(champ => {
      if (!champ.value.trim()) {
        champ.classList.add('input--error');
        valide = false;
      } else {
        champ.classList.remove('input--error');
      }
    });
    if (!valide) {
      e.preventDefault();
      const premier = formulaire.querySelector('.input--error');
      if (premier) { premier.scrollIntoView({ behavior: 'smooth', block: 'center' }); premier.focus(); }
    }
  });

  formulaire.querySelectorAll('.input, .textarea').forEach(champ => {
    champ.addEventListener('input', function () { this.classList.remove('input--error'); });
  });
}

/* =============================================================================
   MS PROJECT — Toggle expand / collapse
   ============================================================================= */

function toggleGroupe(btn) {
  const groupId  = btn.dataset.group;
  const estOuvert = btn.getAttribute('aria-expanded') === 'true';
  const children = document.querySelector('.msp-children[data-group="' + groupId + '"]');
  if (!children) return;

  if (estOuvert) {
    children.style.display = 'none';
    btn.setAttribute('aria-expanded', 'false');
    btn.classList.add('collapsed');
  } else {
    children.style.display = '';
    btn.setAttribute('aria-expanded', 'true');
    btn.classList.remove('collapsed');
  }
}

function toutReplier() {
  document.querySelectorAll('.msp-toggle-btn[aria-expanded="true"]').forEach(btn => toggleGroupe(btn));
}

function toutDeployer() {
  document.querySelectorAll('.msp-toggle-btn[aria-expanded="false"]').forEach(btn => toggleGroupe(btn));
}

/* =============================================================================
   MS PROJECT — Gantt
   ============================================================================= */

function initMSProject() {
  if (!window.OAKTIME) return;

  var lignes = typeof window.OAKTIME.lignes === 'string'
    ? JSON.parse(window.OAKTIME.lignes)
    : (window.OAKTIME.lignes || []);

  if (!lignes.length) return;

  var ganttDebutStr = window.OAKTIME.ganttDebut;
  var ganttFinStr   = window.OAKTIME.ganttFin;
  if (!ganttDebutStr || !ganttFinStr) return;

  var today = new Date(); today.setHours(0, 0, 0, 0);

  var d0 = new Date(ganttDebutStr);
  var d1 = new Date(ganttFinStr);
  var plageMs0  = d1 - d0 || 86400000;
  var margeMs   = plageMs0 * 0.08;
  var plageMin  = new Date(d0.getTime() - margeMs);
  var plageMax  = new Date(d1.getTime() + margeMs);
  var plageTotale = plageMax - plageMin;

  function pct(d) {
    return Math.max(0, Math.min(100, ((d - plageMin) / plageTotale) * 100));
  }

  // Axe temporel
  var axisEl = document.getElementById('gantt-axis');
  if (axisEl) {
    axisEl.innerHTML = '';
    var nbJours = plageTotale / 86400000;
    // Niveau jour si <= 60 jours, semaine si <= 180 jours, sinon mois
    var parJour    = nbJours <= 60;
    var parSemaine = !parJour && nbJours <= 180;
    var cursor;

    if (parJour) {
      cursor = new Date(plageMin.getFullYear(), plageMin.getMonth(), plageMin.getDate());
      while (cursor < plageMax) {
        var finJour = new Date(cursor.getTime() + 86400000);
        var lp = pct(cursor < plageMin ? plageMin : cursor);
        var rp = pct(finJour > plageMax ? plageMax : finJour);
        var wp = rp - lp;
        if (wp > 0.2) {
          var lbl = document.createElement('div');
          lbl.className = 'gantt-axis-label';
          lbl.style.left  = lp.toFixed(3) + '%';
          lbl.style.width = wp.toFixed(3) + '%';
          // Week-end en gris
          var jourSemaine = cursor.getDay();
          if (jourSemaine === 0 || jourSemaine === 6) lbl.style.opacity = '0.4';
          lbl.textContent = cursor.toLocaleDateString('fr-FR', { day: '2-digit', month: wp > 4 ? '2-digit' : undefined });
          axisEl.appendChild(lbl);
        }
        cursor = finJour;
      }
    } else if (parSemaine) {
      cursor = new Date(plageMin);
      cursor.setDate(cursor.getDate() - ((cursor.getDay() + 6) % 7));
      while (cursor < plageMax) {
        var finSem = new Date(cursor.getTime() + 7 * 86400000);
        var lp = pct(cursor < plageMin ? plageMin : cursor);
        var rp = pct(finSem > plageMax ? plageMax : finSem);
        var wp = rp - lp;
        if (wp > 0.5) {
          var lbl = document.createElement('div');
          lbl.className = 'gantt-axis-label';
          lbl.style.left  = lp.toFixed(3) + '%';
          lbl.style.width = wp.toFixed(3) + '%';
          lbl.textContent = cursor.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
          axisEl.appendChild(lbl);
        }
        cursor = finSem;
      }
    } else {
      cursor = new Date(plageMin.getFullYear(), plageMin.getMonth(), 1);
      while (cursor < plageMax) {
        var finMois = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1);
        var lp2 = pct(cursor < plageMin ? plageMin : cursor);
        var rp2 = pct(finMois > plageMax ? plageMax : finMois);
        var wp2 = rp2 - lp2;
        if (wp2 > 0.5) {
          var lbl2 = document.createElement('div');
          lbl2.className = 'gantt-axis-label';
          lbl2.style.left  = lp2.toFixed(3) + '%';
          lbl2.style.width = wp2.toFixed(3) + '%';
          lbl2.textContent = cursor.toLocaleDateString('fr-FR', {
            month: wp2 > 8 ? 'long' : 'short',
            year: wp2 > 12 ? '2-digit' : undefined
          });
          axisEl.appendChild(lbl2);
        }
        cursor = finMois;
      }
    }
  }

  // Barres Gantt
  var todayPct = pct(today);

  document.querySelectorAll('.msp-gantt-cell[data-id]').forEach(function(cell) {
    var debutStr = cell.dataset.debut;
    var finStr   = cell.dataset.fin;
    var corrigee = cell.dataset.corrigee === 'true';
    var racine   = cell.dataset.racine   === 'true';

    // Grille verticale
    var c = new Date(plageMin.getFullYear(), plageMin.getMonth() + 1, 1);
    while (c < plageMax) {
      var gl = document.createElement('div');
      gl.className = 'msp-grid-line';
      gl.style.left = pct(c).toFixed(3) + '%';
      cell.appendChild(gl);
      c = new Date(c.getFullYear(), c.getMonth() + 1, 1);
    }

    // Ligne aujourd'hui
    if (todayPct > 0 && todayPct < 100) {
      var tl = document.createElement('div');
      tl.className = 'msp-today-line';
      tl.style.left = todayPct.toFixed(3) + '%';
      cell.appendChild(tl);
    }

    if (!finStr) return;

    var debut = debutStr ? new Date(debutStr) : new Date(finStr);
    var fin   = new Date(finStr);
    var enRetard = fin < today;

    var leftPct  = pct(debut);
    var widthPct = Math.max(pct(fin) - leftPct, 0.5);

    var classe = 'msp-bar msp-bar--normal';
    if (racine)        classe = 'msp-bar msp-bar--racine msp-bar--racine-style';
    else if (enRetard) classe = 'msp-bar msp-bar--retard';
    else if (corrigee) classe = 'msp-bar msp-bar--corrigee';

    var bar = document.createElement('div');
    bar.className = classe;
    bar.style.left  = leftPct.toFixed(3) + '%';
    bar.style.width = widthPct.toFixed(3) + '%';

    var data = lignes.find(function(l) { return String(l.id) === String(cell.dataset.id); });
    if (data) bar.title = data.nom + ' : ' + (debutStr || '?') + ' → ' + finStr;

    cell.appendChild(bar);
  });
}

/* =============================================================================
   DÉPENDANCES — Layout topologique en colonnes + flèches orthogonales
   
   Principe garanti sans croisement :
   - Rang topologique : chaque tâche est placée dans la colonne rang(pred)+1
   - Les flèches vont toujours de droite → gauche, dans le couloir entre colonnes
   - Le dernier point du chemin est à ARROW_TIP px du bord (= longueur du marqueur)
     pour que la pointe touche exactement le bord sans rentrer dans la card
   ============================================================================= */

const DEPS_COLOR        = '#9b93c4';
const DEPS_COLOR_RETARD = '#d4907a';
const DEPS_STROKE       = 2;
const DEPS_RADIUS       = 6;

const CARD_W     = 240;
const CARD_GAP_X = 60;
const CARD_GAP_Y = 20;

// ── Graphe depuis le DOM ──────────────────────────────────────────────────────
function construireGraphe(cards) {
  const ids   = new Set(cards.map(c => c.dataset.tacheId));
  const preds = new Map();
  const succs = new Map();
  ids.forEach(id => { preds.set(id, []); succs.set(id, []); });
  cards.forEach(c => {
    const id  = c.dataset.tacheId;
    const raw = (c.dataset.predecesseurs || '').trim();
    raw.split(',').filter(Boolean).forEach(pid => {
      pid = pid.trim();
      if (ids.has(pid)) {
        preds.get(id).push(pid);
        succs.get(pid).push(id);
      }
    });
  });
  return { ids, preds, succs };
}

// ── Rang topologique (Kahn) ───────────────────────────────────────────────────
function calculerRangs(ids, preds, succs) {
  const rang  = new Map();
  const inDeg = new Map();
  ids.forEach(id => { rang.set(id, 0); inDeg.set(id, preds.get(id).length); });
  const queue = [...ids].filter(id => inDeg.get(id) === 0);
  while (queue.length) {
    const id = queue.shift();
    succs.get(id).forEach(sid => {
      rang.set(sid, Math.max(rang.get(sid), rang.get(id) + 1));
      inDeg.set(sid, inDeg.get(sid) - 1);
      if (inDeg.get(sid) === 0) queue.push(sid);
    });
  }
  return rang;
}

// ── Layout topologique ────────────────────────────────────────────────────────
function layoutTopoCards(wrap) {
  if (!wrap) return;
  const cards = Array.from(wrap.querySelectorAll('[data-tache-id]:not(.sous-tache-panel--hidden)'));
  if (!cards.length) { wrap.style.height = '0'; return; }

  // Reset positions
  cards.forEach(c => {
    c.style.position = '';
    c.style.left     = '';
    c.style.top      = '';
    c.style.width    = CARD_W + 'px';
  });

  const { ids, preds, succs } = construireGraphe(cards);

  // Vérifier s'il existe au moins une dépendance
  let hasDeps = false;
  ids.forEach(id => { if (preds.get(id).length > 0) hasDeps = true; });

  if (!hasDeps) {
    // Aucune dépendance → flex-wrap natif (pas d'absolute, pas de hauteur forcée)
    wrap.style.display  = 'flex';
    wrap.style.flexWrap = 'wrap';
    wrap.style.gap      = CARD_GAP_Y + 'px';
    wrap.style.height   = '';
    wrap.dataset.flexMode = 'true';
    return;
  }

  // Réinitialiser si on repasse en mode topo après filtre
  wrap.style.display  = '';
  wrap.style.flexWrap = '';
  wrap.style.gap      = '';
  wrap.dataset.flexMode = '';

  const rang   = calculerRangs(ids, preds, succs);
  const nbCols = Math.max(...rang.values()) + 1;

  const colonnes = Array.from({ length: nbCols }, () => []);
  cards.forEach(c => colonnes[rang.get(c.dataset.tacheId)].push(c));

  const colW = colonnes.map(col => Math.max(...col.map(c => c.offsetWidth)));
  const colX = [0];
  for (let i = 1; i < nbCols; i++) colX.push(colX[i-1] + colW[i-1] + CARD_GAP_X);

  colonnes.forEach((col, ci) => {
    let y = 0;
    col.forEach(card => {
      card.style.position = 'absolute';
      card.style.left     = colX[ci] + 'px';
      card.style.top      = y + 'px';
      y += card.offsetHeight + CARD_GAP_Y;
    });
  });

  let maxH = 0;
  cards.forEach(c => maxH = Math.max(maxH, c.offsetTop + c.offsetHeight));
  wrap.style.height = (maxH + CARD_GAP_Y) + 'px';
}

// ── SVG path avec coins arrondis ──────────────────────────────────────────────
function ptsVersSVG(pts) {
  if (pts.length < 2) return '';
  let d = `M${pts[0].x.toFixed(1)},${pts[0].y.toFixed(1)}`;
  for (let i = 1; i < pts.length - 1; i++) {
    const p = pts[i-1], c = pts[i], n = pts[i+1];
    const dx1 = c.x-p.x, dy1 = c.y-p.y, dx2 = n.x-c.x, dy2 = n.y-c.y;
    const l1  = Math.hypot(dx1, dy1), l2 = Math.hypot(dx2, dy2);
    if (l1 < 0.5 || l2 < 0.5) { d += ` L${c.x.toFixed(1)},${c.y.toFixed(1)}`; continue; }
    const rc = Math.min(DEPS_RADIUS, l1/2, l2/2);
    const bx = c.x-(dx1/l1)*rc, by = c.y-(dy1/l1)*rc;
    const fx = c.x+(dx2/l2)*rc, fy = c.y+(dy2/l2)*rc;
    d += ` L${bx.toFixed(1)},${by.toFixed(1)} Q${c.x.toFixed(1)},${c.y.toFixed(1)} ${fx.toFixed(1)},${fy.toFixed(1)}`;
  }
  const l = pts[pts.length-1];
  return d + ` L${l.x.toFixed(1)},${l.y.toFixed(1)}`;
}

// ── Tracer une flèche pred → succ ─────────────────────────────────────────────
// Le chemin s'arrête à ARROW_TIP px du bord de la card dest.
// Le marqueur (triangle de longueur ARROW_TIP) s'étend de ce point jusqu'au bord.
// Résultat : la pointe touche exactement le bord, sans rentrer dans la card.
// ── Tracer une flèche pred → succ ─────────────────────────────────────────────
// allRects : tous les rects, pour détecter les obstacles dans le canal vertical.
// ── Tracer une flèche pred → succ ─────────────────────────────────────────────
function tracerFleche(pred, succ, allRects) {
  const x1 = pred.x + pred.w;        // bord droit pred
  const y1 = pred.y + pred.h / 2;    // milieu vertical pred
  const x2 = succ.x;                 // bord gauche succ
  const y2 = succ.y + succ.h / 2;    // milieu vertical succ
  const M  = 8;

  // ── Succ est à droite (rang supérieur) ────────────────────────────────────
  if (succ.rang > pred.rang) {

    // Cas 1 : même hauteur ET colonnes adjacentes → ligne droite
    if (Math.abs(y1 - y2) < 3 && succ.rang === pred.rang + 1) {
      return ptsVersSVG([{ x: x1, y: y1 }, { x: x2, y: y2 }]);
    }

    // Cas 2 : colonnes adjacentes, hauteurs différentes → Z dans le couloir
    if (succ.rang === pred.rang + 1) {
      const cx = (x1 + x2) / 2;
      return ptsVersSVG([
        { x: x1, y: y1 },
        { x: cx, y: y1 },
        { x: cx, y: y2 },
        { x: x2, y: y2 },
      ]);
    }

    // Cas 3 : saut de plusieurs colonnes → contourner par le bas
    // yPass = juste sous pred (pas sous toutes les cards de la zone)
    const yPass = pred.y + pred.h + CARD_GAP_Y * 0.75;

    // Sort par le bas de pred, longe sous tout, remonte dans succ par le bas.
    const xSuccMid   = x2 + succ.w / 2;
    const markerSize = 10;
    return ptsVersSVG([
      { x: pred.x + pred.w / 2, y: pred.y + pred.h            },
      { x: pred.x + pred.w / 2, y: yPass                       },
      { x: xSuccMid,             y: yPass                       },
      { x: xSuccMid,             y: succ.y + succ.h - markerSize },
    ]);
  }

  // ── Succ est à gauche ou même rang → contourner par le bas ────────────────
  const yPass = Math.max(pred.y + pred.h, succ.y + succ.h) + CARD_GAP_Y * 2;
  return ptsVersSVG([
    { x: pred.x + pred.w / 2, y: pred.y + pred.h },
    { x: pred.x + pred.w / 2, y: yPass            },
    { x: succ.x + succ.w / 2, y: yPass            },
    { x: succ.x + succ.w / 2, y: succ.y           },
  ]);
}

// ── Marqueur flèche ───────────────────────────────────────────────────────────
// markerUnits="strokeWidth" : markerWidth/Height sont en multiples du stroke-width.
// Avec stroke-width=2, markerWidth=5 → 10px, markerHeight=4 → 8px.
// refX=5 place la POINTE (x=5 dans l'espace marker) sur le bord de la card.
// Triangle : M0,0 L0,4 L5,2 Z — pointe à x=5 (bord droit du marker).
function creerMarqueur(NS, id, couleur) {
  const m = document.createElementNS(NS, 'marker');
  m.setAttribute('id',          id);
  m.setAttribute('markerWidth', '5');    // × stroke-width(2) = 10px réels
  m.setAttribute('markerHeight','4');    // × stroke-width(2) = 8px réels
  m.setAttribute('refX',        '5');    // pointe du triangle = point final du chemin
  m.setAttribute('refY',        '2');    // centre vertical
  m.setAttribute('orient',      'auto');
  m.setAttribute('markerUnits', 'strokeWidth');
  const tri = document.createElementNS(NS, 'path');
  tri.setAttribute('d',    'M0,0 L0,4 L5,2 Z');
  tri.setAttribute('fill', couleur);
  m.appendChild(tri);
  return m;
}

// ── Fonction principale ───────────────────────────────────────────────────────
function dessinerFleches(svgId) {
  const svg = document.getElementById(svgId);
  if (!svg) return;
  const wrap = svg.parentElement;
  const NS   = 'http://www.w3.org/2000/svg';

  layoutTopoCards(wrap);

  const W = wrap.scrollWidth, H = wrap.scrollHeight;
  svg.setAttribute('width',   W);
  svg.setAttribute('height',  H);
  svg.setAttribute('viewBox', `0 0 ${W} ${H}`);

  // Vider sans toucher aux defs (on les recrée complètement)
  while (svg.firstChild) svg.removeChild(svg.firstChild);

  const NS_SVG = 'http://www.w3.org/2000/svg';
  const idN    = `an-${svgId}`;
  const idR    = `ar-${svgId}`;
  const defs   = document.createElementNS(NS_SVG, 'defs');
  defs.appendChild(creerMarqueur(NS_SVG, idN, DEPS_COLOR));
  defs.appendChild(creerMarqueur(NS_SVG, idR, DEPS_COLOR_RETARD));
  svg.appendChild(defs);

  // Rects des cards
  const cardsMap = new Map();
  wrap.querySelectorAll('[data-tache-id]:not(.sous-tache-panel--hidden)').forEach(card => {
    cardsMap.set(card.dataset.tacheId, {
      x:      card.offsetLeft,
      y:      card.offsetTop,
      w:      card.offsetWidth,
      h:      card.offsetHeight,
      retard: card.dataset.retard === 'true',
    });
  });
  if (!cardsMap.size) return;

  // Calculer les rangs pour le routing (pred.rang < succ.rang = succ à droite)
  const cardsArr = Array.from(wrap.querySelectorAll('[data-tache-id]:not(.sous-tache-panel--hidden)'));
  const { ids, preds: predsG, succs: succsG } = construireGraphe(cardsArr);
  const rangMap = calculerRangs(ids, predsG, succsG);
  cardsMap.forEach((r, id) => { r.rang = rangMap.get(id) ?? 0; });

  // Flèches
  wrap.querySelectorAll('[data-predecesseurs]:not(.sous-tache-panel--hidden)').forEach(card => {
    const raw = (card.dataset.predecesseurs || '').trim();
    if (!raw) return;
    const succ = cardsMap.get(card.dataset.tacheId);
    if (!succ) return;

    raw.split(',').filter(Boolean).forEach(pid => {
      const pred = cardsMap.get(pid.trim());
      if (!pred) return;

      const allRects = [...cardsMap.values()];
      const couleur  = DEPS_COLOR;
      const mkId     = idN;
      const svgD     = tracerFleche(pred, succ, allRects);
      if (!svgD) return;

      const path = document.createElementNS(NS_SVG, 'path');
      path.setAttribute('d',               svgD);
      path.setAttribute('fill',            'none');
      path.setAttribute('stroke',          couleur);
      path.setAttribute('stroke-width',    String(DEPS_STROKE));
      path.setAttribute('stroke-linecap',  'round');
      path.setAttribute('stroke-linejoin', 'round');
      path.setAttribute('marker-end',      `url(#${mkId})`);
      svg.appendChild(path);
    });
  });
}

function initFleches() {
  dessinerFleches('deps-svg-detail');
  dessinerFleches('deps-svg-liste');

  let timer;
  window.addEventListener('resize', () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      dessinerFleches('deps-svg-detail');
      dessinerFleches('deps-svg-liste');
    }, 120);
  });
}

/* =============================================================================
   INITIALISATION
   ============================================================================= */

window.addEventListener('load', function () {
  initAlertesFlash();
  initValidationFormulaire();
  initFleches();
});

/* =============================================================================
   FILTRES SOUS-TÂCHES — Écran détail
   ============================================================================= */

function filtrerSousTaches() {
  const nom        = (document.getElementById('filtre-nom')?.value || '').toLowerCase().trim();
  const statut     = document.getElementById('filtre-statut')?.value || '';
  const dateDebut  = document.getElementById('filtre-date-debut')?.value || '';
  const dateFin    = document.getElementById('filtre-date-fin')?.value || '';
  const retardOnly = document.getElementById('filtre-retard')?.checked || false;

  const panels = document.querySelectorAll('.sous-tache-panel');
  let visibles = 0;

  const today = new Date().toISOString().split('T')[0];

  panels.forEach(panel => {
    const panelNom    = panel.dataset.nom || '';
    const panelDebut  = panel.dataset.debut || '';
    const panelFin    = panel.dataset.fin || '';
    const panelRetard = panel.dataset.retard === 'true';

    let visible = true;

    // Filtre nom
    if (nom && !panelNom.includes(nom)) visible = false;

    // Filtre retard uniquement
    if (retardOnly && !panelRetard) visible = false;

    // Filtre statut
    if (statut && visible) {
      if (statut === 'en-retard'  && !panelRetard) visible = false;
      if (statut === 'replanifie' && !panel.querySelector('.badge-replanifie')) visible = false;
      if (statut === 'a-venir'    && !(panelDebut > today)) visible = false;
      if (statut === 'en-cours'   && (panelRetard || panelDebut > today)) visible = false;
    }

    // Filtre plage de dates (intersection avec [panelDebut, panelFin])
    if (dateDebut && panelFin  && panelFin  < dateDebut) visible = false;
    if (dateFin   && panelDebut && panelDebut > dateFin)  visible = false;

    panel.classList.toggle('sous-tache-panel--hidden', !visible);
    if (visible) visibles++;
  });

  dessinerFleches('deps-svg-detail');
  dessinerFleches('deps-svg-liste');

  // Afficher message vide si aucun résultat
  const vide = document.getElementById('filtre-vide');
  if (vide) vide.style.display = (visibles === 0 && panels.length > 0) ? 'block' : 'none';
}

function reinitialiserFiltres() {
  const ids = ['filtre-nom', 'filtre-statut', 'filtre-date-debut', 'filtre-date-fin'];
  ids.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  const retard = document.getElementById('filtre-retard');
  if (retard) retard.checked = false;
  filtrerSousTaches();
}

/* =============================================================================
   ONGLETS — switchTab
   ============================================================================= */

function switchTab(tab) {
  const panels = {
    deps:    document.getElementById('panel-deps'),
    kanban:  document.getElementById('panel-kanban'),
    tdb:     document.getElementById('panel-tdb'),
    journal: document.getElementById('panel-journal'),
    dates:   document.getElementById('panel-dates'),
  };
  const tabs = {
    deps:    document.getElementById('tab-deps'),
    kanban:  document.getElementById('tab-kanban'),
    tdb:     document.getElementById('tab-tdb'),
    journal: document.getElementById('tab-journal'),
    dates:   document.getElementById('tab-dates'),
  };

  // Masquer tous les panels et désactiver tous les onglets
  Object.values(panels).forEach(p => { if (p) p.style.display = 'none'; });
  Object.values(tabs).forEach(t => { if (t) t.classList.remove('tab--active'); });

  // Activer le panel et l'onglet demandés
  if (panels[tab]) panels[tab].style.display = '';
  if (tabs[tab])   tabs[tab].classList.add('tab--active');

  if (tab === 'deps') {
    setTimeout(() => dessinerFleches('deps-svg-detail'), 30);
  } else if (tab === 'kanban') {
    updateKanbanCounts();
  } else if (tab === 'dates') {
    setTimeout(() => dessinerFrises(), 30);
  }
}

/* =============================================================================
   KANBAN — drag & drop
   ============================================================================= */

let draggedCardId = null;

function onDragStart(event) {
  const card = event.currentTarget;
  draggedCardId = card.dataset.id;
  card.classList.add('kanban-card--dragging');
  event.dataTransfer.effectAllowed = 'move';
}

function onDrop(event, statut) {
  event.preventDefault();
  document.querySelectorAll('.kanban-col__body--drag-over')
    .forEach(c => c.classList.remove('kanban-col__body--drag-over'));
  if (!draggedCardId) return;

  document.querySelectorAll('.kanban-card--dragging')
    .forEach(c => c.classList.remove('kanban-card--dragging'));

  const cardId = draggedCardId;
  draggedCardId = null;

  fetch(`/taches/${cardId}/statut`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `statut=${encodeURIComponent(statut)}`
  })
  .then(r => r.json().then(data => ({ ok: r.ok, status: r.status, data })))
  .then(({ ok, status, data }) => {
    if (!ok) {
      // Erreur métier (conflit prédécesseur etc.)
      const msg = data.erreur || 'Erreur lors du changement de statut.';
      afficherToast(msg, 'erreur');
      return;
    }
    // Déplacer la card visuellement
    const card = document.querySelector(`.kanban-card[data-id="${cardId}"]`);
    const body = document.querySelector(`.kanban-col[data-statut="${statut}"] .kanban-col__body`);
    if (card && body) body.appendChild(card);
    updateKanbanCounts();

    // Toast d'info si des tâches ascendantes ont été auto-terminées
    if (data.autoTermines && data.autoTermines.length > 0) {
      const noms = data.autoTermines.join(', ');
      afficherToast(
        `✅ Toutes les sous-tâches sont terminées — passage automatique en "Terminé" : ${noms}`,
        'info'
      );
    }
  })
  .catch(err => {
    console.error('Erreur changement statut:', err);
    afficherToast('Erreur réseau lors du changement de statut.', 'erreur');
  });
}

function updateKanbanCounts() {
  document.querySelectorAll('.kanban-col').forEach(col => {
    const count = col.querySelectorAll('.kanban-card').length;
    const badge = col.querySelector('.kanban-col__count');
    if (badge) badge.textContent = count;
  });
}

// Initialiser les compteurs au chargement
window.addEventListener('load', () => updateKanbanCounts());

function onDragEnd(event) {
  event.currentTarget.classList.remove('kanban-card--dragging');
}

function onDragOver(event) {
  event.preventDefault();
  event.currentTarget.classList.add('kanban-col__body--drag-over');
}

function onDragLeave(event) {
  event.currentTarget.classList.remove('kanban-col__body--drag-over');
}

/* =============================================================================
   TOAST — notifications légères
   ============================================================================= */

function afficherToast(message, type) {
  // type : 'info' | 'erreur'
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = 'toast toast--' + (type || 'info');
  toast.textContent = message;
  container.appendChild(toast);

  // Apparition
  requestAnimationFrame(() => toast.classList.add('toast--visible'));

  // Disparition après 4s
  setTimeout(() => {
    toast.classList.remove('toast--visible');
    toast.addEventListener('transitionend', () => toast.remove(), { once: true });
  }, 4000);
}

/* =============================================================================
   ONGLET DATES — frise chronologique + édition inline
   ============================================================================= */

// ---- Dessin de la frise + flèches de dépendances ----

function dessinerFrises() {
  const canvases = document.querySelectorAll('#panel-dates .frise-canvas');
  if (!canvases.length) return;

  // Calculer la plage globale : min(tous débuts) → max(toutes fins)
  let minDate = null, maxDate = null;
  canvases.forEach(c => {
    [c.dataset.debut, c.dataset.fin].filter(Boolean).forEach(d => {
      const dt = new Date(d);
      if (!minDate || dt < minDate) minDate = dt;
      if (!maxDate || dt > maxDate) maxDate = dt;
    });
  });

  if (!minDate || !maxDate) return;

  const span  = maxDate - minDate || 86400000; // au moins 1 jour
  const marge = span * 0.07;
  const rangeStart = new Date(minDate.getTime() - marge);
  const rangeEnd   = new Date(maxDate.getTime() + marge);
  const rangeMs    = rangeEnd - rangeStart;

  canvases.forEach(c => {
    const W = c.parentElement.clientWidth || 300;
    const H = 28;
    c.width  = W;
    c.height = H;
    const ctx = c.getContext('2d');
    ctx.clearRect(0, 0, W, H);

    const toX = d => Math.round(((new Date(d) - rangeStart) / rangeMs) * W);

    const debut      = c.dataset.debut;
    const fin        = c.dataset.fin;
    const estCorrige = c.dataset.finCorrigee === 'true';

    // Rail gris
    ctx.fillStyle = '#e8e4df';
    ctx.roundRect(0, H/2 - 3, W, 6, 3);
    ctx.fill();

    // Barre début → fin
    if (debut && fin) {
      const x1 = toX(debut);
      const x2 = toX(fin);
      ctx.fillStyle = estCorrige ? '#e8a87c' : '#b8aee0';
      ctx.roundRect(x1, H/2 - 6, Math.max(x2 - x1, 4), 12, 5);
      ctx.fill();
    }

    // Points
    const dot = (d, color, r) => {
      if (!d) return;
      ctx.beginPath();
      ctx.arc(toX(d), H/2, r, 0, Math.PI * 2);
      ctx.fillStyle = color;
      ctx.fill();
    };
    dot(debut, '#7a6fc0', 5);
    dot(fin,   estCorrige ? '#c0662a' : '#5a8cc0', 5);
  });

  // Dessiner les flèches après les canvas
  dessinerFlechesFrise();
}

function dessinerFlechesFrise() {
  const svg = document.getElementById('frise-svg');
  if (!svg) return;
  svg.innerHTML = ''; // reset

  const zone     = document.getElementById('frise-zone');
  const zoneRect = zone.getBoundingClientRect();

  // Index : id → canvas
  const canvasById = {};
  document.querySelectorAll('#panel-dates .frise-canvas[data-id]').forEach(c => {
    canvasById[c.dataset.id] = c;
  });

  // Calculer la plage de dates (même logique que dessinerFrises)
  let minDate = null, maxDate = null;
  Object.values(canvasById).forEach(c => {
    [c.dataset.debut, c.dataset.fin].filter(Boolean).forEach(d => {
      const dt = new Date(d);
      if (!minDate || dt < minDate) minDate = dt;
      if (!maxDate || dt > maxDate) maxDate = dt;
    });
  });
  if (!minDate || !maxDate) return;

  const span      = maxDate - minDate || 86400000;
  const marge     = span * 0.07;
  const rangeStart = new Date(minDate.getTime() - marge);
  const rangeEnd   = new Date(maxDate.getTime() + marge);
  const rangeMs    = rangeEnd - rangeStart;

  const toX = (canvas, dateStr) => {
    if (!dateStr) return null;
    const rect = canvas.getBoundingClientRect();
    const W    = rect.width;
    const frac = (new Date(dateStr) - rangeStart) / rangeMs;
    // position absolue dans la zone
    return (rect.left - zoneRect.left) + frac * W;
  };

  const midY = (canvas) => {
    const rect = canvas.getBoundingClientRect();
    return (rect.top - zoneRect.top) + rect.height / 2;
  };

  // Mettre à jour le SVG pour couvrir la zone
  svg.setAttribute('width',  zone.offsetWidth);
  svg.setAttribute('height', zone.offsetHeight);

  // Dessiner une flèche pour chaque dépendance
  document.querySelectorAll('#panel-dates .frise-canvas[data-predecesseurs]').forEach(cSucc => {
    const preds = cSucc.dataset.predecesseurs;
    if (!preds) return;
    preds.split(',').filter(Boolean).forEach(predId => {
      const cPred = canvasById[predId.trim()];
      if (!cPred) return; // prédécesseur hors-écran, ignoré

      const x1 = toX(cPred, cPred.dataset.fin);
      const y1 = midY(cPred);
      const x2 = toX(cSucc, cSucc.dataset.debut);
      const y2 = midY(cSucc);
      if (x1 === null || x2 === null) return;

      const dx   = Math.abs(x2 - x1);
      const cpX1 = x1 + Math.max(dx * 0.4, 20);
      const cpX2 = x2 - Math.max(dx * 0.4, 20);

      // Tracé
      const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      path.setAttribute('d', `M ${x1} ${y1} C ${cpX1} ${y1}, ${cpX2} ${y2}, ${x2} ${y2}`);
      path.setAttribute('fill', 'none');
      path.setAttribute('stroke', '#9b93c4');
      path.setAttribute('stroke-width', '1.8');
      path.setAttribute('stroke-dasharray', '5,3');
      path.setAttribute('opacity', '0.8');
      svg.appendChild(path);

      // Pointe de flèche
      const angle = Math.atan2(y2 - y1, x2 - x1) * 0; // flèche horizontale d'arrivée
      const arr = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
      const sz  = 7;
      // pointe vers la droite (x2,y2)
      arr.setAttribute('points',
        `${x2},${y2} ${x2 - sz},${y2 - sz * 0.5} ${x2 - sz},${y2 + sz * 0.5}`
      );
      arr.setAttribute('fill', '#9b93c4');
      arr.setAttribute('opacity', '0.9');
      svg.appendChild(arr);
    });
  });
}

// ---- Sauvegarde date de fin (input direct) ----

function sauvegarderDateFin(input) {
  const id  = input.dataset.id;
  const val = input.value;

  fetch(`/taches/${id}/dates`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `champ=dateFinCorrigee&valeur=${encodeURIComponent(val)}`
  })
  .then(r => {
    if (!r.ok) return r.text().then(t => { throw new Error(t); });
    // Mettre à jour la frise canvas
    const canvas = document.querySelector(`#panel-dates .frise-canvas[data-id="${id}"]`);
    if (canvas) {
      canvas.dataset.fin = val;
      canvas.dataset.finCorrigee = val ? 'true' : 'false';
    }
    // Marquer visuellement l'input
    if (val) {
      input.classList.add('frise-input-fin--corrigee');
    } else {
      input.classList.remove('frise-input-fin--corrigee');
    }
    // Mettre à jour l'icône d'alerte de la ligne
    const row = input.closest('.frise-row');
    if (row) {
      const alerteCol = row.querySelector('.frise-alerte-col');
      if (alerteCol) {
        const estDepasse = val && new Date(val + 'T00:00:00') < new Date(new Date().toDateString());
        alerteCol.innerHTML = estDepasse
          ? '<span class="frise-alerte-icon" title="Date de fin dépassée">🔴</span>'
          : '';
      }
    }
    dessinerFrises();
    afficherToast('Date de fin mise à jour.', 'info');
  })
  .catch(err => {
    afficherToast('Erreur : ' + err.message, 'erreur');
  });
}

// Redessiner si resize
window.addEventListener('resize', () => {
  if (document.getElementById('panel-dates')?.style.display !== 'none') {
    dessinerFrises();
  }
});