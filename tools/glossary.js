/*
 * Synaxis - the one list of words that are never saints.
 *
 * tools/verify.js, tools/harvest.js and tools/fill-gaps.js all require this
 * module, so a word added here is refused everywhere at once. The CI harvest
 * run of 2026-08-01 proved what happens when these lists live in two places:
 * the loose V8 backlog quietly re-added "Apostles" and "Abbot of Iona" and
 * verify.js, which had the fuller list, failed. One source of truth, by
 * construction.
 */

/*
 * A name that is a title or a role, not a person. A saint named "Cloud"
 * exists in folklore, but OrthodoxWiki's article is about weather.
 */
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
	"shepherds", "commemoration of the shepherds", "holy trinity",
	"jesus christ", "dormition", "pascha", "the faith", "the feasts",
	"the fasts", "translation of relics", "baptism of rus", "baptism of rus'",
	"holy land", "monasticism", "ladder of divine ascent", "abbey"
]);

/*
 * An office is not a person. "Abbot of Iona" is a post, not a life, however
 * reverent it sounds. A saint's name must carry a personal name.
 */
const OFFICE = /^(abbot|abbess|archbishop|bishop|metropolitan|patriarch|priest|deacon|monk|nun|hermit|stylite|pope|elder|igumen|hegumen|archimandrite)\s+of\b/i;

/*
 * Titles that resolve wrong and must never be accepted, however much they
 * look like a life. "Olga of Alaska" is the January 28 "Virgin-martyr Olga
 * (1938)" mis-resolved to Matushka Olga Michael, who is already in the index
 * as "Olga Michael" on 11-10. The CI harvest of 2026-08-01 re-added it;
 * blocking it here keeps the CI output identical to the curated index.
 */
const TITLE_BLOCK = new Set([
	"olga of alaska",
	/* 2026-08-02 culls: these articles exist on OrthodoxWiki and their
	   categories look like a person, but they are not Eastern Orthodox
	   saints - a Coptic Pope, a condemned writer, deposed or uncanonized
	   patriarchs, a canonist, a Coptic double of an existing saint, and a
	   movable feast.  They must never be harvested again. */
	"cyril v (john) of alexandria",
	"evagrius ponticus",
	"john xi bekkos of constantinople",
	"theodosius vi (abourjaily) of antioch",
	"gregory ii of constantinople",
	"matthew blastares",
	"philopater mercurius",
	"joseph the betrothed",
	/* 2026-08-03: culled duplicates that the harvest kept re-adding from
	   day templates and name mis-resolutions.  Each is the same person as
	   an entry already in the index on its real feast date - verified
	   against the OrthodoxWiki articles - and must never return. */
	"amphilochios makris",                    // dup of "Amphilochios (Makris) of Patmos" 04-03
	"justin the philosopher",                 // dup of "Justin Martyr" 06-01
	"chinese new martyrs",                    // dup of "Martyrs of China" 06-11
	"cosmas of aetolia",                      // feast 24 Aug (OW article); the 08-04 slot is not his
	"john (maximovitch) the wonderworker",    // the 12-04 "John the Wonderworker" (of Polybotum) mis-resolves to him
	"sebastian (dabovich)",                   // feast 30 Nov; the 10-24 "Martyr Sebastian" is a different man
	"alexis mechev",                          // dup of "Alexey Mechev of Moscow" 06-09 (OCA)
	"sophia"                                  // pre-comma name of "Sophia, the ascetic of Kleisoura" dup
]);

module.exports = { GLOSS, OFFICE, TITLE_BLOCK };
