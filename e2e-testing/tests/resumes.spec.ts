import { test, expect } from "@playwright/test";
import { registerNewUser } from "../utils/register";
import { tailorResume } from "../utils/tailor";
import { saveResumeDetails } from "../utils/save";

test.describe("Tailoring and managing resumes", () => {
  test.beforeEach(async ({ page }) => {
    await registerNewUser(page, "resumes");
  });

  test("can tailor a new resume from a job description", async ({ page }) => {
    await page.getByRole("link", { name: "Tailor a Resume" }).click();
    await expect(page).toHaveURL(/\/tailor$/);

    await page
      .getByLabel("Job description")
      .fill("Senior Backend Engineer with TypeScript and Node.js experience.");
    await page.getByRole("button", { name: "Tailor my resume" }).click();

    await expect(page).toHaveURL(/\/resumes\/\d+$/);
    await expect(page.getByLabel("Resume name")).toHaveValue(/^Senior Backend Engineer/);
  });

  test("can edit and persist the resume name, company, and job title", async ({ page }) => {
    await tailorResume(page, "Backend Engineer role focused on distributed systems.");

    await page.getByLabel("Resume name").fill("Backend Engineer @ Acme");
    await page.getByLabel("Company").fill("Acme Corp");
    await page.getByLabel("Job title").fill("Backend Engineer");
    await saveResumeDetails(page);

    await page.reload();
    await expect(page.getByLabel("Resume name")).toHaveValue("Backend Engineer @ Acme");
    await expect(page.getByLabel("Company")).toHaveValue("Acme Corp");
    await expect(page.getByLabel("Job title")).toHaveValue("Backend Engineer");
  });

  test("selecting a template enables the download button", async ({ page }) => {
    await tailorResume(page, "Frontend Engineer role focused on React and accessibility.");

    await expect(page.getByRole("button", { name: "Download" })).toBeDisabled();

    await page.getByRole("button", { name: "Modern modern style" }).click();

    await expect(page.getByRole("button", { name: "Download" })).toBeEnabled();
  });

  test("can clone a resume", async ({ page }) => {
    await tailorResume(page, "DevOps Engineer role focused on Kubernetes and CI/CD.");
    await page.getByLabel("Company").fill("Acme Corp");
    await saveResumeDetails(page);

    await page.getByRole("link", { name: "My Resumes" }).click();
    await page.getByRole("button", { name: "Clone" }).click();

    await expect(page).toHaveURL(/\/resumes\/\d+$/);
    await expect(page.getByLabel("Resume name")).toHaveValue(/\(Copy\)$/);
    await expect(page.getByLabel("Company")).toHaveValue("Acme Corp");
  });

  test("can regenerate a resume", async ({ page }) => {
    await tailorResume(page, "QA Engineer role focused on automated testing.");
    const originalUrl = page.url();

    await page.getByRole("link", { name: "My Resumes" }).click();
    await page.getByRole("button", { name: "Regenerate" }).click();

    await expect(page).toHaveURL(/\/resumes\/\d+$/);
    expect(page.url()).not.toBe(originalUrl);
  });

  test("can filter resumes by company", async ({ page }) => {
    await tailorResume(page, "Engineer role at a large company.");
    await page.getByLabel("Company").fill("Acme Corp");
    await saveResumeDetails(page);

    await tailorResume(page, "Engineer role at another company.");
    await page.getByLabel("Company").fill("Globex Inc");
    await saveResumeDetails(page);

    await page.getByRole("link", { name: "My Resumes" }).click();
    await page.getByLabel("Filter by company").fill("Globex");

    await expect(page.getByText("Globex Inc")).toBeVisible();
    await expect(page.getByText("Acme Corp")).not.toBeVisible();
  });

  test("can select two resumes and compare them", async ({ page }) => {
    await tailorResume(page, "Engineer role A with Python experience.");
    await tailorResume(page, "Engineer role B with Java experience.");

    await page.getByRole("link", { name: "My Resumes" }).click();

    const compareButton = page.getByRole("button", { name: /Compare selected/ });
    await expect(compareButton).toBeDisabled();

    const checkboxes = page.getByRole("checkbox", { name: /for comparison/ });
    await checkboxes.nth(0).check();
    await checkboxes.nth(1).check();

    await expect(compareButton).toBeEnabled();
    await compareButton.click();

    await expect(page).toHaveURL(/\/resumes\/compare\?leftId=\d+&rightId=\d+$/);
    await expect(page.getByRole("heading", { name: "Compare Resumes" })).toBeVisible();
  });
});
