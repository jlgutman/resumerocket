---
name: code-review-team
description: Agent Teams PR code review with Devil's Advocate. Spawns 5 team members — 4 specialist reviewers + 1 adversarial challenger — to produce confidence-rated findings. Only findings that survive cross-examination make the final report.
---

# Code Review Team with Devil's Advocate

Orchestrate a team-based code review using Agent Teams (`TeamCreate` + `SendMessage`). Four specialist reviewers analyze a PR's diff in parallel, then a Devil's Advocate cross-examines every finding. Original reviewers must defend, adjust, or withdraw. Only findings that survive the challenge — each carrying a confidence rating — appear in the final report.

## When to Use This Skill

- Before merging a PR — get high-confidence, adversarially filtered feedback instead of a raw findings dump
- For high-stakes PRs — large features, infra/migration changes, auth/payment code, anything where a false positive wastes real review time
- When you want fewer, better findings — the Devil's Advocate phase exists specifically to kill noise
- For PRs with ticket context — auto-detects a linked ticket and checks findings against acceptance criteria

## Parameters

| Parameter | Default | Description |
|---|---|---|
| (first arg) | required | PR URL or PR number |
| `ticket` | auto-detect | Ticket key override (e.g. `ticket=PROJ-94`) |
| `skip-challenge` | `false` | Skip the Devil's Advocate phase — degrades to plain parallel review |
| `verbose` | `false` | Include the full challenge/defense transcript in the report |

## Invocation

```
/code-review-team https://github.com/owner/repo/pull/123
/code-review-team 123
/code-review-team 123 ticket=PROJ-94
/code-review-team 123 skip-challenge=true
/code-review-team 123 verbose=true
```

## Architecture

```
ORCHESTRATOR (main session — team lead, manages state + synthesis)
|
+-- Phase 0: INIT
|   Parse PR URL, fetch metadata + diff, auto-detect ticket
|   Read CLAUDE.md for project conventions
|   TeamCreate("code-review-{number}")
|
+-- Phase 1: TASK CREATION
|   Create 5 tasks in shared task list (TaskCreate)
|   Assign 4 reviewer tasks to specialist teammates
|
+-- Phase 2: PARALLEL REVIEW (4 agents work simultaneously)
|   +-- staff-engineer      (code-refactorer)
|   +-- qa-engineer          (general-purpose)
|   +-- security-reviewer    (general-purpose)
|   +-- architect            (system-architect)
|   Each sends findings JSON to team lead via SendMessage
|
+-- Phase 3: DEVIL'S ADVOCATE CHALLENGE
|   +-- devils-advocate      (code-refactorer)
|   Receives ALL aggregated findings from Phase 2
|   Sends targeted challenges to each original reviewer
|
+-- Phase 4: DEFENSE / REBUTTAL (4 agents respond)
|   Each reviewer: DEFEND / ADJUST / WITHDRAW
|   Sends responses to devils-advocate
|
+-- Phase 5: VERDICT
|   devils-advocate assigns confidence ratings
|   Sends final verdict to team lead
|
+-- Phase 6: SYNTHESIS
|   Build unified report, shutdown teammates, TeamDelete
|   Present report to user
|
+-- STATE: .code-review-team/state.json
```

## Team Members

| Name | Sub-Agent Type | Role | Focus |
|---|---|---|---|
| `staff-engineer` | `code-refactorer` | Staff Engineer | Bugs, project convention violations, framework misuse, TypeScript soundness, performance |
| `qa-engineer` | `general-purpose` | QA Engineer | Test coverage gaps, edge cases, error paths, acceptance criteria |
| `security-reviewer` | `general-purpose` | Security Reviewer | Auth bypasses, silent failures, input validation, data exposure, injection |
| `architect` | `system-architect` | Architect | Scalability, pattern violations, DB design, N+1 queries, migrations |
| `devils-advocate` | `code-refactorer` | Devil's Advocate | Challenges ALL findings — questions assumptions, demands evidence, filters false positives |

Devil's Advocate uses `code-refactorer` (same as staff-engineer) because it needs strong code-reading ability to open every file reviewers reference and check claims against actual source — not just take the reviewer's word for it.

## Workflow

### Phase 0: Initialize

Parse `$ARGUMENTS` for the PR identifier and parameters:

- PR URL: extract from `https://github.com/owner/repo/pull/123`
- PR number: bare number like `123` — uses current repo
- Parameters: `ticket=PROJ-94`, `skip-challenge=true`, `verbose=true`

Fetch PR metadata and diff:

```bash
gh pr view <PR> --json title,body,additions,deletions,changedFiles,baseRefName,headRefName,labels,number
gh pr diff <PR>
gh pr view <PR> --json commits
```

If `gh pr view` fails, ask the user to confirm auth (`gh auth status`) and verify the URL before continuing.

Read the project's `CLAUDE.md` (and any convention docs it references) and build a convention context block covering: API patterns, state management rules, error handling conventions, code-reuse/centralization rules, UI component patterns, database/scalability rules, logging conventions, and any other project-specific rules. This block is injected into every reviewer prompt.

Ticket auto-detection: scan the PR title and branch name for `{PROJECT_KEY}-\d+` patterns (e.g. `PROJ-123`). If found, or if `ticket=` was passed, fetch the ticket via available Jira MCP tools (`fields.summary`, `fields.description`, acceptance-criteria custom field, status, priority). Non-blocking — proceed without ticket context if unavailable.

Create `.code-review-team/` and initialize `state.json`:

```json
{
  "prNumber": 123,
  "prTitle": "PR title",
  "prUrl": "https://github.com/owner/repo/pull/123",
  "teamName": "code-review-123",
  "ticketKey": "PROJ-94",
  "parameters": { "skipChallenge": false, "verbose": false },
  "currentPhase": 0,
  "status": "in_progress",
  "startedAt": "2026-07-15T10:00:00Z",
  "agents": {
    "staff-engineer": { "status": "pending", "findingsCount": 0 },
    "qa-engineer": { "status": "pending", "findingsCount": 0 },
    "security-reviewer": { "status": "pending", "findingsCount": 0 },
    "architect": { "status": "pending", "findingsCount": 0 },
    "devils-advocate": { "status": "pending" }
  },
  "rawFindings": [],
  "challengedFindings": [],
  "finalFindings": [],
  "verdict": null
}
```

If `state.json` already exists for this PR, offer to resume:

```
AskUserQuestion:
  question: "Found existing review state for PR #{number}. Phase {N} was last completed. Resume or start fresh?"
  header: "Resume"
  options:
    - label: "Resume from Phase {N+1}"
      description: "Continue where you left off"
    - label: "Start fresh"
      description: "Delete state and start over"
```

Create the team:

```
TeamCreate:
  team_name: "code-review-{number}"
  description: "Code Review Team for PR #{number} - {title}"
```

### Phase 1: Create Tasks and Spawn Team

Create 5 tasks via `TaskCreate`:

1. Staff Engineer Review — "Review PR #{number} for bugs, code quality, and project convention compliance"
2. QA Engineer Review — "Review PR #{number} for test coverage gaps, edge cases, and error path handling"
3. Security Review — "Review PR #{number} for auth bypasses, silent failures, input validation, and data exposure"
4. Architecture Review — "Review PR #{number} for scalability, pattern violations, DB design, and N+1 queries"
5. Devil's Advocate Challenge — "Challenge ALL review findings — question assumptions, demand evidence, filter false positives"

Spawn the 4 reviewer teammates with the Task tool, passing `team_name`. The Devil's Advocate is spawned later, in Phase 3.

Each reviewer teammate receives: the PR diff, the convention context block, ticket context (if any), their specific review focus, and instructions to send findings as JSON via `SendMessage` to the team lead.

### Phase 2: Parallel Review (4 Agents)

All 4 reviewers work simultaneously and report back independently — none of them see each other's findings in this phase.

**Required output format (all reviewers):**

```json
{
  "reviewer": "staff-engineer",
  "findings": [
    {
      "id": "SE-1",
      "severity": "critical|important|suggestion",
      "file": "path/to/file.tsx",
      "line": 123,
      "issue": "Brief description of the problem",
      "suggestion": "Specific fix or improvement",
      "evidence": "Code snippet or reasoning that supports this finding"
    }
  ]
}
```

Max 10 findings per reviewer — high-impact issues only. Finding ID prefixes: `SE-` (staff-engineer), `QA-` (qa-engineer), `SR-` (security-reviewer), `AR-` (architect).

#### Staff Engineer prompt (sub-agent: `code-refactorer`, name: `staff-engineer`)

```
You are a Staff Engineer reviewing PR #{number}: "{title}".

## PR Diff
{full diff}

## Changed Files
{file list}

{ticket context if available}

## Project Conventions
{convention context block extracted from CLAUDE.md}

## Your Review Focus
- Bugs, logic errors, and incorrect behavior
- Project convention violations (check every rule in the conventions block above)
- Framework-specific issues (hook misuse, missing deps, memory leaks, unnecessary re-renders)
- TypeScript type safety (any casts, missing types, unsound assertions)
- Performance issues (N+1 queries, missing memoization, blocking operations)
- Code quality (naming, structure, readability)

## Evidence Requirements
Every finding MUST include:
- Exact file path + line number (e.g. `src/api/route.ts:45`)
- Code snippet from the diff showing the violation
- Specific convention rule name being violated
- User impact: what breaks, degrades, or becomes vulnerable

Findings without code evidence will be dismissed by the Devil's Advocate.

## Devil's Advocate Warning
Your findings WILL be challenged by a DA with full codebase access. They will open every file
you reference, search for existing handling you might have missed, and dismiss findings without
code evidence. To survive: cite exact lines, quote code, name the convention rule.

## Instructions
1. Review the diff thoroughly against the conventions above
2. For each finding, provide concrete evidence (code snippets, line numbers)
3. Send your findings as JSON via SendMessage to the team lead

MAX 10 findings. Focus on high-impact issues only.
```

#### QA Engineer prompt (sub-agent: `general-purpose`, name: `qa-engineer`)

```
You are a QA Engineer reviewing PR #{number}: "{title}".

## PR Diff
{full diff}

## Changed Files
{file list}

{ticket context if available}
{acceptance criteria if available}

## Project Conventions
{convention context block extracted from CLAUDE.md}

## Your Review Focus
- Missing unit/integration tests for new functionality
- Untested edge cases (empty arrays, null values, boundary conditions)
- Error path coverage gaps (network failures, auth failures, validation errors)
- Regression risks from changed behavior
- Test quality (descriptive names, deterministic, independent)
- Acceptance criteria coverage (if ticket context provided)

## Convention Testing Compliance
Check that tests cover project-specific patterns:
- API client success/error path tests (both branches)
- State management store tests (state changes, reset, selector behavior)
- Server-side auth flow tests, especially auth failure paths
- Error boundary behavior for data-driven components
- Background operation failure paths

## Devil's Advocate Warning
Your findings WILL be challenged by a DA with full codebase access. They will open every file
you reference, search for existing handling you might have missed, and dismiss findings without
code evidence. To survive: cite exact lines, quote code, name the convention rule.

## Instructions
1. For each changed file with logic, check if corresponding tests exist and are adequate
2. Identify specific test cases that should be written
3. If ticket acceptance criteria are available, map each to implementation evidence
4. Send your findings as JSON via SendMessage to the team lead

MAX 10 findings. Focus on critical coverage gaps.
```

#### Security Reviewer prompt (sub-agent: `general-purpose`, name: `security-reviewer`)

```
You are a Security Reviewer reviewing PR #{number}: "{title}".

## PR Diff
{full diff}

## Changed Files
{file list}

{ticket context if available}

## Project Conventions
{convention context block extracted from CLAUDE.md}

## Your Review Focus
- Auth bypasses (missing auth checks, trusting client-supplied user identity)
- Silent failures (empty catch blocks, swallowed errors, inappropriate fallbacks)
- Input validation gaps (missing schema validation, SQL injection vectors, XSS)
- Data exposure (error internals in UI responses, PII in logs, secrets in code)
- Missing authorization checks on protected routes
- Error suppression without user feedback
- Background operations with no failure feedback

## Common Security Anti-Patterns
- **Auth from body**: user identity read from request body instead of server-side session
- **Error message leakage**: raw DB/API error messages exposed in UI or API responses
- **Bare catch blocks**: `catch {}` / `catch (_e) {}` with no logging
- **Bare fire-and-forget**: detached async with `.catch(() => {})` — silent swallow, no logging
- **Hardcoded secrets/URLs**: API keys or internal service URLs in source instead of env vars

## Devil's Advocate Warning
Your findings WILL be challenged by a DA with full codebase access. They will open every file
you reference, search for existing handling you might have missed, and dismiss findings without
code evidence. To survive: cite exact lines, quote code, name the convention rule.

## Instructions
1. Focus on security-critical code paths (auth, data access, mutations)
2. Check every catch block for proper error handling
3. Verify auth checks exist on all protected operations
4. Send your findings as JSON via SendMessage to the team lead

MAX 10 findings. Focus on actual security risks, not theoretical concerns.
```

#### Architect prompt (sub-agent: `system-architect`, name: `architect`)

```
You are a System Architect reviewing PR #{number}: "{title}".

## PR Diff
{full diff}

## Changed Files
{file list}

{ticket context if available}

## Project Conventions
{convention context block extracted from CLAUDE.md}

## Your Review Focus
- Scalability issues (client-side aggregation, missing pagination, unbounded queries)
- Pattern violations (not following established service/repository/mapping patterns)
- Database design (missing indexes, security policy gaps, migration quality)
- N+1 queries and unnecessary database round-trips
- Architectural misalignment (wrong abstraction level, wrong file location)
- Over-engineering (premature abstractions, unnecessary indirection)

## Common Architecture Anti-Patterns
- **Raw DB rows in UI**: DB rows passed directly to UI instead of DTO/mapping patterns
- **Wildcard selects**: `.select('*')` / `SELECT *` instead of named columns
- **Client-side aggregation**: fetching all rows and summing/counting in JS instead of SQL
- **Missing cascade updates**: new tables storing user data not added to cleanup/deletion flows
- **Duplicated logic**: business logic repeated across files instead of a shared service

## Devil's Advocate Warning
Your findings WILL be challenged by a DA with full codebase access. They will open every file
you reference, search for existing handling you might have missed, and dismiss findings without
code evidence. To survive: cite exact lines, quote code, name the convention rule.

## Instructions
1. Evaluate architectural impact of the changes
2. Check database queries for scalability (10K rows? 100K rows?)
3. Verify patterns match existing codebase conventions
4. Send your findings as JSON via SendMessage to the team lead

MAX 10 findings. Focus on concerns that affect maintainability or scale.
```

### Phase 3: Devil's Advocate Challenge

Skip this phase entirely if `skip-challenge=true` — instead assign every raw finding MEDIUM confidence and jump to Phase 6.

Once all 4 reviewers report in:

1. Aggregate findings into `.code-review-team/findings-raw.json`
2. Update state: `currentPhase: 3`
3. Spawn the Devil's Advocate (sub-agent: `code-refactorer`, name: `devils-advocate`)

```
You are a Devil's Advocate on a code review team. Your job is to CHALLENGE every finding from
4 specialist reviewers. You are equally skeptical of ALL findings — your goal is to filter false
positives, calibrate severity, and catch issues the reviewers missed.

## PR Context
PR #{number}: "{title}"
{brief PR description}

## PR Diff
{full diff}

## Project Conventions
{convention context block extracted from CLAUDE.md}

## All Review Findings
{JSON array of all findings from all 4 reviewers}

## Phase 1: Independent Verification
Before challenging anything, run your own pattern scan on the diff:

| Pattern | Search For | Convention Category |
|---|---|---|
| Unsafe date parsing | `new Date(` on string args | Date Handling |
| Bare catch blocks | `catch {`, `catch (_` | Error Handling |
| Server-side console logging | `console.log/error/warn` in server code | Logging |
| Wildcard selects | `.select('*')`, `SELECT *` | Database Queries |
| Error message leakage | raw error messages in UI state, toast, or API responses | Error Handling |
| Auth from body | user identity from request body instead of server session | Authentication |
| Bare fire-and-forget | `.catch(() => {})`, `.catch(() => undefined)` | Error Handling |
| Hardcoded secrets | API keys, tokens, passwords in source | Security |
| Missing input validation | unvalidated request body used directly | Input Validation |

Record what you find — this calibrates reviewer accuracy and may surface false negatives.

## Phase 2: Challenge Protocol
For EACH finding apply all 6 checks:

1. **False Positive Check** — open the actual file. DISMISS if already handled elsewhere nearby.
2. **Severity Calibration** — blast radius x likelihood. DISMISS "critical" if blast radius <1%
   of users AND requires unlikely conditions.
3. **Fix Quality Check** — does the suggested fix introduce new issues or unneeded complexity?
   DISMISS if the suggestion is worse than the current code.
4. **Actionability Test** — DISMISS vague findings with no concrete code change specified.
5. **Context Verification** — search the codebase for handling the reviewer may have missed.
   DISMISS if existing code already handles it within ~20 lines of the flagged location.
6. **Convention Accuracy** — verify the cited convention rule actually says what's claimed.
   DISMISS if it doesn't.

## Phase 3: Send Challenges
For each finding: open the referenced file and read surrounding context, search the codebase
for related handling, write a specific (not generic) challenge, and include your
`verificationResult` — what you actually found when you checked.

Group challenges by reviewer and send via SendMessage — challenges for staff-engineer's findings
go to staff-engineer, and so on for each of the 4 reviewers.

## Challenge Message Format (send to each reviewer)
{
  "challenges": [
    {
      "findingId": "SE-1",
      "challengeType": "false_positive|severity_calibration|fix_quality|actionability|context_verification|convention_accuracy",
      "challenge": "Specific question or counterargument",
      "evidence": "What you found in the codebase that supports your challenge",
      "verificationResult": "I opened {file}:{line} and found: {what you saw}",
      "recommendation": "dismiss|reduce_severity|accept_as_is"
    }
  ]
}

Wait for responses from all 4 reviewers before proceeding to verdict.
Save challenges to .code-review-team/challenge-log.json
```

### Phase 4: Defense / Rebuttal (4 Agents Respond)

The reviewers spawned in Phase 2 stay alive and receive challenges directly from the Devil's Advocate via `SendMessage`. Each reviewer must respond to every challenged finding with exactly one action:

```json
{
  "responses": [
    {
      "findingId": "SE-1",
      "action": "DEFEND",
      "evidence": "Specific file paths, line numbers, code snippets that prove this is real",
      "updatedFinding": null
    },
    {
      "findingId": "SE-2",
      "action": "ADJUST",
      "evidence": "The DA raised a fair point about severity",
      "updatedFinding": {
        "severity": "suggestion",
        "issue": "Revised issue description",
        "suggestion": "Revised suggestion"
      }
    },
    {
      "findingId": "SE-3",
      "action": "WITHDRAW",
      "reason": "DA correctly identified this is already handled in middleware"
    }
  ]
}
```

**Evidence tiers (cap confidence in Phase 5):**

| Tier | Evidence Quality | Max Confidence | Example |
|---|---|---|---|
| 1 (strongest) | Code snippet + exact line number + convention rule name | HIGH (90-100%) | "Line 45 of route.ts uses `catch {}` — violates error handling rules" |
| 2 | File reference + logical argument | MEDIUM (60-89%) | "route.ts doesn't validate auth — likely trusts request body" |
| 3 (weakest) | General reasoning only, no code reference | LOW (30-59%) | "This pattern could lead to security issues" |

A finding backed only by Tier 3 evidence cannot reach HIGH confidence, no matter how strongly the reviewer insists.

### Phase 5: Verdict

The Devil's Advocate collects all responses and assigns confidence:

| Confidence | Range | Criteria |
|---|---|---|
| HIGH | 90-100% | Defended with code evidence, DA agrees it's valid |
| MEDIUM | 60-89% | Defended but DA has minor reservations, or finding was adjusted |
| LOW | 30-59% | Weak defense, DA can't disprove but stays skeptical |
| Dropped | <30% | DA override — removed from final report |

Verdict sent to team lead:

```json
{
  "verdict": [
    {
      "findingId": "SE-1",
      "confidence": 95,
      "confidenceLevel": "HIGH",
      "evidenceTier": 1,
      "severityVerified": true,
      "status": "defended",
      "summary": "Finding is valid — reviewer provided clear evidence of missing auth check"
    },
    {
      "findingId": "SE-2",
      "confidence": 70,
      "confidenceLevel": "MEDIUM",
      "evidenceTier": 2,
      "severityVerified": true,
      "status": "adjusted",
      "summary": "Severity reduced from critical to important — quality concern, not security"
    },
    {
      "findingId": "SE-3",
      "confidence": 0,
      "confidenceLevel": "Dropped",
      "evidenceTier": 3,
      "severityVerified": false,
      "status": "withdrawn",
      "summary": "Reviewer withdrew — pattern already handled in middleware"
    }
  ],
  "falseNegatives": [
    {
      "id": "FN-1",
      "severity": "important",
      "file": "path/to/file.tsx",
      "line": 78,
      "issue": "Bare `catch {}` block — no reviewer flagged this",
      "conventionRule": "Error Handling",
      "foundBy": "pattern_scan"
    }
  ],
  "stats": {
    "totalChallenged": 25,
    "defended": 12,
    "adjusted": 5,
    "withdrawn": 8,
    "falsePositiveRate": 32,
    "falseNegativesFound": 1
  }
}
```

`falseNegatives` are issues the DA's own Phase 1 pattern scan caught that no reviewer flagged — added to the report at MEDIUM confidence. `severityVerified: false` means the original reviewer's severity stands, but flagged with a note that the DA couldn't independently confirm blast radius/likelihood.

### Phase 6: Synthesis

1. Build the final report from verdict data
2. Save to `.code-review-team/report.md`
3. Shut down all teammates (`SendMessage` with `type: "shutdown_request"`)
4. `TeamDelete` to clean up the team
5. Update `state.json` to `status: "completed"`
6. Present the report to the user

## Output Report

```markdown
# Code Review: PR #{number} - {title}

## Verdict Summary
| Metric | Value |
|---|---|
| Raw findings (pre-challenge) | N |
| Defended | N |
| Adjusted | N |
| Withdrawn | N |
| **Final findings** | **N** |
| False positive rate | N% |

## Critical Issues (must fix before merge)
### 1. [HIGH 95%] {Issue title}
**Source**: {Reviewer name} | **Defended against challenge**
**File**: `path/to/file.tsx:123`
**Issue**: {Description}
**Suggestion**: {Specific fix}
**Devil's Advocate**: {What was challenged and how it was defended}

## Important Issues
{same format, important-severity}

## Suggestions
{same format, suggestion-severity}

## Withdrawn Findings (filtered by Devil's Advocate)
| # | Finding | Reviewer | Withdrawal Reason |
|---|---|---|---|

## Convention Violations Summary
| Rule | Violations | Files |
|---|---|---|

## False Negatives (found by Devil's Advocate)
### 1. [MEDIUM 65%] {Issue title}
**Found by**: DA pattern scan
**File**: `path/to/file.tsx:78`
**Issue**: {Description}
**Convention Rule**: {Rule name from CLAUDE.md}

## Acceptance Criteria Coverage (if ticket found)
| Criterion | Status | Evidence |
|---|---|---|
```

If `verbose=true`, append a "Challenge/Defense Transcript" section with the full back-and-forth for each finding.

## Communication Flow

```
Phase 2:  staff-engineer ------> team-lead (findings JSON)
          qa-engineer ---------> team-lead
          security-reviewer ---> team-lead
          architect -----------> team-lead

Phase 3:  team-lead -----------> devils-advocate (aggregated findings)
          devils-advocate -----> staff-engineer (challenges)
          devils-advocate -----> qa-engineer (challenges)
          devils-advocate -----> security-reviewer (challenges)
          devils-advocate -----> architect (challenges)

Phase 4:  staff-engineer ------> devils-advocate (DEFEND/ADJUST/WITHDRAW)
          qa-engineer ---------> devils-advocate
          security-reviewer ---> devils-advocate
          architect -----------> devils-advocate

Phase 5:  devils-advocate -----> team-lead (verdict + confidence ratings)

Phase 6:  team-lead -----------> all (shutdown_request)
```

## State Files

| File | Purpose |
|---|---|
| `.code-review-team/state.json` | Phase tracking, agent status, parameters |
| `.code-review-team/findings-raw.json` | All findings before challenge |
| `.code-review-team/challenge-log.json` | Challenges + responses |
| `.code-review-team/report.md` | Final output report |

## Error Handling

| Scenario | Action |
|---|---|
| `gh pr view` fails | Ask user to confirm auth (`gh auth status`) and verify URL |
| Reviewer agent fails | Continue with remaining agents, note the gap in the report |
| DA fails before sending challenges | Fall back to standard mode — present raw findings at MEDIUM confidence |
| DA fails mid-challenge | Use challenges sent so far, mark the rest "unchallenged" MEDIUM confidence |
| Reviewer doesn't respond to a challenge | Finding marked "undefended" LOW confidence |
| Jira MCP fails | Continue without ticket context (non-blocking) |
| `CLAUDE.md` not found | Warn the user, proceed with a generic best-practice review |
| Early exit / error | Shut down all teammates, `TeamDelete`, update state to `status: "error"` |

## Tips

- Start with a real PR — the skill needs a PR URL or number to fetch diff and metadata.
- Use `skip-challenge=true` for a quick pass — parallel review without the adversarial filter.
- Check `.code-review-team/report.md` afterward — the full report is saved for reference.
- A high withdrawal/false-positive rate is a good sign — it means the DA is filtering noise.
- Use `verbose=true` when you want to see the reasoning behind confidence ratings, not just the verdict.
- `CLAUDE.md` drives the review — the richer the project conventions, the more targeted the findings.
