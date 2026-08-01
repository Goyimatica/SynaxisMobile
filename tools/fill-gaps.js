#!/usr/bin/env node
/*
 * Synaxis - fill the gaps the day-template harvest missed.  node tools/fill-gaps.js
 *
 * The V8 harvest (saints.json.bak) already contains OrthodoxWiki day-page
 * names WITH their article titles (`o`) and feast dates (`f`). It was culled
 * in V9 because the loose V8 parser let glossary words through - but the
 * names themselves are mostly real. So this pass takes every bak candidate
 * not already in saints.json and makes it prove itself with the exact same
 * category rule the V9+ harvest uses. Whatever survives is a real person
 * commemorated on that day; whatever does not (Abbess, Abbot, Akathist,
 * Afterfeast ...) is left behind again.
 *
 * Entries already in saints.json are never touched and never overwritten.
 */

const fs = require("fs");
const path = require("path");

const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "saints.json");
const BAK = OUT + ".bak";
const API = "https://orthodoxwiki.org/api.php";
const AGENT = "Synaxis/10.0 (Android reader; github.com/Goyimatica/SynaxisMobile)";

/* The same two category rules as harvest.js. */
const PERSON = new RegExp([
	"saints", "martyr", "hieromartyr", "bishops", "hierarch", "patriarch",
	"apostle", "prophet", "monastic", "abbot", "abbess", "nuns", "hermit",
	"ascetic", "confessor", "fools-for-christ", "unmerc", "wonderworker",
	"righteous", "equals-to-the-apostles", "church fathers", "desert fathers",
	"archbishops", "metropolitans", "presbyters", "deacons", "virgin"
].join("|"), "i");

const NOT_PERSON = new RegExp([
	"feasts", "fasts", "glossary", "liturgic", "places", "geograph",
	"jurisdiction", "diocese", "metropolis", "monasteries", "churches",
	"cathedral", "books", "hymns", "prayers", "icons", "councils",
	"church history", "calendar", "dictionar", "disambiguation", "lists",
	"oriental orthodox", "coptic orthodox", "armenian apostolic",
	"syriac orthodox", "ethiopian orthodox", "malankara"
].join("|"), "i");

/* The V8 harvest let glossary words through; these were the worst of them.
   A name on this list is refused before any category check is spent on it. */
const GLOSS = new Set([
	"abbess", "abbey", "abbot", "abbreviations", "acheiropoieta", "afterfeast",
	"akathist", "akolouthia", "anchorite", "angels", "apodosis", "apostate",
	"apostle", "archbishop", "bishop", "cathedral", "church", "deacon",
	"glossary", "hermit", "icon", "liturgy", "martyr", "monastery", "monk",
	"nun", "patriarch", "priest", "prophet", "relics", "saint", "synaxis",
	"theotokos", "translation of relics", "vespers", "matins", "fasting",
	"great lent", "pascha", "holy trinity", "jesus christ", "dormition",
	"feasts", "fasts", "saints", "other events", "cross procession",
	"adoration of the magi", "commemoration of the shepherds", "magi",
	"shepherds", "icons", "baptism of rus'", "baptism of rus", "holy land"
]);

function sleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }

async function api(query) {
	const url = API + "?format=json&formatversion=2&" + query;
	for (let attempt = 0; attempt < 3; attempt++) {
		try {
			const res = await fetch(url, { headers: { "User-Agent": AGENT } });
			if (res.ok) return await res.json();
		} catch (e) { /* retry */ }
		await sleep(700 * (attempt + 1));
	}
	return null;
}

function slug(name) {
	return name
		.toLowerCase()
		.replace(/[\u2018\u2019']/g, "")
		.replace(/[^a-z0-9]+/g, "-")
		.replace(/^-+|-+$/g, "")
		.slice(0, 48);
}

/* Categories in batches of forty. */
async function verify(titles) {
	const good = new Set();
	for (let i = 0; i < titles.length; i += 40) {
		const slice = titles.slice(i, i + 40);
		const j = await api(
			"action=query&prop=categories&cllimit=500&titles=" +
			slice.map(encodeURIComponent).join("|")
		);
		const pages = (j && j.query && j.query.pages) || [];
		pages.forEach(function (p) {
			if (p.missing) return;
			const cats = (p.categories || [])
				.map(function (c) { return String(c.title).replace(/^Category:/, ""); })
				.join(" | ");
			if (!cats) return;
			if (NOT_PERSON.test(cats)) return;
			if (!PERSON.test(cats)) return;
			good.add(p.title);
		});
		await sleep(150);
	}
	return good;
}

function main() {
	if (typeof fetch !== "function") {
		console.error("  x  this needs Node 18 or newer");
		process.exit(1);
	}

	const cur = JSON.parse(fs.readFileSync(OUT, "utf8"));
	const bak = JSON.parse(fs.readFileSync(BAK, "utf8"));

	const byId = new Map();
	const known = new Set();
	cur.forEach(function (s) {
		byId.set(s.id, s);
		known.add(String(s.n).toLowerCase());
		if (s.o) known.add(String(s.o).toLowerCase());
	});

	/* Candidates: bak people, with a feast and a title, not already known,
	   and not a glossary word. */
	const candidates = [];
	const seen = new Set();
	bak.forEach(function (s) {
		if (s.k && s.k !== "saint") return;
		if (!s.f || !s.o) return;
		const n = String(s.n || "").trim();
		const lower = n.toLowerCase();
		if (!n || seen.has(lower)) return;
		if (GLOSS.has(lower)) return;
		if (known.has(lower) || known.has(String(s.o).toLowerCase())) return;
		seen.add(lower);
		candidates.push(s);
	});

	console.log("  .  " + candidates.length + " bak candidates to verify");

	mainAsync(candidates, byId, known, bak).catch(function (e) {
		console.error("  x  " + e.message);
		process.exit(1);
	});
}

async function mainAsync(candidates, byId, known, bak) {
	/* The day template that first lists this article, for dates the bak
	   itself never recorded. */
	const dayOf = new Map();
	bak.forEach(function (s) {
		if (s.f && !dayOf.has(String(s.o).toLowerCase())) {
			dayOf.set(String(s.o).toLowerCase(), s.f);
		}
	});

	const titles = candidates.map(function (s) { return s.o; });
	const verified = await verify(titles);
	console.log("  .  " + verified.size + " of them are people");

	const added = [];
	candidates.forEach(function (s) {
		const title = String(s.o);
		if (!verified.has(title)) return;
		if (known.has(title.toLowerCase())) return;
		known.add(title.toLowerCase());

		const n = String(s.n || "").trim();
		const e = String(s.e || "").trim();
		const key = s.f || dayOf.get(title.toLowerCase()) || null;
		let id = slug(n) || slug(title);
		if (!id) return;
		if (byId.has(id) && key) id = id + "-" + key.replace("-", "");
		if (byId.has(id)) return;

		const entry = {
			id: id,
			n: n,
			e: e,
			f: key,
			fl: null,
			era: "",
			j: "",
			w: "",
			o: title,
			b: [],
			c: null,
			note: null,
			pending: false,
			k: "saint"
		};
		byId.set(id, entry);
		added.push(entry);
	});

	const all = Array.from(byId.values());
	all.sort(function (a, b) { return String(a.n).localeCompare(String(b.n), "en"); });
	fs.writeFileSync(OUT, JSON.stringify(all));

	const saints = all.filter(function (s) { return s.k === "saint"; }).length;
	console.log("");
	console.log("  added     " + added.length);
	console.log("  saints    " + saints);
	console.log("  subjects  " + (all.length - saints));
	console.log("  total     " + all.length);
	console.log("  ->        " + OUT);

	if (added.length > 0) {
		console.log("");
		console.log("  added entries:");
		added.forEach(function (s) {
			console.log("    " + s.f + "  " + s.n + (s.e ? ", " + s.e : "") + "  [" + s.o + "]");
		});
	}
}

main();
