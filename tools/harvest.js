#!/usr/bin/env node
/*
 * Synaxis - harvest OrthodoxWiki
 *
 *   node tools/harvest.js
 *
 * Adds to app/src/main/assets/saints.json:
 *
 *   1. every commemoration listed on OrthodoxWiki's 366 day pages, with the
 *      feast date taken from the day page it was found on;
 *   2. the subject articles at the bottom of this file - feasts, fasts,
 *      councils, practices - so that everything the app shows can be read.
 *
 * Entries already in saints.json are never touched, never reordered and
 * never overwritten. This only ever adds.
 */

const fs = require("fs");
const path = require("path");

const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "saints.json");
const API = "https://orthodoxwiki.org/api.php";
const AGENT = "Synaxis/8.0 (Android reader; https://github.com/Goyimatica/SynaxisMobile)";

const MONTHS = [
	"January", "February", "March", "April", "May", "June",
	"July", "August", "September", "October", "November", "December"
];
const DAYS_IN = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

/* Subjects. Every one of these is a real OrthodoxWiki article title. */
const TOPICS = [
	["feast", "Pascha"],
	["feast", "Nativity of the Theotokos"],
	["feast", "Exaltation of the Holy Cross"],
	["feast", "Entrance of the Theotokos"],
	["feast", "Nativity of Christ"],
	["feast", "Theophany"],
	["feast", "Meeting of the Lord"],
	["feast", "Annunciation"],
	["feast", "Palm Sunday"],
	["feast", "Ascension"],
	["feast", "Pentecost"],
	["feast", "Transfiguration"],
	["feast", "Dormition"],
	["feast", "Circumcision of Christ"],
	["feast", "Protection of the Theotokos"],
	["feast", "Procession of the Precious Cross"],
	["feast", "Sunday of Orthodoxy"],
	["feast", "Holy Week"],
	["feast", "Great and Holy Friday"],
	["feast", "Bright Week"],
	["feast", "Sunday of All Saints"],

	["fast", "Fasting"],
	["fast", "Great Lent"],
	["fast", "Nativity Fast"],
	["fast", "Apostles Fast"],
	["fast", "Dormition Fast"],
	["fast", "Xerophagy"],
	["fast", "Fast-free week"],
	["fast", "Cheesefare Week"],
	["fast", "Meatfare Sunday"],
	["fast", "Clean Monday"],

	["topic", "Church calendar"],
	["topic", "Julian calendar"],
	["topic", "Revised Julian calendar"],
	["topic", "Paschalion"],
	["topic", "Menaion"],
	["topic", "Triodion"],
	["topic", "Pentecostarion"],
	["topic", "Octoechos"],
	["topic", "Typikon"],
	["topic", "Synaxarion"],

	["topic", "Divine Liturgy"],
	["topic", "Divine Liturgy of St. John Chrysostom"],
	["topic", "Divine Liturgy of St. Basil the Great"],
	["topic", "Presanctified Liturgy"],
	["topic", "Vespers"],
	["topic", "Matins"],
	["topic", "Compline"],
	["topic", "Hours"],
	["topic", "All-Night Vigil"],
	["topic", "Prokeimenon"],
	["topic", "Troparion"],
	["topic", "Kontakion"],
	["topic", "Akathist"],
	["topic", "Canon"],

	["topic", "Holy Mysteries"],
	["topic", "Baptism"],
	["topic", "Chrismation"],
	["topic", "Eucharist"],
	["topic", "Confession"],
	["topic", "Holy Unction"],
	["topic", "Marriage"],
	["topic", "Ordination"],

	["topic", "Ecumenical Councils"],
	["topic", "First Ecumenical Council"],
	["topic", "Second Ecumenical Council"],
	["topic", "Third Ecumenical Council"],
	["topic", "Fourth Ecumenical Council"],
	["topic", "Fifth Ecumenical Council"],
	["topic", "Sixth Ecumenical Council"],
	["topic", "Seventh Ecumenical Council"],
	["topic", "Nicene-Constantinopolitan Creed"],
	["topic", "Great Schism"],

	["topic", "Theosis"],
	["topic", "Hesychasm"],
	["topic", "Jesus Prayer"],
	["topic", "Prayer"],
	["topic", "Prayer Rule"],
	["topic", "Repentance"],
	["topic", "Almsgiving"],
	["topic", "Passions"],
	["topic", "Nous"],
	["topic", "Grace"],
	["topic", "Uncreated Light"],
	["topic", "Essence and energies"],

	["topic", "Icon"],
	["topic", "Iconostasis"],
	["topic", "Iconoclasm"],
	["topic", "Church architecture"],
	["topic", "Incense"],
	["topic", "Prosphora"],
	["topic", "Antidoron"],
	["topic", "Holy water"],
	["topic", "Relics"],
	["topic", "Myrrh-streaming icon"],

	["topic", "Monasticism"],
	["topic", "Mount Athos"],
	["topic", "Skete"],
	["topic", "Lavra"],
	["topic", "Elder"],
	["topic", "Schema"],
	["topic", "Philokalia"],
	["topic", "Desert Fathers"],
	["topic", "Fool-for-Christ"],

	["topic", "Bishop"],
	["topic", "Priest"],
	["topic", "Deacon"],
	["topic", "Patriarch"],
	["topic", "Autocephaly"],
	["topic", "Church of Constantinople"],
	["topic", "Church of Russia"],
	["topic", "Church of Greece"],
	["topic", "Church of Romania"],
	["topic", "Church of Georgia"],
	["topic", "Church of Serbia"],
	["topic", "Orthodox Church in America"],
	["topic", "ROCOR"],
	["topic", "Church of Antioch"],
	["topic", "Church of Alexandria"],
	["topic", "Church of Jerusalem"],

	["topic", "Theotokos"],
	["topic", "Holy Trinity"],
	["topic", "Incarnation"],
	["topic", "Resurrection"],
	["topic", "Second Coming"],
	["topic", "Toll houses"],
	["topic", "Memorial service"],
	["topic", "Saint"],
	["topic", "Glorification"],
	["topic", "Martyr"]
];

/* Titles a day page links to that are plainly not a commemoration. */
const REJECT = [
	"category:", "template:", "help:", "orthodoxwiki:", "image:", "file:",
	"main page", "calendar", "fasting", "typikon", "menaion", "pascha",
	"great lent", "triodion", "pentecostarion", "list of", "index of"
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
			/* fall through to the retry */
		}
		await sleep(700 * (attempt + 1));
	}
	return null;
}

function pad(n) {
	return n < 10 ? "0" + n : String(n);
}

function slug(name) {
	return name
		.toLowerCase()
		.replace(/[\u2018\u2019']/g, "")
		.replace(/[^a-z0-9]+/g, "-")
		.replace(/^-+|-+$/g, "")
		.slice(0, 48);
}

function looksLikePerson(title) {
	const t = title.toLowerCase();
	if (t.length < 4 || t.length > 90) return false;
	if (REJECT.some(function (r) { return t.indexOf(r) === 0 || t === r; })) return false;
	/* a bare month-and-day link, i.e. another day page */
	if (MONTHS.some(function (m) { return t.indexOf(m.toLowerCase() + " ") === 0; })) return false;
	if (/^\d/.test(t)) return false;
	return true;
}

/* Split "Seraphim of Sarov" into a name and an epithet the app can show. */
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

async function linksOn(title) {
	const out = [];
	let cont = "";
	for (let page = 0; page < 4; page++) {
		const j = await api(
			"action=query&prop=links&plnamespace=0&pllimit=500&titles=" +
			encodeURIComponent(title) + cont
		);
		if (!j) break;
		const pages = j.query && j.query.pages;
		if (pages && pages[0] && Array.isArray(pages[0].links)) {
			pages[0].links.forEach(function (l) { out.push(l.title); });
		}
		if (j.continue && j.continue.plcontinue) {
			cont = "&plcontinue=" + encodeURIComponent(j.continue.plcontinue);
		} else {
			break;
		}
	}
	return out;
}

async function main() {
	if (typeof fetch !== "function") {
		console.error("  x  this needs Node 18 or newer (global fetch)");
		process.exit(1);
	}
	if (!fs.existsSync(OUT)) {
		console.error("  x  no saints.json at " + OUT + " - run build-assets.js first");
		process.exit(1);
	}

	const existing = JSON.parse(fs.readFileSync(OUT, "utf8"));
	const byId = new Map();
	const byName = new Set();
	existing.forEach(function (s) {
		byId.set(s.id, s);
		byName.add(String(s.n).toLowerCase());
		if (s.o) byName.add(String(s.o).toLowerCase());
	});

	console.log("  .  starting from " + existing.length + " entries");

	const added = [];

	/* ---- 1. the day pages ---------------------------------------------- */

	for (let m = 0; m < 12; m++) {
		for (let d = 1; d <= DAYS_IN[m]; d++) {
			const dayTitle = MONTHS[m] + " " + d;
			const key = pad(m + 1) + "-" + pad(d);
			const links = await linksOn(dayTitle);

			links.forEach(function (title) {
				if (!looksLikePerson(title)) return;
				const lower = title.toLowerCase();
				if (byName.has(lower)) return;
				byName.add(lower);

				const parts = split(title);
				let id = slug(title);
				if (!id || byId.has(id)) id = id + "-" + key.replace("-", "");
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

			if (d === 1) console.log("  .  " + dayTitle + "  (" + added.length + " new so far)");
			await sleep(120);
		}
	}

	/* ---- 2. the subjects ------------------------------------------------ */

	for (let i = 0; i < TOPICS.length; i++) {
		const kind = TOPICS[i][0];
		const title = TOPICS[i][1];
		const lower = title.toLowerCase();
		let id = "t-" + slug(title);
		if (byId.has(id)) continue;

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
		byName.add(lower);
		added.push(entry);
	}

	/* ---- 3. write -------------------------------------------------------- */

	/* Anything already in the file keeps its exact shape; new entries are
	   appended, then the whole thing is sorted by name so the Lives index
	   stays alphabetical. */
	const all = Array.from(byId.values()).map(function (s) {
		if (!s.k) s.k = "saint";
		return s;
	});
	all.sort(function (a, b) { return String(a.n).localeCompare(String(b.n), "en"); });

	fs.writeFileSync(OUT, JSON.stringify(all));

	const saints = all.filter(function (s) { return s.k === "saint"; }).length;
	const feasts = all.filter(function (s) { return s.k === "feast"; }).length;
	const fasts = all.filter(function (s) { return s.k === "fast"; }).length;
	const topics = all.filter(function (s) { return s.k === "topic"; }).length;

	console.log("");
	console.log("  added   " + added.length);
	console.log("  saints  " + saints);
	console.log("  feasts  " + feasts + "   fasts " + fasts + "   subjects " + topics);
	console.log("  total   " + all.length);
	console.log("  ->      " + OUT);
}

main();