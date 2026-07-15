export interface TestUser {
  fullName: string;
  email: string;
  password: string;
}

/** A fresh, never-before-registered user so tests don't collide with each other or prior runs. */
export function uniqueUser(prefix = "e2e"): TestUser {
  const stamp = Date.now();
  const rand = Math.floor(Math.random() * 100_000);
  return {
    fullName: "E2E Test User",
    email: `${prefix}-${stamp}-${rand}@example.com`,
    password: "SuperSecret123",
  };
}
