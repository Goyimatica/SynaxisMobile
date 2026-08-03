#!/usr/bin/env node
/* Temp probe: confirm correct OW titles + feast dates for the ambiguous ones. */
const OW = "https://orthodoxwiki.org/api.php";
async function get(q) {
  for (let a = 0; a < 3; a++) {
    try {
      const r = await fetch(OW + "?format=json&formatversion=2&" + q, { headers: { "User-Agent": "Synaxis-probe/1.0" } });
      if (r.ok) return await r.json();
    } catch (e) {}
    await new Promise((r) => setTimeout(r, 800 * (a + 1)));
  }
  return null;
}
async function article(title) {
  const j = await get("action=parse&prop=wikitext&page=" + encodeURIComponent(title));
  if (!j || !j.parse || !j.parse.wikitext) return null;
  const t = String(j.parse.wikitext);
  const m = t.match(/(?:feast|feast day|commemorated)[^\n]{0,90}/i);
  return (m ? m[0].trim() : "(no feast line)").replace(/\{\{|\}\}|\[\[|\]\]/g, "");
}
async function search(q) {
  const j = await get("action=query&list=search&srnamespace=0&srlimit=5&srsearch=" + encodeURIComponent(q));
  return ((j && j.query && j.query.search) || []).map((r) => r.title);
}
(async () => {
  for (const t of [
    "Vladimir, Equal-to-the-Apostles",
    "Vladimir of Kiev",
    "Vladimir the Great",
    "Cosmas of Aetolia",
    "Cosmas of Aitolia",
    "Andrew Rublev",
    "Andrei Rublev",
    "Job the Long-suffering",
  ]) {
    const found = await search(t);
    const exact = found.find((x) => x.toLowerCase() === t.toLowerCase());
    const pick = exact || found[0] || "(none)";
    const feast = pick.startsWith("(") ? "" : await article(pick);
    console.log(t.padEnd(30), "->", pick, "|", feast || "");
  }
  console.log("\n--- OW day template August 4 full ---");
  const j = await get("action=parse&prop=wikitext&page=" + encodeURIComponent("Template:August 4"));
  console.log((j && j.parse && j.parse.wikitext) ? String(j.parse.wikitext).slice(0, 900) : "(none)");
  console.log("\n--- OW day template July 4 (Rublev's church date?) ---");
  const k = await get("action=parse&prop=wikitext&page=" + encodeURIComponent("Template:July 4"));
  console.log((k && k.parse && k.parse.wikitext) ? String(k.parse.wikitext).slice(0, 700) : "(none)");
})();
