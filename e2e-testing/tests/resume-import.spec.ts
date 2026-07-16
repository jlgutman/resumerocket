import path from "node:path";
import { fileURLToPath } from "node:url";
import { test, expect } from "@playwright/test";
import { registerNewUser } from "../utils/register";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FIXTURE_PDF = path.join(__dirname, "..", "fixtures", "resume.pdf");

test.describe("Resume import", () => {
  test.beforeEach(async ({ page }) => {
    await registerNewUser(page, "resume-import");
  });

  test("bootstraps an empty profile from an uploaded resume (US1, US2)", async ({ page }) => {
    await page.getByRole("button", { name: "Upload resume to prefill profile" }).click();
    // The visible trigger is a styled button; the underlying <input type="file"> is hidden.
    await page.setInputFiles('input[type="file"]', FIXTURE_PDF);

    await expect(page.getByText('Review data extracted from "resume.pdf"')).toBeVisible();
    await expect(page.getByDisplayValue("Alex Rivera")).toBeVisible();
    await expect(page.getByDisplayValue("Nimbus Analytics")).toBeVisible();

    // Correct a field before confirming (US2) — the corrected value, not the
    // extracted one, must be what ends up on the profile.
    const titleInput = page.locator('input[id^="we-title-"]').first();
    await titleInput.fill("Staff Software Engineer");

    await page.getByRole("button", { name: "Confirm import" }).click();
    await expect(page.getByText('Review data extracted from "resume.pdf"')).not.toBeVisible();

    await expect(page.getByLabel("Full name")).toHaveValue("Alex Rivera");
    await expect(page.getByText("Staff Software Engineer · Nimbus Analytics")).toBeVisible();
  });

  test("cancelling the review leaves the profile unchanged (FR-013)", async ({ page }) => {
    await page.getByRole("button", { name: "Upload resume to prefill profile" }).click();
    await page.setInputFiles('input[type="file"]', FIXTURE_PDF);

    await expect(page.getByText('Review data extracted from "resume.pdf"')).toBeVisible();
    await page.getByRole("button", { name: "Discard" }).click();

    await expect(page.getByText('Review data extracted from "resume.pdf"')).not.toBeVisible();
    await expect(page.getByText("No work experience added yet.")).toBeVisible();
  });

  test("rejects a non-PDF file before any review screen appears (FR-002)", async ({ page }) => {
    await page.getByRole("button", { name: "Upload resume to prefill profile" }).click();
    await page.setInputFiles('input[type="file"]', {
      name: "resume.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("not a pdf"),
    });

    await expect(page.getByText("Only PDF files are supported.")).toBeVisible();
    await expect(page.getByText(/^Review data extracted from/)).not.toBeVisible();
  });
});
