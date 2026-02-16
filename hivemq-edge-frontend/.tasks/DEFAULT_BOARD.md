# Default Team Configuration

## Primary Team for HiveMQ Edge Frontend

**Team Name:** Edge
**Team ID:** a4184231-321e-4c61-a53e-0019e1b618c4
**Team Key:** EDG
**Status:** Active

## Why This Team?

This team is designated as the default for AI assistant operations because:

1. **Project Alignment** - Matches the HiveMQ Edge Frontend project context
2. **Active Development** - Currently active and maintained
3. **Team Focus** - Primary team for Edge frontend work

## Issue Identifier Pattern

All Edge team issues follow the pattern: `EDG-{number}`

**Examples:**
- `EDG-40` - Frontend handover documentation
- `EDG-38` - Frontend should ignore read-only information for topic-filter schemas
- `EDG-35` - Visual Rendering of Tags in Data Combinings

## Task Directory Naming

When creating task directories, use the Linear issue identifier:

**Pattern:** `.tasks/{issue-id}-{short-description}/`

**Examples:**
- Linear issue `EDG-40` → `.tasks/EDG-40-technical-documentation/`
- Linear issue `EDG-38` → `.tasks/EDG-38-readonly-schemas/`

**Branch Naming:**
- Git branches may include slashes: `feat/EDG-40/technical-documentation`
- Task directories use hyphens: `EDG-40-technical-documentation`

## Usage in AI Context

When you ask questions without specifying a team, the AI will assume you mean the **Edge team**. For example:

- "What issues are in progress?" → Queries Edge team issues
- "Show me my issues" → Filters Edge team for your assignments
- "What's in backlog?" → Queries Edge team backlog

## Override Default

To query a different team, simply specify it:

- "What's in the Broker Core team?"
- "Show issues from Platform team"
- "List tasks in Documentation team"

## Team Access

You have access to multiple teams across the HiveMQ Linear workspace. The Edge team is the default context for this project's work.

## Related Teams

Other teams you may work with:
- **Integrations** (ID: 859b5bcf-bd85-4298-b515-5fdfdf2c8eb7)
- **Documentation** (ID: 5c21744d-5973-43dd-9122-3d41f2fce423)
- **Platform** (ID: 23bf01ab-58f5-4d65-886a-e60e45e7e025)
- And 20+ other teams in the workspace
