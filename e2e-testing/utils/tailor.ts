import type { Page } from "@playwright/test";
import { expect } from "@playwright/test";

/** Submits a job description through the tailor flow and leaves the browser on the new resume's detail page. */
export async function tailorResume(page: Page, jobDescription: string): Promise<void> {
  await page.goto("/tailor");
  await page.getByLabel("Job description").fill(jobDescription);
  await page.getByRole("button", { name: "Tailor my resume" }).click();

  await expect(page).toHaveURL(/\/resumes\/\d+$/);
}
