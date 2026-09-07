import { readFile } from "node:fs/promises";

const catalogs = await Promise.all(
  ["pt-BR", "en"].map(async (locale) => [locale, JSON.parse(await readFile(new URL(`../messages/${locale}.json`, import.meta.url), "utf8"))]),
);

function keys(value, prefix = "") {
  return Object.entries(value).flatMap(([key, child]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    return child && typeof child === "object" ? keys(child, path) : [path];
  });
}

function dottedKeys(value, prefix = "") {
  return Object.entries(value).flatMap(([key, child]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    const invalid = key.includes(".") ? [path] : [];
    return child && typeof child === "object" ? [...invalid, ...dottedKeys(child, path)] : invalid;
  });
}

const [referenceLocale, reference] = catalogs[0];
const referenceKeys = new Set(keys(reference));
let failed = false;
for (const [locale, catalog] of catalogs) {
  const invalid = dottedKeys(catalog);
  if (invalid.length) {
    failed = true;
    console.error(`${locale} contains keys with dots: ${invalid.join(", ")}`);
  }
}
for (const [locale, catalog] of catalogs.slice(1)) {
  const localeKeys = new Set(keys(catalog));
  const missing = [...referenceKeys].filter((key) => !localeKeys.has(key));
  const extra = [...localeKeys].filter((key) => !referenceKeys.has(key));
  if (missing.length || extra.length) {
    failed = true;
    if (missing.length) console.error(`${locale} is missing: ${missing.join(", ")}`);
    if (extra.length) console.error(`${locale} has extra keys: ${extra.join(", ")}`);
  }
}
if (failed) process.exitCode = 1;
else console.log(`Translation catalogs match ${referenceLocale} (${referenceKeys.size} messages).`);
