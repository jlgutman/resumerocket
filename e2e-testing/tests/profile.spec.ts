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
    await page.getByLabel("Category (optional)").fill("Languages");
    await page.getByRole("button", { name: "Add", exact: true }).click();

    const skillChip = page.getByText("TypeScript");
    await expect(skillChip).toBeVisible();

    await page.getByRole("button", { name: "Remove TypeScript" }).click();
    await expect(skillChip).not.toBeVisible();
  });

  test("can save and persist links", async ({ page }) => {
    await page
      .getByLabel("Links (portfolio, LinkedIn, GitHub — comma-separated)")
      .fill("https://portfolio.dev, https://github.com/test");
    await page.getByRole("button", { name: "Save profile" }).click();

    await expect(page.getByText("Your information has been saved.")).toBeVisible();

    await page.reload();
    await expect(
      page.getByLabel("Links (portfolio, LinkedIn, GitHub — comma-separated)")
    ).toHaveValue("https://portfolio.dev, https://github.com/test");
  });

  test("can add, edit, and delete an education entry", async ({ page }) => {
    await page.locator("#edu-institution").fill("State University");
    await page.locator("#edu-credential").fill("B.S.");
    await page.locator("#edu-field").fill("Computer Science");
    await page.locator("#edu-start").fill("2016-08-01");
    await page.locator("#edu-end").fill("2020-05-01");
    await page.locator("#edu-description").fill("Graduated with honors");
    await page.getByRole("button", { name: "Add education" }).click();

    await expect(page.getByText("B.S., State University")).toBeVisible();

    await page.getByRole("button", { name: "Edit" }).click();
    await page.locator("#edu-credential").fill("M.S.");
    await page.getByRole("button", { name: "Save changes" }).click();
    await expect(page.getByText("M.S., State University")).toBeVisible();

    await page.getByRole("button", { name: "Delete" }).click();
    await expect(page.getByText("No education added yet.")).toBeVisible();
  });

  test("can add a work experience entry, edit it, cancel an edit, and delete it", async ({
    page,
  }) => {
    await page.locator("#we-company").fill("Acme Corp");
    await page.locator("#we-title").fill("Software Engineer");
    await page.locator("#we-start").fill("2021-06-01");
    await page.getByLabel("This is my current role").check();
    await expect(page.locator("#we-end")).toBeDisabled();
    await page.locator("#we-description").fill("Building scalable web applications");
    await page.getByRole("button", { name: "Add work experience" }).click();

    await expect(page.getByText("Software Engineer · Acme Corp")).toBeVisible();
    await expect(page.getByText("2021-06-01 – Present")).toBeVisible();

    await page.getByRole("button", { name: "Edit" }).click();
    await page.locator("#we-title").fill("Should not be saved");
    await page.getByRole("button", { name: "Cancel" }).click();
    await expect(page.getByText("Software Engineer · Acme Corp")).toBeVisible();

    await page.getByRole("button", { name: "Edit" }).click();
    await page.locator("#we-title").fill("Staff Software Engineer");
    await page.getByRole("button", { name: "Save changes" }).click();
    await expect(page.getByText("Staff Software Engineer · Acme Corp")).toBeVisible();

    await page.getByRole("button", { name: "Delete" }).click();
    await expect(page.getByText("No work experience added yet.")).toBeVisible();
  });
});
