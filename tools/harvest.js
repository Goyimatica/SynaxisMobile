#!/usr/bin/env node
/*
 * Synaxis - harvest OrthodoxWiki, carefully.  V9.
 *
 *   node tools/harvest.js
 *
 * Two rules, both of which V8 lacked:
 *
 *   1. Only the FIRST link of each bullet line on a day page is a
 *      commemoration. Everything else on that line is a place, a see or a
 *      glossary word, and is ignored.
 *   2. Every candidate must then prove it is a person by its own categories.
 *      Saints, martyrs, hierarchs, monastics, ascetics, apostles, prophets.
 *      Anything categorised as a feast, a fast, a place, a jurisdiction or a
 *      glossary entry is refused even if it also looks saintly.
 *
 * Entries already in saints.json are never touched and never overwritten.
 */

const fs = require("fs");
const path = require("path");

const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "saints.json");
const API = "https://orthodoxwiki.org/api.php";
const AGENT = "Synaxis/9.0 (Android reader; github.com/Goyimatica/SynaxisMobile)";

const MONTHS = [
	"January", "February", "March", "April", "May", "June",
	"July", "August", "September", "October", "November", "December"
];
const DAYS_IN = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

/* A category containing any of these means: this article is a person. */
const PERSON = new RegExp([
	"saints", "martyr", "hieromartyr", "bishops", "hierarch", "patriarch",
	"apostle", "prophet", "monastic", "abbot", "abbess", "nuns", "hermit",
	"ascetic", "confessor", "fools-for-christ", "unmerc", "wonderworker",
	"righteous", "equals-to-the-apostles", "church fathers", "desert fathers",
	"archbishops", "metropolitans", "presbyters", "deacons", "virgin"
].join("|"), "i");

/* A category containing any of these means: whatever else it is, not a person. */
const NOT_PERSON = new RegExp([
	"feasts", "fasts", "glossary", "liturgic", "places", "geograph",
	"jurisdiction", "diocese", "metropolis", "monasteries", "churches",
	"cathedral", "books", "hymns", "prayers", "icons", "councils",
	"church history", "calendar", "dictionar", "disambiguation", "lists"
].join("|"), "i");

/* Titles that are never a commemoration, whatever their categories say. */
const BLOCK = new Set([
	"abbot", "abbess", "abbey", "abbreviations", "holy land", "dormition",
	"baptism of rus'", "baptism of rus", "monk", "nun", "priest", "deacon",
	"bishop", "archbishop", "metropolitan", "patriarch", "saint", "martyr",
	"icon", "relics", "monastery", "cathedral", "church", "liturgy",
	"vespers", "matins", "fasting", "great lent", "pascha", "theotokos",
	"jesus christ", "holy trinity", "apostle", "prophet", "synaxis",
	"translation of relics", "main page", "orthodox church"
]);

/* Subjects, added as themselves - feasts, fasts and the faith. */
const TOPICS = [
	["feast", "Pascha"], ["feast", "Nativity of the Theotokos"],
	["feast", "Exaltation of the Holy Cross"], ["feast", "Entrance of the Theotokos"],
	["feast", "Nativity of Christ"], ["feast", "Theophany"],
	["feast", "Meeting of the Lord"], ["feast", "Annunciation"],
	["feast", "Palm Sunday"], ["feast", "Ascension"], ["feast", "Pentecost"],
	["feast", "Transfiguration"], ["feast", "Dormition"],
	["feast", "Circumcision of Christ"], ["feast", "Protection of the Theotokos"],
	["feast", "Procession of the Precious Cross"], ["feast", "Sunday of Orthodoxy"],
	["feast", "Holy Week"], ["feast", "Great and Holy Friday"],
	["feast", "Bright Week"], ["feast", "Sunday of All Saints"],
	["feast", "Baptism of Rus'"],

	["fast", "Fasting"], ["fast", "Great Lent"], ["fast", "Nativity Fast"],
	["fast", "Apostles Fast"], ["fast", "Dormition Fast"], ["fast", "Xerophagy"],
	["fast", "Fast-free week"], ["fast", "Cheesefare Week"],
	["fast", "Meatfare Sunday"], ["fast", "Clean Monday"],

	["topic", "Church calendar"], ["topic", "Julian calendar"],
	["topic", "Revised Julian calendar"], ["topic", "Paschalion"],
	["topic", "Menaion"], ["topic", "Triodion"], ["topic", "Pentecostarion"],
	["topic", "Octoechos"], ["topic", "Typikon"], ["topic", "Synaxarion"],

	["topic", "Divine Liturgy"],
	["topic", "Divine Liturgy of St. John Chrysostom"],
	["topic", "Divine Liturgy of St. Basil the Great"],
	["topic", "Presanctified Liturgy"], ["topic", "Vespers"], ["topic", "Matins"],
	["topic", "Compline"], ["topic", "Hours"], ["topic", "All-Night Vigil"],
	["topic", "Prokeimenon"], ["topic", "Troparion"], ["topic", "Kontakion"],
	["topic", "Akathist"], ["topic", "Canon"],

	["topic", "Holy Mysteries"], ["topic", "Baptism"], ["topic", "Chrismation"],
	["topic", "Eucharist"], ["topic", "Confession"], ["topic", "Holy Unction"],
	["topic", "Marriage"], ["topic", "Ordination"],

	["topic", "Ecumenical Councils"], ["topic", "First Ecumenical Council"],
	["topic", "Second Ecumenical Council"], ["topic", "Third Ecumenical Council"],
	["topic", "Fourth Ecumenical Council"], ["topic", "Fifth Ecumenical Council"],
	["topic", "Sixth Ecumenical Council"], ["topic", "Seventh Ecumenical Council"],
	["topic", "Nicene-Constantinopolitan Creed"], ["topic", "Great Schism"],

	["topic", "Theosis"], ["topic", "Hesychasm"], ["topic", "Jesus Prayer"],
	["topic", "Prayer"], ["topic", "Prayer Rule"], ["topic", "Repentance"],
	["topic", "Almsgiving"], ["topic", "Passions"], ["topic", "Nous"],
	["topic", "Grace"], ["topic", "Uncreated Light"],
	["topic", "Essence and energies"],

	["topic", "Icon"], ["topic", "Iconostasis"], ["topic", "Iconoclasm"],
	["topic", "Church architecture"], ["topic", "Incense"], ["topic", "Prosphora"],
	["topic", "Antidoron"], ["topic", "Holy water"], ["topic", "Relics"],
	["topic", "Myrrh-streaming icon"],

	["topic", "Monasticism"], ["topic", "Mount Athos"], ["topic", "Skete"],
	["topic", "Lavra"], ["topic", "Elder"], ["topic", "Schema"],
	["topic", "Philokalia"], ["topic", "Desert Fathers"], ["topic", "Fool-for-Christ"],

	["topic", "Bishop"], ["topic", "Priest"], ["topic", "Deacon"],
	["topic", "Patriarch"], ["topic", "Autocephaly"],
	["topic", "Church of Constantinople"], ["topic", "Church of Russia"],
	["topic", "Church of Greece"], ["topic", "Church of Romania"],
	["topic", "Church of Georgia"], ["topic", "Church of Serbia"],
	["topic", "Orthodox Church in America"], ["topic", "ROCOR"],
	["topic", "Church of Antioch"], ["topic", "Church of Alexandria"],
	["topic", "Church of Jerusalem"],

	["topic", "Theotokos"], ["topic", "Holy Trinity"], ["topic", "Incarnation"],
	["topic", "Resurrection"], ["topic", "Second Coming"], ["topic", "Toll houses"],
	["topic", "Memorial service"], ["topic", "Saint"], ["topic", "Glorification"],
	["topic", "Martyr"]
];

function sleep(ms) {
	return new Promise(function (r) { setTimeout(r, ms); });
}

async function api(query) {
	const url = API + "?format=json&formatversion=2&" + query;
	for (let attempt = 0; attempt < 3; attempt++) {
		try {
			const res = await fetch(url, { headers: { "User-Agent": AGENT } });
			if (res.ok) return await res.json();
		} catch (e) {
			/* retry */
		}
		await sleep(700 * (attempt + 1));
	}
	return null;
}

function pad(n) { return n < 10 ? "0" + n : String(n); }

function slug(name) {
	return name
		.toLowerCase()
		.replace(/[\u2018\u2019']/g, "")
		.replace(/[^a-z0-9]+/g, "-")
		.replace(/^-+|-+$/g, "")
		.slice(0, 48);
}

/* Rule one: the first link of each bullet, and nothing else. */
function commemorationsIn(text) {
	const out = [];
	const lines = String(text).split("\n");
	for (let i = 0; i < lines.length; i++) {
		const line = lines[i].trim();
		if (/^==+\s*(sources|see also|external links|notes|references)/i.test(line)) break;
		if (line.charAt(0) !== "*") continue;
		const hit = /\[\[([^\]|#]+)/.exec(line);
		if (!hit) continue;
		const title = hit[1].trim();
		if (title.length < 4 || title.length > 90) continue;
		if (title.indexOf(":") !== -1) continue;
		if (/^\d/.test(title)) continue;
		if (BLOCK.has(title.toLowerCase())) continue;
		out.push(title);
	}
	return out;
}

async function wikitextOf(title) {
	const j = await api("action=parse&prop=wikitext&page=" + encodeURIComponent(title));
	if (!j || !j.parse || !j.parse.wikitext) return "";
	return String(j.parse.wikitext);
}

/* Rule two: make each candidate prove itself, forty at a time. */
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
		if (i % 400 === 0) {
			console.log("  .  verified " + i + " of " + titles.length +
				"  (" + good.size + " kept)");
		}
		await sleep(150);
	}
	return good;
}

function split(title) {
	const comma = title.indexOf(",");
	if (comma > 3) {
		return {
			name: title.slice(0, comma).trim(),
			epithet: title.slice(comma + 1).trim()
		};
	}
	return { name: title.trim(), epithet: "" };
}

async function main() {
	if (typeof fetch !== "function") {
		console.error("  x  this needs Node 18 or newer");
		process.exit(1);
	}
	if (!fs.existsSync(OUT)) {
		console.error("  x  no saints.json at " + OUT);
		process.exit(1);
	}

	const existing = JSON.parse(fs.readFileSync(OUT, "utf8"));
	const byId = new Map();
	const known = new Set();
	existing.forEach(function (s) {
		byId.set(s.id, s);
		known.add(String(s.n).toLowerCase());
		if (s.o) known.add(String(s.o).toLowerCase());
	});

	console.log("  .  starting from " + existing.length + " entries");

	/* ---- 1. read the day pages ----------------------------------------- */

	const dayOf = new Map();   /* title -> "MM-DD" of the first day it is listed */

	for (let m = 0; m < 12; m++) {
		for (let d = 1; d <= DAYS_IN[m]; d++) {
			const dayTitle = MONTHS[m] + " " + d;
			const key = pad(m + 1) + "-" + pad(d);
			const text = await wikitextOf(dayTitle);
			commemorationsIn(text).forEach(function (title) {
				if (!dayOf.has(title)) dayOf.set(title, key);
			});
			await sleep(110);
		}
		console.log("  .  " + MONTHS[m] + "  (" + dayOf.size + " candidates so far)");
	}

	/* ---- 2. make them prove it ------------------------------------------ */

	const candidates = Array.from(dayOf.keys())
		.filter(function (t) { return !known.has(t.toLowerCase()); });

	console.log("");
	console.log("  .  " + candidates.length + " candidates to verify");
	const verified = await verify(candidates);
	console.log("  .  " + verified.size + " of them are people");

	/* ---- 3. build -------------------------------------------------------- */

	const added = [];

	verified.forEach(function (title) {
		const lower = title.toLowerCase();
		if (known.has(lower)) return;
		known.add(lower);

		const key = dayOf.get(title) || null;
		const parts = split(title);
		let id = slug(title);
		if (!id) return;
		if (byId.has(id) && key) id = id + "-" + key.replace("-", "");
		if (byId.has(id)) return;

		const entry = {
			id: id,
			n: parts.name,
			e: parts.epithet,
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

	TOPICS.forEach(function (pair) {
		const kind = pair[0];
		const title = pair[1];
		const id = "t-" + slug(title);
		if (byId.has(id)) return;
		const entry = {
			id: id,
			n: title,
			e: "",
			f: null,
			fl: null,
			era: kind === "fast" ? "The fasts" : (kind === "feast" ? "The feasts" : "The faith"),
			j: "",
			w: "",
			o: title,
			b: [],
			c: null,
			note: null,
			pending: false,
			k: kind
		};
		byId.set(id, entry);
		added.push(entry);
	});

	const all = Array.from(byId.values()).map(function (s) {
		if (!s.k) s.k = "saint";
		return s;
	});
	all.sort(function (a, b) { return String(a.n).localeCompare(String(b.n), "en"); });

	fs.writeFileSync(OUT, JSON.stringify(all));

	const saints = all.filter(function (s) { return s.k === "saint"; }).length;
	const subjects = all.length - saints;

	console.log("");
	console.log("  added     " + added.length);
	console.log("  saints    " + saints);
	console.log("  subjects  " + subjects);
	console.log("  total     " + all.length);
	console.log("  ->        " + OUT);
}

main();