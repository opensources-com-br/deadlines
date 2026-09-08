import { readdir, readFile } from "node:fs/promises";
import { relative, resolve } from "node:path";

const repositoryRoot = resolve(import.meta.dirname, "../..");
const ignoredDirectories = new Set([".git", ".gradle", ".next", "build", "node_modules"]);
const compatibilityFiles = new Set([
  "backend/src/main/kotlin/opensources/shared/database/DatabaseQuery.kt",
  "web/lib/cookies.ts",
]);
const textExtensions = /\.(json|kt|kts|md|mjs|sql|ts|tsx|yaml|yml)$/;
const violations = [];

async function inspect(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    if (entry.isDirectory() && ignoredDirectories.has(entry.name)) continue;
    const path = resolve(directory, entry.name);
    if (entry.isDirectory()) {
      await inspect(path);
      continue;
    }
    if (!textExtensions.test(entry.name)) continue;

    const repositoryPath = relative(repositoryRoot, path);
    if (repositoryPath.startsWith("database/migrations/") || compatibilityFiles.has(repositoryPath)) continue;
    const content = await readFile(path, "utf8");
    if (/deadlines/i.test(content)) violations.push(repositoryPath);
  }
}

await inspect(repositoryRoot);
if (violations.length) {
  console.error(`Legacy branding found in: ${violations.join(", ")}`);
  process.exitCode = 1;
} else {
  console.log("No legacy product branding found outside compatibility history.");
}
