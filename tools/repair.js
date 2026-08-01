#!/usr/bin/env node
/*
 * Synaxis - undo the V8 harvest.  node tools/repair.js
 *
 * Keeps: everything your converter wrote, and every subject entry.
 * Drops: everything the V8 day-page harvester added, which is identifiable
 *        with certainty because it has no era, no Wikipedia title, no tags
 *        and no century - all four are empty on those and never all empty
 *        on a curated one.
 */

const fs = require("fs");
const path = require("path");

const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "saints.json");
const BAK = OUT + ".bak";

function filled(v) {
	if (v === null || v === undefined) return false;
	if (Array.isArray(v)) return v.length > 0;
	return String(v).trim() !== "";
}

const all = JSON.parse(fs.readFileSync(OUT, "utf8"));
fs.writeFileSync(BAK, JSON.stringify(all));

const kept = [];
const dropped = [];

all.forEach(function (s) {
	const kind = s.k || "saint";
	const curated =
		kind !== "saint" ||
		String(s.id || "").indexOf("t-") === 0 ||
		filled(s.era) || filled(s.w) || filled(s.b) || filled(s.c) ||
		filled(s.j) || filled(s.note);
	if (curated) kept.push(s);
	else dropped.push(s.n);
});

fs.writeFileSync(OUT, JSON.stringify(kept));

console.log("");
console.log("  kept     " + kept.length);
console.log("  dropped  " + dropped.length);
if (dropped.length > 0) {
	console.log("  e.g.     " + dropped.slice(0, 12).join(", "));
}
console.log("  backup   " + BAK);