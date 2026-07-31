#!/usr/bin/env node
/*
 * Synaxis - web data -> Android assets
 *
 *   node tools/build-assets.js /path/to/synaxis-web
 *
 * Reads data.saints.js, data.saints2.js and data.quotes.js from the web app
 * and writes app/src/main/assets/saints.json and quotes.json.
 *
 * The data files declare their arrays with const/let, which never become
 * properties of a vm context, so each source is rewritten to assign onto
 * globalThis before it is evaluated. Nothing else about the files changes.
 */

const fs = require("fs");
const path = require("path");
const vm = require("vm");

const WEB = process.argv[2] || ".";
const OUT = path.join(__dirname, "..", "app", "src", "main", "assets");
const FILES = ["data.saints.js", "data.saints2.js", "data.quotes.js"];

function die(msg) {
	console.error("  x  " + msg);
	process.exit(1);
}

if (!fs.existsSync(WEB)) die("no such folder: " + WEB);
if (!fs.existsSync(OUT)) fs.mkdirSync(OUT, { recursive: true });

const sandbox = { console: console };
sandbox.globalThis = sandbox;
sandbox.window = sandbox;
vm.createContext(sandbox);

for (const name of FILES) {
	const file = path.join(WEB, name);
	if (!fs.existsSync(file)) die("missing " + file);
	const raw = fs.readFileSync(file, "utf8");
	const src = raw.replace(
		/^[ \t]*(?:const|let|var)[ \t]+([A-Za-z_$][\w$]*)[ \t]*=/gm,
		"globalThis.$1 ="
	);
	try {
		vm.runInContext(src, sandbox, { filename: name });
	} catch (e) {
		die("could not evaluate " + name + ": " + e.message);
	}
	console.log("  .  read " + name);
}

const saints = [];
const quotes = [];
const seenSaint = new Set();
const seenQuote = new Set();

for (const key of Object.keys(sandbox)) {
	const value = sandbox[key];
	if (!Array.isArray(value)) continue;
	for (const item of value) {
		if (!item || typeof item !== "object") continue;
		if (typeof item.n === "string" && typeof item.id === "string") {
			if (seenSaint.has(item.id)) continue;
			seenSaint.add(item.id);
			saints.push(item);
		} else if (typeof item.q === "string" && typeof item.by === "string") {
			const k = item.q.slice(0, 60);
			if (seenQuote.has(k)) continue;
			seenQuote.add(k);
			quotes.push(item);
		}
	}
}

if (!saints.length) die("found no saints - are the data files the right ones?");
if (!quotes.length) die("found no quotes");

// Kotlin's parser wants every optional field present and typed, so normalise.
const clean = saints
	.map(function (s) {
		return {
			id: String(s.id),
			n: String(s.n),
			e: String(s.e || ""),
			f: s.f ? String(s.f) : null,
			fl: s.fl ? String(s.fl) : null,
			era: String(s.era || ""),
			j: String(s.j || ""),
			w: String(s.w || ""),
			o: String(s.o || ""),
			b: Array.isArray(s.b) ? s.b.map(String) : [],
			c: s.c ? String(s.c) : null,
			note: s.note ? String(s.note) : null,
			pending: s.pending === true
		};
	})
	.sort(function (a, b) {
		return a.n.localeCompare(b.n, "en");
	});

const cleanQuotes = quotes.map(function (q, i) {
	return {
		q: String(q.q),
		by: String(q.by),
		id: q.id ? String(q.id) : null,
		i: i
	};
});

fs.writeFileSync(path.join(OUT, "saints.json"), JSON.stringify(clean));
fs.writeFileSync(path.join(OUT, "quotes.json"), JSON.stringify(cleanQuotes));

const withFeast = clean.filter(function (s) { return !!s.f; }).length;
const pending = clean.filter(function (s) { return s.pending; }).length;

console.log("");
console.log("  saints  " + clean.length + "  (" + withFeast + " with a fixed feast, " + pending + " pending glorification)");
console.log("  quotes  " + cleanQuotes.length);
console.log("  ->      " + OUT);