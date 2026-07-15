import type { Page } from "@playwright/test";
import { expect } from "@playwright/test";
import { uniqueUser, type TestUser } from "./test-data";

/** Registers a brand-new user through the real UI/API and leaves the browser on /profile. */
export async function registerNewUser(page: Page, prefix = "e2e"): Promise<TestUser> {
  const user = uniqueUser(prefix);

  await page.goto("/register");
  await page.getByLabel("Full name").fill(user.fullName);
  await page.getByLabel("Email").fill(user.email);
  await page.getByLabel("Password").fill(user.password);
  await page.getByRole("button", { name: "Create account" }).click();

  await expect(page).toHaveURL(/\/profile$/);

  return user;
}
