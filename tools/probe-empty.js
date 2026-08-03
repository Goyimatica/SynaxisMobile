#!/usr/bin/env node
/* Temp probe: vladimir/job/rublev details + what the 7 empty church dates carry. */
const s = require("../app/src/main/assets/saints.json");
console.log("=== vladimir / job / rublev entries ===");
s.filter((x) => /vladimir|^job$|rublev/i.test(x.n + " " + (x.o || "")))
  .forEach((x) => console.log(" ", x.id, "|", x.n, "|", x.e || "-", "| f=" + (x.f || "-"), "| o=" + (x.o || "-"), "| w=" + (x.w || "-")));

console.log("\n=== probe empty dates on Wikipedia EO liturgics + OrthodoxWiki templates ===");
const MONTHS = ["January","February","March","April","May","June","July","August","September","October","November","December"];
const WP = "https://en.wikipedia.org/w/api.php";
const OW = "https://orthodoxwiki.org/api.php";
async function get(base, q) {
  for (let a = 0; a < 3; a++) {
    try {
      const r = await fetch(base + "?format=json&formatversion=2&" + q, { headers: { "User-Agent": "Synaxis-probe/1.0" } });
      if (r.ok) return await r.json();
    } catch (e) {}
    await new Promise((r) => setTimeout(r, 800 * (a + 1)));
  }
  return null;
}
async function lit(day) {
  const j = await get(WP, "action=parse&prop=wikitext&page=" + encodeURIComponent(day + " (Eastern Orthodox liturgics)"));
  return ((j && j.parse && j.parse.wikitext) || "").toLowerCase();
}
async function owDay(day) {
  let j = await get(OW, "action=parse&prop=wikitext&page=" + encodeURIComponent("Template:" + day));
  if (j && j.parse && j.parse.wikitext) return String(j.parse.wikitext).toLowerCase();
  j = await get(OW, "action=parse&prop=wikitext&page=" + encodeURIComponent(day));
  return ((j && j.parse && j.parse.wikitext) || "").toLowerCase();
}
function heads(text) {
  return text.split(/[;\n]+/).map((x) => x.trim()).filter(Boolean)
    .map((x) => x.split(",")[0].replace(/'''|\[\[|\]\]/g, "").replace(/\s+/g, " ").trim())
    .filter((x) => x.length > 2 && x.length < 60).slice(0, 14);
}
(async () => {
  for (const [m, d] of [["01","27"],["04","16"],["07","18"],["08","04"],["09","12"],["11","02"],["12","28"]]) {
    const day = MONTHS[parseInt(m) - 1] + " " + parseInt(d);
    const w = await lit(day);
    const o = await owDay(day);
    console.log("\n--- " + day + " ---");
    console.log("WP heads:", heads(w).join(" | ") || "(none)");
    console.log("OW heads:", heads(o).join(" | ") || "(none)");
  }
})();
