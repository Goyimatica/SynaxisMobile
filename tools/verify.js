#!/usr/bin/env node
/*
 * Synaxis - verify saints.json.  node tools/verify.js
 *
 * Checks, in order:
 *   1. every one of the 366 church dates has at least one saint
 *   2. no entry is a glossary word dressed as a person
 *   3. no entry has a feast but no OrthodoxWiki title
 *   4. ids are unique and well-formed
 *
 * Prints a report and exits non-zero if anything is wrong, so it can be
 * called twice, from two shells, and be trusted both times.
 */

const fs = require("fs");
const path = require("path");

const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "saints.json");

/* A name that is a title or a role, not a person. A saint named "Cloud"
   exists in folklore, but OrthodoxWiki's article is about weather. */
const GLOSS = new Set([
	"abbess", "abbot", "abbreviations", "acheiropoieta", "afterfeast",
	"akathist", "akolouthia", "anchorite", "angels", "apodosis", "apostate",
	"apostles", "archangel", "archbishop", "archimandrite", "asceticism",
	"bishop", "cathedral", "church", "cloud", "deacon", "elder", "evangelist",
	"fasting", "feasts", "geronta", "glossary", "great lent", "hermit",
	"hegumen", "igumen", "icon", "icons", "liturgy", "martyr", "matins",
	"metropolitan", "miracle", "missionary", "monastery", "monk", "novice",
	"nun", "passion-bearer", "patriarch", "pope", "priest", "prophet",
	"relics", "saint", "schema", "stylite", "synaxis", "theotokos",
	"wonderworker", "wonder-worker", "vespers", "the ladder of divine ascent",
	"other events", "cross procession", "adoration of the magi", "magi",
	"shepherds", "holy trinity", "jesus christ", "dormition", "pascha",
	"the faith", "the feasts", "the fasts", "translation of relics",
	"baptism of rus", "baptism of rus'", "holy land", "monasticism",
	"ladder of divine ascent", "abbey"
]);

const MONTHS_IN = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

function pad(n) { return n < 10 ? "0" + n : String(n); }

const data = JSON.parse(fs.readFileSync(OUT, "utf8"));
const people = data.filter(function (s) { return s.k === "saint" && s.f; });

const byFeast = {};
people.forEach(function (s) {
	(byFeast[s.f] = byFeast[s.f] || []).push(s);
});

const all = [];
for (let m = 1; m <= 12; m++) {
	for (let d = 1; d <= MONTHS_IN[m - 1]; d++) {
		all.push(pad(m) + "-" + pad(d));
	}
}

const missing = all.filter(function (k) { return !byFeast[k]; });
const junk = people.filter(function (s) {
	return GLOSS.has(String(s.n).trim().toLowerCase());
});
const noTitle = people.filter(function (s) { return !s.o && !s.w; });

/* V11: an office is not a person. A saint's name must carry a personal name;
   "Abbot of Iona" is a post, not a life, however reverent it sounds. */
const OFFICE = /^(abbot|abbess|archbishop|bishop|metropolitan|patriarch|priest|deacon|monk|nun|hermit|stylite|pope|elder|igumen|hegumen|archimandrite)\s+of\b/i;
const officeJunk = people.filter(function (s) {
	return OFFICE.test(String(s.n).trim());
});
const ids = new Set();
let dupIds = 0;
data.forEach(function (s) {
	if (ids.has(s.id)) dupIds++;
	ids.add(s.id);
});

let bad = 0;
function line(ok, msg) {
	console.log((ok ? "  ok  " : "  x   ") + msg);
	if (!ok) bad++;
}

console.log("");
console.log("saints.json  " + data.length + " entries, " +
	people.length + " saints, " + (data.length - people.length) + " subjects");
console.log("");

line(missing.length === 0, "every church date has a saint  (" +
	Object.keys(byFeast).length + " of " + all.length + " covered" +
	(missing.length ? "; missing: " + missing.join(" ") : "") + ")");

line(junk.length === 0, "no glossary names as saints" +
	(junk.length ? ";  " + junk.map(function (s) {
		return s.f + " " + s.n;
	}).join(", ") : ""));

line(officeJunk.length === 0, "no office titles as saints" +
	(officeJunk.length ? ";  " + officeJunk.map(function (s) {
		return s.f + " " + s.n;
	}).join(", ") : ""));

line(noTitle.length === 0, "every saint with a feast has an OrthodoxWiki or Wikipedia title" +
	(noTitle.length ? ";  " + noTitle.map(function (s) {
		return s.f + " " + s.n;
	}).join(", ") : ""));

line(dupIds === 0, "ids are unique" + (dupIds ? ";  " + dupIds + " duplicates" : ""));

console.log("");
if (bad > 0) {
	console.log("  " + bad + " problem(s) found");
	process.exit(1);
} else {
	console.log("  clean");
}
