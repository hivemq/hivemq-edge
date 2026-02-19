/**
 * Codemod: migrate cy.intercept() → cy.interceptApi()
 *
 * Uses the ESLint Node.js API to collect all suggestion fix operations produced
 * by the `local/no-bare-cy-intercept` rule, then applies them as text edits.
 *
 * The ESLint rule merges import injection and call replacement into a single
 * "mega-fix" covering [0, callEnd]. When a file has multiple violations, naively
 * applying all mega-fixes would corrupt the file. This script decomposes each
 * mega-fix back into (callStart, callEnd, newCallText) so that multiple fixes
 * per file can be applied independently, then injects the import once.
 *
 * Run:
 *   node --experimental-strip-types tools/migrateCyIntercept.ts [--dry-run]
 *
 * Options:
 *   --dry-run   Print a summary of what would change without writing any files
 *
 * @see {@link https://linear.app/hivemq/issue/EDG-73}
 */

import { readFileSync, writeFileSync } from 'node:fs'
import { relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'
// @ts-ignore — ESLint is a CJS module; types are available but import style varies
import { ESLint } from 'eslint'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')

const DRY_RUN = process.argv.includes('--dry-run')

// The import line the ESLint rule injects at the top of the file
const IMPORT_TEXT = "import { API_ROUTES } from '@cypr/support/__generated__/apiRoutes'\n"

// ─── Types ────────────────────────────────────────────────────────────────────

interface SuggestionFix {
  range: [number, number]
  text: string
}

interface Suggestion {
  messageId: string
  desc: string
  fix?: SuggestionFix
}

interface LintMessage {
  ruleId: string | null
  line: number
  column: number
  suggestions?: Suggestion[]
}

interface CallReplacement {
  callStart: number
  callEnd: number
  newCallText: string
}

interface DecomposedFix extends CallReplacement {
  needsImport: boolean
  /** Character offset in source where the import line should be inserted (= body[0].range[0]) */
  importInsertPos: number
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Converts a 1-indexed line + 0-indexed column to a character offset in source.
 * ESLint messages report the position of the reported AST node using these
 * conventions, giving us the exact start of the cy.intercept() call expression.
 */
function lineColToOffset(source: string, line: number, col: number): number {
  let offset = 0
  let currentLine = 1
  for (let i = 0; i < source.length; i++) {
    if (currentLine === line) return offset + col
    if (source[i] === '\n') {
      offset = i + 1
      currentLine++
    }
  }
  return offset + col
}

// ─── Fix decomposition ────────────────────────────────────────────────────────

/**
 * Decomposes an ESLint suggestion fix into its constituent parts.
 *
 * When the file lacks the API_ROUTES import, the rule merges two edits into a
 * single mega-fix:
 *   fix.range = [body[0].range[0], callEnd]
 *   fix.text  = IMPORT_TEXT + source[body[0].range[0]..callStart] + newCallText
 *
 * Note: fix.range[0] is body[0].range[0] (the first import/statement in the
 * file), NOT necessarily 0 — a leading `/// <reference>` comment sits before
 * body[0] and is excluded from the fix range.
 *
 * We use the ESLint message's line/column to compute callStart precisely.
 * A character-scan would fail because cy.intercept and cy.interceptApi share
 * the "cy.intercept" prefix, causing the scan to overshoot.
 */
function decomposeFix(fix: SuggestionFix, source: string, msgLine: number, msgColumn: number): DecomposedFix {
  if (fix.text.startsWith(IMPORT_TEXT)) {
    const importInsertPos = fix.range[0] // = body[0].range[0]
    const callStart = lineColToOffset(source, msgLine, msgColumn)
    // fix.text = IMPORT_TEXT + source[importInsertPos..callStart] + newCallText
    const newCallText = fix.text.slice(IMPORT_TEXT.length + (callStart - importInsertPos))
    return { needsImport: true, importInsertPos, callStart, callEnd: fix.range[1], newCallText }
  }

  // Simple call-only replacement (file already had the import when linted)
  return {
    needsImport: false,
    importInsertPos: 0,
    callStart: fix.range[0],
    callEnd: fix.range[1],
    newCallText: fix.text,
  }
}

// ─── Apply edits to source ────────────────────────────────────────────────────

/**
 * Applies call replacements and optional import injection to source.
 *
 * Replacements are applied in descending callStart order so character positions
 * earlier in the file remain valid as later edits proceed. The import is then
 * inserted at importInsertPos (= body[0].range[0]) — this is always before any
 * call site, so its offset is unaffected by the call replacements above.
 */
function applyEdits(
  source: string,
  replacements: CallReplacement[],
  addImport: boolean,
  importInsertPos: number
): string {
  const sorted = [...replacements].sort((a, b) => b.callStart - a.callStart)
  let result = source
  for (const r of sorted) {
    result = result.slice(0, r.callStart) + r.newCallText + result.slice(r.callEnd)
  }
  if (addImport) {
    result = result.slice(0, importInsertPos) + IMPORT_TEXT + result.slice(importInsertPos)
  }
  return result
}

// ─── Main ─────────────────────────────────────────────────────────────────────

const eslint = new ESLint({ cwd: ROOT, fix: false })
const patterns = ['src/**/*.spec.cy.{ts,tsx}', 'cypress/e2e/**/*.{ts,tsx}']

console.log(`\nMigrating cy.intercept() → cy.interceptApi()${DRY_RUN ? ' [DRY RUN]' : ''}\n`)

let totalFiles = 0
let modifiedFiles = 0
let appliedFixes = 0
let skippedViolations = 0
const noSuggestionFiles: string[] = []

for (const pattern of patterns) {
  const results: ESLint.LintResult[] = await eslint.lintFiles(pattern)

  for (const result of results) {
    const filePath = result.filePath
    const relPath = relative(ROOT, filePath)

    const messages = (result.messages as LintMessage[]).filter((m) => m.ruleId === 'local/no-bare-cy-intercept')
    if (messages.length === 0) continue

    totalFiles++

    const originalSource = readFileSync(filePath, 'utf-8')
    const alreadyHasImport = originalSource.includes(IMPORT_TEXT.trimEnd())

    const replacements: CallReplacement[] = []
    let needsImport = false
    let importInsertPos = 0
    let fileSkipped = 0
    let fixCount = 0

    for (const msg of messages) {
      const firstSuggestion = msg.suggestions?.[0]
      if (!firstSuggestion?.fix) {
        fileSkipped++
        skippedViolations++
        if (!noSuggestionFiles.includes(relPath)) noSuggestionFiles.push(relPath)
        continue
      }

      const decomposed = decomposeFix(firstSuggestion.fix, originalSource, msg.line, msg.column)
      replacements.push({
        callStart: decomposed.callStart,
        callEnd: decomposed.callEnd,
        newCallText: decomposed.newCallText,
      })
      if (decomposed.needsImport && !alreadyHasImport) {
        needsImport = true
        importInsertPos = decomposed.importInsertPos
      }
      fixCount++
    }

    if (replacements.length === 0) continue

    const newSource = applyEdits(originalSource, replacements, needsImport, importInsertPos)
    if (newSource === originalSource) continue

    modifiedFiles++
    appliedFixes += fixCount

    if (!DRY_RUN) {
      writeFileSync(filePath, newSource, 'utf-8')
    }

    const action = DRY_RUN ? 'Would patch' : 'Patched'
    const skippedNote = fileSkipped > 0 ? ` (${fileSkipped} skipped — no suggestion)` : ''
    console.log(`  ${action}: ${relPath}  [${fixCount} fix(es)${skippedNote}]`)
  }
}

// ─── Summary ──────────────────────────────────────────────────────────────────

console.log(`
─────────────────────────────────────────────────────────
Summary
─────────────────────────────────────────────────────────
  Files with violations   : ${totalFiles}
  Files ${DRY_RUN ? 'to be ' : ''}modified        : ${modifiedFiles}
  Fixes ${DRY_RUN ? 'to be ' : ''}applied         : ${appliedFixes}
  Violations skipped       : ${skippedViolations}  (no suggestion available)
─────────────────────────────────────────────────────────`)

if (noSuggestionFiles.length > 0) {
  console.log(`\nFiles with un-migratable violations (manual review needed):`)
  for (const f of noSuggestionFiles) console.log(`  ${f}`)
}

if (!DRY_RUN && modifiedFiles > 0) {
  console.log(`\nNext step: pnpm build:tsc  — check for TypeScript type clashes in migrated files`)
}
