You are preparing a review plan for a second reviewer agent.

## PR Context
Read these environment variables for context:
- `PR_NUMBER`
- `PR_TITLE`
- `PR_URL`
- `PR_BASE_REF`, `PR_BASE_SHA`
- `PR_HEAD_REF`, `PR_HEAD_SHA`

## Goal
Produce a concise, actionable plan that helps a reviewer inspect the PR.

## Required workflow
1. Inspect only this PR delta (`$PR_BASE_SHA...$PR_HEAD_SHA`).
2. Identify changed files and classify each one (new/modified/renamed/deleted).
3. Read repository guidance docs if present (`AGENTS.md`).
4. For each changed file, find files it imports or calls, and existing files that solve a similar problem.
5. Call out architectural choices that deviate from existing patterns.
6. Define focused review checks for security, correctness, consistency, and API usage.
7. Decide whether a sequence diagram is required for this PR.

## Output format
Output plain text only (no markdown code fences), using this exact structure:

Review the PR changes for [brief description tied to PR title and scope].

Read all of the following files in parallel before beginning your review:
- [file path or file path:startLine-endLine] - [new/modified/comparison/dependency/guidance — brief reason]
- ...

Include every file the reviewer needs: changed, dependencies, comparisons, and guidance docs. Use line ranges when only a specific section matters.

Architectural choices that deviate from existing patterns:
- [choice and why]

Focus areas:
- Security: [specific concerns]
- Correctness: [logic to verify]
- Performance: [hot paths, queries, async concerns]
- Consistency: [patterns/files to compare against]
- API usage: [external or internal APIs to validate]

Sequence diagram:
- Decision: required | optional
- Reason: [brief rationale tied to runtime/control/data-flow complexity]

## Guidelines
- Call out any security-sensitive code (auth, tokens, credentials)
- Explicitly list similar existing files the reviewer should compare against
- Note any external API usage that should be verified against docs
- Keep descriptions concise but informative
- Omit sections that don't apply
- Do not output anything except the final plan text.
