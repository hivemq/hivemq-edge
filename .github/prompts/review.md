You are generating a pull request review for this repository.

## PR Context
Read these environment variables for context:
- `PR_NUMBER`
- `PR_TITLE`
- `PR_URL`
- `PR_BASE_REF`, `PR_BASE_SHA`
- `PR_HEAD_REF`, `PR_HEAD_SHA`
- `REVIEW_PLAN_FILE` (defaults to `pi-review-plan.txt`)

## Steps
1. Parse `REVIEW_PLAN_FILE` to identify:
   - Files to review (new and modified)
   - Files they import or call, and existing files that solve a similar problem
   - Architectural choices that deviate from existing patterns
   - Focus areas (contracts, correctness, security, consistency)
   - Sequence diagram decision (required | optional)

2. Read all files in parallel:
   - Read `AGENTS.md` if present.
   - Read all files listed in the review plan (new, modified, comparisons, dependencies).
   - When line ranges are specified (e.g., `file:100-150`), read only those ranges.
   - Do not read listed files one at a time.
   - If `REVIEW_PLAN_FILE` is missing, continue using the PR diff directly.

3. Review only the PR delta, using context files from the review plan:
   - **Contracts**: Do interfaces with existing code match? (function signatures, SQL, serialization, expected behavior of called code)
   - **Correctness**: Logic errors, edge cases, error handling
   - **Security**: Auth handling, input validation, secret leakage
   - **Performance**: Unnecessary allocations, N+1 queries, blocking calls in async code, algorithmic complexity
   - **Consistency**: Compare against similar implementations and codebase patterns

## Diagram policy
Use the review plan's `Sequence diagram` decision:
- If `Decision: required`, include one Mermaid sequence diagram for the actual runtime/control/data flow.
- If `Decision: optional`, omit the sequence diagram section.
- If the plan is missing or has no decision, include a diagram only when the PR changes meaningful runtime/control/data flow.

## Output format (must match exactly)
<h2>PR Review</h2>

<h3>Summary</h3>
[2-4 sentence overall assessment]

<h3>Issues</h3>
Use this exact structure (one `<li>` per issue, do not combine issues):
<ol>
  <li>
    <strong>[severity]</strong> <code>file_path:line_or_range</code> - [description with impact, max 3 lines]
    <ul>
      <li><strong>Fix:</strong> [if applicable, max 3 lines]</li>
    </ul>
  </li>
</ol>

Severities: critical (security, data loss), high (bugs, incorrect behavior), medium (perf, maintainability), low (style, minor)

If no issues:
<p>No actionable issues found in changed code.</p>

<h3>Questions</h3>
Use an HTML ordered list with one `<li>` per clarification question:
<ol>
<li>[question]</li>
</ol>
If none:
<p>None.</p>

If diagram is required, include:
<h3>Sequence Diagram</h3>

```mermaid
sequenceDiagram
    [runtime/control/data flow introduced by this PR]
```

If diagram is optional, omit the entire Sequence Diagram section.

Return only the final review markdown.
