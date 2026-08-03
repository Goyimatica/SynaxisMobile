#!/usr/bin/env node
/* Temp fix: the RETITLE in fix-calendar.js keyed on o="Vladimir of Kiev" and
   hit BOTH vladimir entries. The Grand Prince (07-15, w="Vladimir the Great")
   is correctly titled "Vladimir of Kiev" on OrthodoxWiki; only the
   Metropolitan hieromartyr (01-25) is "Vladimir (Bogoyavlensky) of Kiev".
   Restore the Grand Prince; leave the Metropolitan as Bogoyavlensky. */
const fs = require("fs");
const path = require("path");
const OUT = path.join(__dirname, "..", "app", "src", "main", "assets", "saints.json");
const data = JSON.parse(fs.readFileSync(OUT, "utf8"));
let fixed = 0;
data.forEach((s) => {
  if (s.id === "vladimir" && s.f === "07-15") {
    s.n = "St Vladimir";
    s.o = "Vladimir of Kiev";
    fixed++;
  }
});
fs.writeFileSync(OUT, JSON.stringify(data));
console.log("fixed:", fixed);
