import { expect, test } from "@playwright/test";

test("opens the public authentication entry points", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle(/opensources/i);

  await page.getByRole("link", { name: /get started|começar/i }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.goto("/register");
  await expect(page).toHaveURL(/\/register$/);
});
