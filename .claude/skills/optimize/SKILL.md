---
name: "optimize"
description: "Run a full-stack performance pass on ResumeRocket — profile a slow flow end-to-end (React/Vite frontend → Spring Boot API → MySQL) and find the real bottleneck before touching code. Use whenever the user says something is slow, laggy, or janky; asks to optimize/speed up/profile an endpoint, page, query, render, or bundle; mentions N+1 queries, missing indexes, slow load times, large bundles, or excessive re-renders; or asks to make the app faster. Prefer this over ad-hoc edits for anything performance-related, even when the user doesn't say the word 'optimize'."
argument-hint: "[target: an endpoint, page, flow, or file] [--report | --apply]"
compatibility: "ResumeRocket monorepo — backend/ (Spring Boot + MySQL via Flyway/JPA), frontend/ (React + Vite + react-query)"
user-invocable: true
disable-model-invocation: false
---

## User Input

```text
$ARGUMENTS
```

The argument is the optimization **target** plus an optional mode flag. Consider it before doing anything else.

- A target may be an endpoint (`GET /resumes/{id}`), a page (`ProfileEditor`), a described symptom ("saving my profile takes 3 seconds"), or a file path.
- `--report` → investigate and hand back a prioritized findings report; change nothing.
- `--apply` → investigate, then implement the fixes and verify them.
- If no flag is given, do the full investigation first, then **stop and ask** which findings to apply before editing anything. Never start editing on an unqualified invocation — the point of this skill is to measure before cutting, and the user chose "ask me each time."

If no target is given at all, ask what feels slow (a page, an endpoint, a flow) rather than guessing.

## Why this skill exists

The expensive mistake in performance work is optimizing the wrong layer — rewriting a React component when the real cost is an N+1 query two tiers down, or adding a database index for a route whose latency is actually in JSON serialization. ResumeRocket spans three tiers (browser → `/api/v1` → MySQL), so a single "this is slow" almost always has one dominant cost that lives in exactly one of them. This skill's job is to **locate that cost with evidence before writing any code**, then fix the layer that actually matters.

## The method

Work top-down through the tiers, following the request. Stop drilling once you've found where the time actually goes — you don't need to profile all three tiers if the first one explains the latency.

### 1. Frame the flow

Trace the target across tiers so you know every hop involved. For a user-visible symptom, name the page component, the `services/*.ts` wrapper it calls, the controller/service on the backend, and the entities/queries underneath. `analyzer` (the read-only cross-file agent) is the fast way to get this map — route the "trace this flow end to end" question to it rather than reading files one by one.

Write down the hops before measuring. You're looking for the one that dominates.

### 2. Measure each layer with evidence, not guesses

Never assert a bottleneck you haven't observed. Gather real signal:

**Database (usual suspect #1 — N+1 and missing indexes):**
- Turn on SQL logging and count queries per request. If one logical fetch fires N+1 statements, that's almost always the fix. Look for `@OneToMany`/`@ManyToOne` lazy loads iterated in a loop, and DTO mapping that touches child collections (`MasterProfile` → education/work/skills is the prime candidate).
- Check `EXPLAIN` on slow queries and confirm the columns in `WHERE`/`JOIN` are indexed. Remember schema is **owned by Flyway** — a new index is a new `V*__*.sql` migration in `backend/src/main/resources/db/migration`, never a JPA/Hibernate change and never an edit to an existing applied migration.
- Watch for over-fetching: loading full entities to return a small DTO, `fetch = EAGER` pulling subtrees, or `findAll` where a paged/filtered query belongs.

**Backend (usual suspect #2 — work done per request):**
- Time the endpoint (`curl -w '%{time_total}'` against `http://localhost:8080/api/v1/...`, or logs/timers). Subtract the DB time you measured; what's left is app-layer cost.
- Look for repeated work that could be cached or hoisted, synchronous calls that block, and serialization of large object graphs. Be deliberate about Jackson version (this project pins Jackson 2 alongside Boot 4's Jackson 3 default).
- **Never** treat the OpenAI/`ChatClient` call in `tailoring/` as an optimization target by making it faster/parallel without saying so — it's an external paid call; surface it as a finding, don't silently restructure it.

**Frontend (usual suspect #3 — waterfalls, re-renders, bundle):**
- Network waterfall: sequential dependent requests that could be parallel, missing `react-query` caching causing refetch storms, no pagination on a large list.
- Render cost: unstable props/deps causing re-renders, expensive work in render that belongs in `useMemo`, list rendering without keys/virtualization.
- Bundle: run `npm run build` and read the output sizes; look for heavy deps pulled into the initial chunk that could be lazy/route-split.

### 3. Rank findings by impact

Order by measured (or well-estimated) latency saved, not by how easy the fix is. State each finding as: **layer → what's slow → evidence → proposed fix → estimated win**. A 400 ms N+1 fix outranks a 5 ms memoization every time. Call out anything that's a correctness risk to change (caching that could serve stale data, index that changes query plans elsewhere).

### 4. Report or apply

**Report mode** — hand back the ranked findings in this shape and stop:

```
# Optimization report: <target>

## Bottleneck
<the one dominant cost, with the measurement that proves it>

## Findings (ranked by impact)
1. [DB] <what> — <evidence> — fix: <how> — est. <win>
2. [Backend] ...
3. [Frontend] ...

## Recommended next step
<which one to apply first, and why>
```

**Apply mode** — implement the accepted findings, smallest-blast-radius first, then **verify the win is real, not assumed**:
- Re-measure the same signal you used in step 2 (query count, `time_total`, bundle size) and report before/after numbers.
- Confirm you didn't break behavior: run the relevant suite for the tier you touched (`mvn test` for backend — needs Docker for Testcontainers; `npm run test` / `npm run build` for frontend). For a change worth verifying against the live stack, route to the `test-runner` agent.
- Run `mvn spotless:apply` after any backend edits (formatting is unbound from the build here).
- If a fix didn't move the number, say so and revert it rather than leaving speculative complexity behind.

## Guardrails

- **Evidence before edits.** If you catch yourself about to change code without a measurement that names the bottleneck, stop and measure first. A plausible-sounding optimization with no number behind it is the thing this skill exists to prevent.
- **One migration per schema change**, forward-only, matching entities (`ddl-auto: validate` will reject drift).
- **Don't micro-optimize what doesn't dominate.** If the DB is 90% of the latency, memoizing a component is noise — note it and move on.
- Keep changes reviewable and scoped to the target; a perf pass is not a refactor.
