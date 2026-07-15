import { test, expect } from "@playwright/test";
import { registerNewUser } from "../utils/register";

test.describe("Profile", () => {
  test.beforeEach(async ({ page }) => {
    await registerNewUser(page, "profile");
  });

  test("can update and persist contact information", async ({ page }) => {
    await page.getByLabel("Phone").fill("555-0100");
    await page.getByLabel("Location").fill("Remote");
    await page.getByRole("button", { name: "Save profile" }).click();

    await expect(page.getByText("Your information has been saved.")).toBeVisible();

    await page.reload();
    await expect(page.getByLabel("Phone")).toHaveValue("555-0100");
    await expect(page.getByLabel("Location")).toHaveValue("Remote");
  });

  test("can add and remove a skill", async ({ page }) => {
    await page.getByLabel("Skill").fill("TypeScript");
    await page.getByRole("button", { name: "Add", exact: true }).click();

    const skillChip = page.getByText("TypeScript");
    await expect(skillChip).toBeVisible();

    await page.getByRole("button", { name: "Remove TypeScript" }).click();
    await expect(skillChip).not.toBeVisible();
  });
});
