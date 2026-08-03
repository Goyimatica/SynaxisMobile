#!/usr/bin/env node
/* Temp check: what state is saints.json in right now? */
const s = require("../app/src/main/assets/saints.json");
const find = (re) => s.filter((x) => re.test((x.n || "") + " " + (x.o || "") + " " + (x.w || "")));
console.log("total:", s.length);
console.log("--- key moves applied? ---");
[
  ["xenia", /xenia/i],
  ["silouan", /silouan/i],
  ["clement-of-rome", /clement of rome/i],
  ["seraphim-rose", /seraphim rose/i],
  ["justin-popovic", /justin popovi/i],
  ["royal-martyrs", /royal martyrs of russia/i],
  ["lazar", /lazar of serbia/i],
  ["efraim", /ephraim of katounakia/i],
  ["simeon-myrrh", /simeon the myrrh/i],
  ["hilarion", /hilarion \(troitsky\)/i],
  ["alexander-nevsky", /alexander nevsky/i],
].forEach(([label, re]) => {
  const hits = find(re);
  console.log(label.padEnd(20), hits.map((x) => x.id + " @ " + (x.f || "-")).join(", ") || "(none)");
});
console.log("--- culls applied (should be empty)? ---");
[
  ["cyril-v", /cyril v \(john\)/i],
  ["evagrius", /evagrius ponticus/i],
  ["bekkos", /bekkos/i],
  ["theodosius-vi", /theodosius vi/i],
  ["gregory-ii", /gregory ii of constantinople/i],
  ["blastares", /blastares/i],
  ["philopater", /philopater/i],
  ["amphilochios-makris", /amphilochios makris/i],
  ["justin-philosopher", /justin the philosopher/i],
  ["joseph-betrothed", /joseph the betrothed/i],
  ["cosmas-aetolia-dup", /cosmas of aetolia/i],
  ["catherine-1125", /catherine of alexandria/i],
].forEach(([label, re]) => {
  const hits = find(re);
  console.log(label.padEnd(22), hits.map((x) => x.id + " @ " + (x.f || "-")).join(", ") || "(none)");
});
console.log("--- dup-by-feast culls ---");
[["acepsimus@09-01", /acepsimus/i], ["job@05-06", /^Job$/], ["rublev@07-04", /andrei rublev/i]].forEach(([label, re]) => {
  const hits = s.filter((x) => re.test((x.n || "") + " " + (x.o || "")));
  console.log(label.padEnd(18), hits.map((x) => x.id + " @ " + (x.f || "-") + " [" + (x.o || "") + "]").join(", ") || "(none)");
});
console.log("--- vladimir retitle ---");
find(/vladimir/i).forEach((x) => console.log(" ", x.id, "|", x.n, "|", x.o));
console.log("--- empty dates? ---");
const byFeast = {};
s.filter((x) => x.f).forEach((x) => (byFeast[x.f] = byFeast[x.f] || []).push(x.n));
const MISSING = [];
for (let m = 1; m <= 12; m++) {
  const days = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][m - 1];
  for (let d = 1; d <= days; d++) {
    const k = String(m).padStart(2, "0") + "-" + String(d).padStart(2, "0");
    if (!byFeast[k]) MISSING.push(k);
  }
}
console.log("missing:", MISSING.join(" ") || "(none)");
