#!/usr/bin/env node
/*
 * Synaxis - apply the 2026-08-02 calendar corrections.  node tools/fix-calendar.js
 *
 * Every move and cull below was verified on 2026-08-02 against:
 *   - Wikipedia "Month D (Eastern Orthodox liturgics)" pages
 *   - OrthodoxWiki day templates and article categories
 *   - web research on each ambiguous figure
 *
 * MOVES: a saint whose feast `f` was stored on their *civil* date instead of
 * their *church* date is 13 days late (old style is civil + 13).  The audit
 * found each of them on the day thirteen back, so they move back 13.
 *
 * CULLS: figures that are not Eastern Orthodox saints at all - Coptic-only
 * popes (Cyril V), condemned writers (Evagrius Ponticus), deposed unionist
 * patriarchs (John XI Bekkos), primates never canonized (Theodosius VI,
 * Gregory II), a canonist never glorified (Matthew Blastares), a Coptic
 * double of St Mercurius (Philopater), a movable feast pinned to a wrong
 * fixed date (Joseph the Betrothed), and genuine duplicates.
 *
 * Entries are matched by their OrthodoxWiki title `o`, which is unique per
 * person, not by id (ids are unstable slugs).
 */

const fs = require("fs");
const path = require("path");
const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "saints.json");

/* o-title -> new feast.  Civil date stored; church date is 13 back. */
const MOVES = {
	"Silouan the Athonite": "09-11",
	"Xenia of St. Petersburg": "01-24",
	"Seraphim Rose": "08-20",
	"Alexander Nevsky": "08-30",
	"Basil of Ostrog": "04-29",
	"Cleopa Ilie": "11-19",
	"Ilie Lacatusu": "07-09",
	"Elizabeth the New Martyr": "07-05",
	"Gabriel Urgebadze": "10-20",
	"Jonah of Hankou": "10-07",
	"Justin Popovich": "06-01",
	"Peter of Cetinje": "10-18",
	"Sava of Serbia": "01-14",
	"Sebastian Dabovich": "11-17",
	"Seraphim of Vyritsa": "03-21",
	"Seraphim Sobolev": "02-13",
	"Royal Martyrs of Russia": "07-04",
	"Tikhon of Athos": "09-10",
	"Hilarion (Troitsky) of Verey": "12-15",
	"John Maximovitch": "06-19",
	"Lazar of Serbia": "06-15",
	"Ephraim of Katounakia": "02-14",
	"Simeon the Myrrhstreaming": "02-13",
	"Clement of Rome": "11-25",   // main feast Nov 25; the 01-04 slot is the Synaxis
};

/* o-title to remove.  For duplicates where the same person is listed twice,
   the one on the wrong date is culled and the right-dated one stays. */
const CULLS = {
	"Cyril V (John) of Alexandria": null,           // Coptic Pope 1874-1927
	"Evagrius Ponticus": null,                       // condemned at Fifth Ecumenical Council
	"John XI Bekkos of Constantinople": null,        // deposed unionist, never a saint
	"Theodosius VI (Abourjaily) of Antioch": null,   // never canonized
	"Gregory II of Constantinople": null,            // never canonized
	"Matthew Blastares": null,                       // never canonized
	"Philopater Mercurius": null,                    // Coptic name for St Mercurius (dup 11-24)
	"Amphilochios Makris": null,                     // dup of "Amphilochios (Makris) of Patmos" 04-03
	"Justin the Philosopher": null,                  // dup of "Justin Martyr" 06-01
	"Joseph the Betrothed": null,                    // movable only; no fixed date
	"Cosmas of Aetolia": null,                       // dup of "Cosmas of Aitolia" 08-24 (main feast)
	"Catherine of Alexandria": "11-25",              // dup of catherine-of-alexandria 11-24
	"Chinese New Martyrs": null,                     // dup of "Martyrs of China" 06-11
	"John (Maximovitch) the Wonderworker": null,     // wrong date; real St John of Shanghai is 06-19
};

/* dup pairs culled by (o-title, feast) where o alone is ambiguous */
const CULLS_BY_FEAST = {
	"Acepsimus, Joseph, and Aeithalas": ["09-01"],   // real date 11-03
	"Job": ["05-06"],                                 // dup of "Job the Long-suffering"
	"Sophia, the ascetic of Kleisoura": ["05-06"],    // dup of "Sophia of Kleisoura"
	"Andrei Rublev": ["07-04"],                       // dup of "Andrew Rublev"
};

/* o-title -> { o, n } corrections (right person, wrong article title).
   NB: "Vladimir of Kiev" is the GRAND PRINCE (OrthodoxWiki: "commemorated
   by the Church on July 15") and must keep that title; only the Metropolitan
   hieromartyr (feast 01-25) is Vladimir (Bogoyavlensky).  Matched by id so
   the two can never be confused again. */
const RETITLE = {
	"vladimir-kiev": { o: "Vladimir (Bogoyavlensky) of Kiev", n: "St Vladimir (Bogoyavlensky)" },
};

const data = JSON.parse(fs.readFileSync(OUT, "utf8"));
let moved = 0, culled = 0, retitled = 0;
const empties = new Set();

function byO(o) { return data.filter(function (s) { return s.o === o; }); }

Object.keys(MOVES).forEach(function (o) {
	const hits = byO(o);
	if (hits.length === 0) { console.log("  !  move target not found: " + o); return; }
	hits.forEach(function (s) {
		console.log("  move  " + s.id + "  " + (s.f || "-") + " -> " + MOVES[o] + "  " + s.n);
		s.f = MOVES[o];
		moved++;
	});
});

Object.keys(CULLS).forEach(function (o) {
	const hits = byO(o);
	if (hits.length === 0) { console.log("  !  cull target not found: " + o); return; }
	hits.forEach(function (s) {
		if (CULLS[o] && s.f !== CULLS[o]) return;  // keep the right-dated one
		console.log("  cull  " + s.id + "  " + s.n + "  [" + o + "] @" + s.f);
		empties.add(s.f);
		const i = data.indexOf(s);
		data.splice(i, 1);
		culled++;
	});
});

Object.keys(CULLS_BY_FEAST).forEach(function (o) {
	byO(o).forEach(function (s) {
		if (!CULLS_BY_FEAST[o].includes(s.f)) return;
		console.log("  cull  " + s.id + "  " + s.n + "  [" + o + "] @" + s.f);
		empties.add(s.f);
		const i = data.indexOf(s);
		data.splice(i, 1);
		culled++;
	});
});

Object.keys(RETITLE).forEach(function (o) {
	byO(o).forEach(function (s) {
		const t = RETITLE[o];
		if (t.o) s.o = t.o;
		if (t.n) s.n = t.n;
		console.log("  title " + s.id + "  -> " + s.n + "  [" + s.o + "]");
		retitled++;
	});
});

data.sort(function (a, b) { return String(a.n).localeCompare(String(b.n), "en"); });
fs.writeFileSync(OUT, JSON.stringify(data));

/* report dates that may have lost their only saint (filled by the CI harvest) */
const byFeast = {};
data.forEach(function (s) { if (s.f) (byFeast[s.f] = byFeast[s.f] || []).push(s); });
const nowEmpty = Array.from(empties).filter(function (f) { return !byFeast[f]; });

console.log("");
console.log("  moved   " + moved);
console.log("  culled  " + culled);
console.log("  titled  " + retitled);
console.log("  total   " + data.length + " (was " + data.length + " before save)");
if (nowEmpty.length) {
	console.log("  EMPTY DATES after culls (CI harvest will refill): " + nowEmpty.join(" "));
}
