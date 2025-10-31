# Tasks Log Directory

This directory contains ephemeral session documents generated during task work. These are working documents that complement the permanent documentation in `.tasks/`.

## Purpose

- **Temporary logs**: Session-specific documents that don't need to be in git
- **AI-generated summaries**: Quick reference documents created during work sessions
- **Running commentary**: Step-by-step records of what was done
- **Troubleshooting notes**: Detailed fixes and issues resolved

## Naming Convention

All files follow this pattern:

```
{TASK_NUMBER}_{INDEX}_{DESCRIPTIVE_NAME}.md
```

### Examples:

- `25337_01_Phase1_Component_Tests_Complete.md`
- `25337_02_Phase2_E2E_Tests_Complete.md`
- `25337_03_TypeScript_Errors_Fixed.md`
- `25337_04_Accessibility_Select_Fix.md`
- `25337_05_CyWait_Removal_Complete.md`

### Components:

- **TASK_NUMBER**: The task/issue number (e.g., `25337`)
- **INDEX**: Sequential number starting from 01 (e.g., `01`, `02`, `03`)
- **DESCRIPTIVE_NAME**: Clear description using PascalCase or snake_case

## Git Status

This directory is **excluded from git** (see `.gitignore`). These are local working documents that:

- Help track session progress
- Provide quick reference during work
- Don't need to be committed to the repository

## Permanent Documentation

For permanent documentation that should be committed to git, use the `.tasks/` directory structure instead.

---

**Note:** Clean this directory periodically to remove old session logs.
