#!/usr/bin/env node
/*
 * Synaxis - audit saints.json against the calendars themselves.
 *   node tools/audit.js
 *
 * Two checks, both against authoritative sources:
 *
 *   1. FEAST DATES.  For every saint with a feast `f`, ask two sources what
 *      is commemorated on that church date - Wikipedia's "Month D (Eastern
 *      Orthodox liturgics)" pages and OrthodoxWiki's day templates.  If the
 *      saint is not on their stored day but IS on the day thirteen days
 *      earlier or later, their *civil* date was stored instead of their
 *      *church* date (the classic 13-day slip) and the report says which way
 *      to move them.  If they are nowhere at all, they are listed for a
 *      hand check - many are recent canonizations the sources have not yet
 *      templated.
 *
 *   2. JURISDICTION.  For every saint with an OrthodoxWiki title, fetch the
 *      article's categories.  Categories that mark a post-schism
 *      non-Chalcedonian church (Non-Chalcedonian, Coptic, Armenian
 *      Apostolic, Syriac, Ethiopian Tewahedo, Malankara, Church of the
 *      East) are reported for removal.  Pre-schism Ecumenical saints
 *      (Gregory the Enlightener, Frumentius, Moses the Black) carry the
 *      cultural categories too but are genuinely Eastern Orthodox - so
 *      every hit is reported with its categories and the decision is made
 *      by hand after research.
 *
 * The tool only reports; it never writes saints.json.
 */

const fs = require("fs");
const path = require("path");

const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "saints.json");
const OW_API = "https://orthodoxwiki.org/api.php";
const WP_API = "https://en.wikipedia.org/w/api.php";
const AGENT = "Synaxis/audit (Android reader; github.com/Goyimatica/SynaxisMobile)";

const MONTHS = [
	"January", "February", "March", "April", "May", "June",
	"July", "August", "September", "October", "November", "December"
];
const DAYS_IN = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

/* Categories that say "post-schism non-Chalcedonian church".  Pre-schism
   Ecumenical saints are reported too (their cultural categories match) but
   are kept after research. */
const FOREIGN = new RegExp([
	"non-chalcedonian", "coptic orthodox", "ethiopian orthodox",
	"syriac orthodox", "armenian apostolic", "malankara",
	"assyrian church", "church of the east", "jacobite", "miaphysite",
	"tewahedo"
].join("|"), "i");

/* The same but broader, so the report also shows cultural-only hits like
   "Armenian Saints" or "Coptic Saints" for the hand-check list. */
const FOREIGN_WIDE = new RegExp([
	"non-chalcedonian", "coptic", "ethiopian", "syriac", "armenian",
	"malankara", "assyrian", "jacobite", "miaphysite", "tewahedo",
	"church of the east"
].join("|"), "i");

/* 2026-08-02: schismatic and non-canonical bodies are not the Church, so a
   figure whose article is categorised under one of them is not an Eastern
   Orthodox saint either - however holy their biography reads.  ROCOR is
   deliberately absent: it has been in communion since 2007. */
const SCHISM = new RegExp([
	"old believer", "old-believer", "starover", "old calendarist",
	"old-calendarist", "matthewite", "florinite", "gennadios",
	"true orthodox", "catacomb", "renovationist", "living church",
	"autocephalous church of ukraine", "ukrainian autocephalous",
	"uncanonical", "self-consecrated", "denisenko"
].join("|"), "i");

/* Words that could match a neutral or legitimate use - a council that
   deposed a heretic, a saint deposed by heretics (Ignatius of
   Constantinople), the restored Macedonian church (in communion again
   since 2022) - reported but softer, decided by hand. */
const SCHISM_WIDE = new RegExp([
	"old believer", "old-believer", "starover", "old calendarist",
	"old-calendarist", "matthewite", "florinite", "gennadios",
	"true orthodox", "catacomb", "renovationist", "living church",
	"autocephalous church of ukraine", "ukrainian autocephalous",
	"uncanonical", "self-consecrated", "denisenko", "macedonian orthodox",
	"montenegrin orthodox", "excommunicated", "defrocked", "deposed",
	"schism", "synod"
].join("|"), "i");

function pad(n) { return n < 10 ? "0" + n : String(n); }
function sleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }

/* fold "Stăniloae" -> "staniloae" so a diacritic can never hide a saint
   from the matcher - the 2026-08-02 run flagged Dumitru Stăniloae purely
   because "ă" split his name in two and no day matched it. */
function fold(s) {
	return String(s).normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
}

async function api(base, query, pause) {
	const url = base + "?format=json&formatversion=2&" + query;
	for (let attempt = 0; attempt < 3; attempt++) {
		try {
			const res = await fetch(url, { headers: { "User-Agent": AGENT } });
			if (res.ok) return await res.json();
		} catch (e) { /* retry */ }
		await sleep(pause * (attempt + 1));
	}
	return null;
}

/* significant words of a name: length > 3, not an honourific. */
const STOP = new Set([
	"saint", "saints", "st", "sts", "holy", "venerable", "blessed",
	"righteous", "right-believing", "new", "newly", "great", "martyr",
	"hieromartyr", "martyr", "prophet", "apostle", "evangelist", "father",
	"the", "of", "and", "with", "in", "at", "for"
]);

function sigWords(s) {
	return fold(s)
		.split(/[^a-z0-9]+/)
		.filter(function (w) { return w.length > 3 && !STOP.has(w); });
}

/* Does this day text mention this saint?  All the significant words of any
   one candidate title must be present. */
function mentions(text, titles) {
	const t = fold(text);
	for (const title of titles) {
		const words = sigWords(title);
		if (words.length === 0) continue;
		if (words.every(function (w) { return t.indexOf(w) !== -1; })) return true;
	}
	return false;
}

/* church date -> civil date (old style is +13). */
function civilKey(churchKey, shift) {
	const m = parseInt(churchKey.slice(0, 2), 10);
	const d = parseInt(churchKey.slice(3), 10);
	const date = new Date(Date.UTC(2026, m - 1, d + shift));
	return pad(date.getUTCMonth() + 1) + "-" + pad(date.getUTCDate());
}

async function wpLiturgics(key) {
	const day = parseInt(key.slice(3), 10);
	const month = MONTHS[parseInt(key.slice(0, 2), 10) - 1];
	const j = await api(WP_API, "action=parse&prop=wikitext&page=" +
		encodeURIComponent(month + " " + day + " (Eastern Orthodox liturgics)"), 60);
	return (j && j.parse && j.parse.wikitext) ? String(j.parse.wikitext) : "";
}

async function owDay(key) {
	const day = parseInt(key.slice(3), 10);
	const month = MONTHS[parseInt(key.slice(0, 2), 10) - 1];
	const j = await api(OW_API, "action=parse&prop=wikitext&page=" +
		encodeURIComponent("Template:" + month + " " + day), 70);
	if (j && j.parse && j.parse.wikitext) return String(j.parse.wikitext);
	const k = await api(OW_API, "action=parse&prop=wikitext&page=" +
		encodeURIComponent(month + " " + day), 70);
	return (k && k.parse && k.parse.wikitext) ? String(k.parse.wikitext) : "";
}

async function main() {
	if (typeof fetch !== "function") {
		console.error("  x  this needs Node 18 or newer");
		process.exit(1);
	}
const data = JSON.parse(fs.readFileSync(OUT, "utf8"));
const people = data.filter(function (s) { return s.k === "saint" && s.f; });
console.log("  .  " + people.length + " saints with a feast to audit");

/* Saints whose stored church date is verified correct even though the day
   templates disagree.  New-calendar saints are feasted on their civil
   repose date; OrthodoxWiki templates file them under the old-style date
   and the naive check would chase them 13 days back forever.  Verified
   2026-08-03 against the churches' own calendars and the saints' articles:
   Romania feasts Stăniloae on 4 October, Greece feasts Savvas on 7 April
   (his OrthodoxWiki article says exactly that). */
const KEEP_DATE = new Set([
	"Dumitru Staniloae",
	"Savvas the New of Kalymnos",
]);

/* Pre-schism Ecumenical saints OrthodoxWiki files under the modern
   non-Chalcedonian category by cultural continuity.  All five are genuine
   Eastern Orthodox commemorations (Greek and Slavic calendars) - keep
   them.  Verified 2026-08-03: Anianus's feast is 25 April (Chalcedonian,
   Holweck), Frumentius sits on OrthodoxWiki's own Template:November_30,
   Gregory the Enlightener and Rhipsime are EO feasts on 30 September. */
const KEEP_FOREIGN = new Set([
	"Anianus of Alexandria",
	"Frumentius of Axum",
	"Gregentios of Himyaritia",
	"Gregory the Enlightener",
	"Rhipsime of Armenia",
]);

	/* ---- 1. fetch the calendar text for every church date, both sources - */
	const dayText = {};
	for (let m = 1; m <= 12; m++) {
		for (let d = 1; d <= DAYS_IN[m - 1]; d++) {
			const key = pad(m) + "-" + pad(d);
			const wp = await wpLiturgics(key);
			const ow = await owDay(key);
			dayText[key] = wp + "\n" + ow;
		}
		console.log("  .  fetched " + MONTHS[m - 1]);
	}

	/* ---- 2. feast-date check ------------------------------------------- */
	console.log("");
	console.log("=== FEAST DATE CHECK ===");
	const move = [];
	const notFound = [];
	people.forEach(function (s) {
		if (KEEP_DATE.has(s.o)) return;                 // verified correct; templates disagree
		const titles = [s.o, s.n].filter(Boolean);
		const here = dayText[s.f] || "";
		if (mentions(here, titles)) return;                 // right day
		const back = civilKey(s.f, -13);
		const fwd = civilKey(s.f, +13);
		const backHit = mentions(dayText[back] || "", titles);
		const fwdHit = mentions(dayText[fwd] || "", titles);
		if (backHit && !fwdHit) {
			move.push({ s: s, from: s.f, to: back, dir: "back 13 (civil->church)" });
		} else if (fwdHit && !backHit) {
			move.push({ s: s, from: s.f, to: fwd, dir: "forward 13" });
		} else if (backHit && fwdHit) {
			notFound.push({ s: s, why: "found on both +-13; hand-check: " + back + " / " + fwd });
		} else {
			notFound.push({ s: s, why: "nowhere nearby; recent canonization or variant name" });
		}
	});
	console.log("  " + move.length + " saints on the wrong day (should move):");
	move.forEach(function (m) {
		console.log("    " + m.s.f + " -> " + m.to + "  " + m.s.n +
			"  [" + (m.s.o || m.s.w || "") + "]  (" + m.dir + ")");
	});
	console.log("  " + notFound.length + " saints not found on their stored day:");
	notFound.forEach(function (m) {
		console.log("    " + m.s.f + "  " + m.s.n +
			"  [" + (m.s.o || m.s.w || "") + "]  - " + m.why);
	});

	/* ---- 3. jurisdiction check ----------------------------------------- */
	console.log("");
	console.log("=== JURISDICTION CHECK ===");
	const titles = Array.from(new Set(people
		.map(function (s) { return s.o; })
		.filter(Boolean)));
	const cat = {};
	for (let i = 0; i < titles.length; i += 40) {
		const slice = titles.slice(i, i + 40);
		const j = await api(OW_API,
			"action=query&prop=categories&cllimit=500&titles=" +
			slice.map(encodeURIComponent).join("|"), 150);
		const pages = (j && j.query && j.query.pages) || [];
		pages.forEach(function (p) {
			cat[p.title] = (p.categories || []).map(function (c) {
				return String(c.title).replace(/^Category:/, "");
			});
		});
	}
	const flagged = [];
	people.forEach(function (s) {
		if (KEEP_FOREIGN.has(s.o)) return;              // pre-schism EO, kept on purpose
		const c = cat[s.o] || [];
		if (FOREIGN_WIDE.test(c.join(" | "))) {
			flagged.push({ s: s, cats: c.filter(function (x) { return FOREIGN_WIDE.test(x); }) });
		}
	});
	console.log("  " + flagged.length + " saints whose article carries non-Eastern-Orthodox category words:");
	flagged.forEach(function (f) {
		const hard = FOREIGN.test(f.cats.join(" "));
		console.log("    " + (hard ? "[HARD] " : "[soft] ") + f.s.f + "  " + f.s.n +
			"  [" + f.s.o + "]  {" + f.cats.join(", ") + "}");
	});

	/* ---- 4. schism check ------------------------------------------------ */
	console.log("");
	console.log("=== SCHISM CHECK ===");
	const sch = [];
	people.forEach(function (s) {
		const c = cat[s.o] || [];
		if (SCHISM_WIDE.test(c.join(" | "))) {
			sch.push({ s: s, cats: c.filter(function (x) { return SCHISM_WIDE.test(x); }) });
		}
	});
	console.log("  " + sch.length + " saints whose article carries schismatic/non-canonical category words:");
	sch.forEach(function (f) {
		const hard = SCHISM.test(f.cats.join(" "));
		console.log("    " + (hard ? "[HARD] " : "[soft] ") + f.s.f + "  " + f.s.n +
			"  [" + f.s.o + "]  {" + f.cats.join(", ") + "}");
	});

	/* ---- report ---------------------------------------------------------- */
	console.log("");
	const hardForeign = flagged.filter(function (f) { return FOREIGN.test(f.cats.join(" ")); }).length;
	const hardSchism = sch.filter(function (f) { return SCHISM.test(f.cats.join(" ")); }).length;
	const bad = move.length + hardForeign + hardSchism;
	if (bad === 0 && notFound.length === 0) {
		console.log("  clean: every feast date checks out");
	} else {
		console.log("  " + bad + " definite fix(es): " + move.length + " date moves, " +
			hardForeign + " foreign entries, " + hardSchism + " schismatic entries");
		console.log("  " + notFound.length + " hand-checks (recent canonizations / variant names)");
		if (bad > 0) process.exitCode = 1;
	}
}

main();
