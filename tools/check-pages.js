#!/usr/bin/env node
/*
 * Synaxis - check-pages.js
 *
 * The startup download used to re-offer the same two "missing" lives on
 * every launch: entries whose OrthodoxWiki title does not exist and whose
 * Wikipedia side cannot be found either fetch nothing, and a blank text
 * reads as "missing" forever.
 *
 * This tool asks both wikis, in batches, whether every entry's titles
 * actually exist, and replays the app's Wikipedia search fallback for the
 * entries OrthodoxWiki does not carry.  Anything that resolves to nothing
 * is the exact list that will nag the user, so fixing those titles is what
 * makes the download dialog stop coming back.
 *
 *   node tools/check-pages.js
 */

const fs = require("fs");
const path = require("path");

const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "saints.json");
const OW = "https://orthodoxwiki.org/api.php";
const WP = "https://en.wikipedia.org/w/api.php";
const AGENT = "Synaxis-check/1.0 (https://github.com/Goyimatica/SynaxisMobile)";

const data = JSON.parse(fs.readFileSync(OUT, "utf8"));

const USER_AGENT = { "User-Agent": AGENT };

async function getJson(url) {
	const res = await fetch(url, { headers: USER_AGENT });
	if (!res.ok) throw new Error("HTTP " + res.status + " for " + url.slice(0, 80));
	return res.json();
}

/* action=query&titles=A|B|C - returns which titles are missing. */
async function existing(base, titles) {
	const out = new Map(); // title -> exists
	for (let i = 0; i < titles.length; i += 20) {
		const chunk = titles.slice(i, i + 20);
		const url = base + "?format=json&formatversion=2&redirects=1&action=query&titles=" +
			encodeURIComponent(chunk.join("|"));
		const o = await getJson(url);
		const pages = (o.query || {}).pages || [];
		for (const p of pages) {
			out.set(p.title, !p.missing);
		}
	}
	return out;
}

async function searchTitle(base, q) {
	const url = base + "?format=json&formatversion=2&action=query&list=search&srnamespace=0&srlimit=1&srsearch=" +
		encodeURIComponent(q);
	const o = await getJson(url);
	const arr = (o.query || {}).search || [];
	if (arr.length === 0) return "";
	const title = arr[0].title || "";
	/* same "is this the right article" rule the app uses */
	const words = q.toLowerCase().split(/[^a-z0-9]+/).filter(function (w) { return w.length > 3; });
	if (words.length === 0) return title;
	return words.some(function (w) { return title.toLowerCase().indexOf(w) >= 0; }) ? title : "";
}

async function main() {
	console.log("check-pages  " + data.length + " entries");

	const byId = {};
	data.forEach(function (s) { byId[s.id] = s; });

	const owTitles = data.map(function (s) { return s.o || s.name; }).filter(function (t, i, a) {
		return a.indexOf(t) === i;
	});
	console.log("  checking " + owTitles.length + " OrthodoxWiki titles...");
	const owExists = await existing(OW, owTitles);

	const wpTitles = data.map(function (s) { return s.w || ""; }).filter(function (t) { return t; })
		.filter(function (t, i, a) { return a.indexOf(t) === i; });
	console.log("  checking " + wpTitles.length + " Wikipedia titles...");
	const wpExists = await existing(WP, wpTitles);

	/* entries whose OW side is a dead end, and whose WP side we must replay */
	const dead = [];
	const needWpSearch = [];
	for (const s of data) {
		const owTitle = s.o || s.name;
		const owOk = owExists.get(owTitle);
		if (owOk) continue;

		if (s.w && wpExists.get(s.w)) continue;

		if (s.w) {
			dead.push({ s: s, why: "o '" + owTitle + "' missing and w '" + s.w + "' missing" });
		} else {
			needWpSearch.push(s);
		}
	}

	/* replay the app's Wikipedia search for entries with no w */
	if (needWpSearch.length > 0) {
		console.log("  searching Wikipedia for " + needWpSearch.length + " untitled entries...");
		for (const s of needWpSearch) {
			const first = s.k === "saint" ? s.n : s.n + " Eastern Orthodox";
			const found = await searchTitle(WP, first) || await searchTitle(WP, s.n);
			if (!found) {
				dead.push({ s: s, why: "o '" + (s.o || s.name) + "' missing and no Wikipedia article found" });
			}
		}
	}

	dead.sort(function (a, b) { return (a.s.f || "-").localeCompare(b.s.f || "-"); });

	console.log("");
	if (dead.length === 0) {
		console.log("  clean: every entry resolves on OrthodoxWiki or Wikipedia");
		return;
	}

	console.log("  " + dead.length + " entr" + (dead.length === 1 ? "y" : "ies") + " would fetch nothing:");
	dead.forEach(function (d) {
		console.log("    " + (d.s.f || "----") + "  " + d.s.id + "  " + d.s.n);
		console.log("        " + d.why);
	});
	process.exit(1);
}

main().catch(function (e) {
	console.error("check-pages failed: " + e.message);
	process.exit(2);
});
