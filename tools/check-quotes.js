#!/usr/bin/env node
/*
 * Synaxis - verify quotes.json.  node tools/check-quotes.js
 *
 * The daily saying must be complete: one quote for every one of the 366
 * church-calendar slots (February 29 has its own), no duplicates, and every
 * entry fully written.  Prints a report and exits non-zero on any problem.
 */

const fs = require("fs");
const path = require("path");

const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "quotes.json");

const data = JSON.parse(fs.readFileSync(OUT, "utf8"));

let bad = 0;
function line(ok, msg) {
	console.log((ok ? "  ok  " : "  x   ") + msg);
	if (!ok) bad++;
}

console.log("");
console.log("quotes.json  " + data.length + " entries");

line(data.length === 366, "one quote for every church-calendar slot  (" + data.length + " of 366)");

const slots = new Set();
let dupSlots = 0;
const texts = new Set();
let dupTexts = 0;
const ids = new Set();
let dupIds = 0;
let empty = 0;
let missingBy = 0;
let missingId = 0;
let short = 0;

data.forEach(function (q) {
	const i = q.i;
	const t = String(q.q || "").trim();
	const by = String(q.by || "").trim();
	const id = String(q.id || "").trim();

	if (i == null || i < 1 || i > 366) { empty++; return; }
	if (slots.has(i)) dupSlots++;
	slots.add(i);

	if (!t) empty++;
	else if (t.length < 20) short++;

	if (!by) missingBy++;
	if (!id) missingId++;
	if (t && texts.has(t)) dupTexts++;
	if (t) texts.add(t);
	if (id && ids.has(id)) dupIds++;
	if (id) ids.add(id);
});

line(empty === 0, "every entry has its slot and a quote" + (empty ? "  (" + empty + " bad)" : ""));
line(missingBy === 0, "every quote has an author" + (missingBy ? "  (" + missingBy + " missing)" : ""));
line(missingId === 0, "every quote has an id" + (missingId ? "  (" + missingId + " missing)" : ""));
line(dupSlots === 0, "slots 1..366 each used once" + (dupSlots ? "  (" + dupSlots + " duplicates)" : ""));
line(dupTexts === 0, "no repeated quote text" + (dupTexts ? "  (" + dupTexts + " repeats)" : ""));
line(short === 0, "no truncated sayings" + (short ? "  (" + short + " under 20 chars)" : ""));

console.log("");
if (bad > 0) {
	console.log("  " + bad + " problem(s) found");
	process.exit(1);
} else {
	console.log("  clean");
}
