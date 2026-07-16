import type { Page } from "@playwright/test";

/**
 * Clicks "Save details" and waits for the PATCH to come back before returning, so a
 * following reload or navigation can't cancel the request while it's still in flight.
 */
export async function saveResumeDetails(page: Page): Promise<void> {
  await Promise.all([
    page.waitForResponse(
      (res) =>
        res.request().method() === "PATCH" &&
        /\/tailored-resumes\/\d+$/.test(new URL(res.url()).pathname) &&
        res.ok(),
    ),
    page.getByRole("button", { name: "Save details" }).click(),
  ]);
}
