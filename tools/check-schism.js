#!/usr/bin/env node
/*
 * Synaxis - scan saints.json for schismatic / non-canonical / foreign bodies.
 *   node tools/check-schism.js
 *
 * A figure whose article or tags belong to a schismatic or non-canonical
 * body (Old Believers, Old Calendarists, catacomb "True Orthodox",
 * Renovationists, the Ukrainian Autocephalous Church, the Macedonian or
 * Montenegrin churches before their restoration) is not an Eastern Orthodox
 * saint.  This scans the *stored fields* (tags, titles, jurisdictions),
 * while tools/audit.js scans the articles' categories on OrthodoxWiki - the
 * two run together in CI.
 *
 * ROCOR is deliberately absent: it has been in communion since 2007.
 * Exits non-zero if anything hard is found, so CI fails loudly.
 */

const s = require("../app/src/main/assets/saints.json");

/* Words that only mean the schismatic body - a hard hit. */
const HARD = /old believer|old-believer|starover|old calendarist|old-calendarist|matthewite|florinite|gennadios|true orthodox|catacomb|renovationist|living church|autocephalous church of ukraine|ukrainian autocephalous|macedonian orthodox church|montenegrin orthodox|uncanonical|self-consecrated|excommunicat|anathema|denisenko/i;

/* Broader words, for the report only. */
const WIDE = /old believer|old-believer|starover|old calendarist|old-calendarist|matthewite|florinite|gennadios|true orthodox|catacomb|renovationist|living church|autocephalous church of ukraine|ukrainian autocephalous|macedonian orthodox|montenegrin orthodox|uncanonical|self-consecrated|excommunicat|anathema|denisenko|militant|schism/i;

const hits = s.filter((x) =>
  WIDE.test(
    (x.n || "") + " " + (x.e || "") + " " + (x.o || "") + " " +
    (x.w || "") + " " + (x.j || "") + " " + (x.era || "") + " " +
    (x.b || []).join(" ")
  )
);
console.log("schism-ish matches:", hits.length);
let hard = 0;
hits.forEach((x) => {
  const text =
    (x.n || "") + " " + (x.e || "") + " " + (x.o || "") + " " +
    (x.w || "") + " " + (x.j || "") + " " + (x.era || "") + " " +
    (x.b || []).join(" ");
  const isHard = HARD.test(text);
  if (isHard) hard++;
  console.log(
    (isHard ? "[HARD] " : "[soft] ") + x.id + " | " + x.n + " | " +
    (x.e || "") + " | f=" + (x.f || "-") + " | " + (x.j || "") + " | " +
    (x.o || "").slice(0, 50) + " | " + (x.b || []).join(",")
  );
});

/* jurisdictions histogram */
const js = {};
s.forEach((x) => (js[x.j || "(none)"] = (js[x.j || "(none)"] || 0) + 1));
console.log("\n--- jurisdictions ---");
Object.entries(js)
  .sort((a, b) => b[1] - a[1])
  .forEach(([k, v]) => console.log(String(v).padStart(4), k));

console.log("");
if (hard > 0) {
  console.log("  " + hard + " hard schismatic hit(s) - fix before release");
  process.exit(1);
} else {
  console.log("  clean: no schismatic entries");
}
