---
description: Fix a GitHub issue by number
argument-hint: <issue-number>
allowed-tools: Bash(gh *), Bash(git *), Read, Edit, Write, Grep, Glob
---

Fix GitHub issue #$1.

1. Read the issue with `gh issue view $1` to understand the problem and any discussion.
2. Locate the relevant code and diagnose the root cause.
3. Create a branch `fix/issue-$1`.
4. Implement the fix, matching existing code conventions.
5. Add or update tests covering the fix, and run the relevant test suite.
6. Commit with a message referencing the issue (e.g. `Fixes #$1`), then push.
7. Open a PR with `gh pr create`, linking the issue in the body.
