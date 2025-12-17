# User Documentation: JavaScript Validation for DataHub Scripts

**Created:** December 11, 2025  
**Feature:** Real-time JavaScript validation in Script Editor  
**Version:** 1.0

---

## JavaScript Validation: Catch Errors Before They Happen

### What It Is

HiveMQ Edge DataHub now includes **real-time JavaScript validation** that checks your transformation scripts as you type. Instead of discovering syntax errors after deployment, you'll see immediate feedback highlighting problems while you're still editing.

The validation catches common JavaScript mistakes:

- **Syntax Errors** - Missing braces, parentheses, or semicolons
- **Undefined Variables** - Typos in variable names that would cause runtime failures
- **Unclosed Strings** - Missing quotation marks that break your code
- **Invalid Code Structures** - Malformed functions or expressions

All validation happens instantly in your browser using TypeScript's powerful compiler—no code is executed, keeping your environment secure.

---

### How It Works

1. **Open the Script Editor** by creating a new script or editing an existing one
2. **Type your transformation function** - validation runs automatically as you edit
3. **See errors immediately** - syntax problems appear beneath the code editor with line and column numbers
4. **Fix the errors** - the validation updates in real-time as you correct issues
5. **Save with confidence** - the Save button is disabled until all errors are resolved

The validation is synchronous and fast—you'll see feedback instantly without any delay, even for complex scripts.

---

### How It Helps

#### Catch Mistakes Early

Discover syntax errors while you're writing code, not after deploying to production. A missing closing brace or typo in a variable name is caught immediately, saving debugging time later.

#### Prevent Invalid Scripts

The Save button stays disabled when validation errors exist, preventing you from accidentally saving broken code. You can't create a script that will fail at runtime due to syntax problems.

#### Learn JavaScript Better

Clear error messages with line and column numbers help you understand what's wrong. If you mistype a variable name, the validator suggests the correct one—turning errors into learning opportunities.

#### Work Faster

No need to save and test to find syntax errors. Fix problems as you type and iterate quickly on your transformation logic without leaving the editor.

---

### Looking Ahead

This JavaScript validation represents our **initial implementation** focused on catching syntax and basic semantic errors. **We're collecting feedback on which errors are most helpful and which edge cases need better handling.**

The validation currently works in **non-strict JavaScript mode**, catching syntax errors and obvious typos while being permissive about runtime patterns. As DataHub evolves, we may add more sophisticated validation rules based on real-world transformation patterns.

**If you encounter scripts that should validate but show errors, or vice versa, please share your examples**—they help us refine the validation rules.

---

**Try creating a transformation script with an intentional syntax error to see the validation in action, then fix it and watch the error disappear.**
