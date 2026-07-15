import { test, expect } from "@playwright/test";
import { registerNewUser } from "../utils/register";

test.describe("Authentication", () => {
  test("a new user can register and land on their profile", async ({ page }) => {
    await registerNewUser(page, "register");

    await expect(page.getByRole("heading", { name: "Your Master Profile" })).toBeVisible();
  });

  test("a registered user can sign out and sign back in", async ({ page }) => {
    const user = await registerNewUser(page, "login");

    await page.getByRole("button", { name: "Sign out" }).click();
    await expect(page).toHaveURL(/\/login$/);

    await page.getByLabel("Email").fill(user.email);
    await page.getByLabel("Password").fill(user.password);
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page).toHaveURL(/\/profile$/);
  });

  test("shows an error for invalid login credentials", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Email").fill("nonexistent-user@example.com");
    await page.getByLabel("Password").fill("wrong-password");
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page.getByText("Invalid email or password.")).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test("unauthenticated users are redirected to login from protected routes", async ({ page }) => {
    await page.goto("/profile");
    await expect(page).toHaveURL(/\/login$/);

    await page.goto("/resumes");
    await expect(page).toHaveURL(/\/login$/);
  });

  test("can navigate between the login and register pages", async ({ page }) => {
    await page.goto("/login");
    await page.getByRole("link", { name: "Register" }).click();
    await expect(page).toHaveURL(/\/register$/);

    await page.getByRole("link", { name: "Sign in" }).click();
    await expect(page).toHaveURL(/\/login$/);
  });
});
