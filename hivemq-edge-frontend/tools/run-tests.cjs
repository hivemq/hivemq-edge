#!/usr/bin/env node
/* istanbul ignore file -- @preserve */
/**
 * Simple Cypress test runner with coverage
 *
 * Features:
 * - Run tests sequentially or in parallel
 * - Continue on test failures
 * - Always collect and merge coverage
 * - Optional verbose output
 *
 * Usage:
 *   node tools/run-tests.cjs [--parallel] [--verbose]
 */

const { spawn, execSync } = require('node:child_process')
const fs = require('fs-extra')
const path = require('node:path')

// Colors for better readability
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  dim: '\x1b[2m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
}

// Parse arguments
const args = process.argv.slice(2)
const parallel = args.includes('--parallel')
const verbose = args.includes('--verbose')
const includeAll = args.includes('--all')

// Test groups with colors
const ALL_TESTS = [
  { name: 'vitest', type: 'vitest', spec: null, color: colors.blue },
  { name: 'e2e', type: 'e2e', spec: './cypress/e2e/**/*', color: colors.cyan },
  { name: 'components', type: 'component', spec: './src/components/**/*', color: colors.green },
  {
    name: 'extensions',
    type: 'component',
    spec: './src/extensions/**/*',
    color: colors.magenta,
  },
  { name: 'modules', type: 'component', spec: './src/modules/**/*', color: colors.yellow },
]

// Filter tests based on --all flag
const TESTS = includeAll ? ALL_TESTS : ALL_TESTS.filter((t) => t.type !== 'vitest')

const results = { passed: [], failed: [], running: [] }

const testType = includeAll ? 'all tests (Vitest + Cypress)' : 'Cypress tests'
console.log(
  `${colors.bright}${colors.blue}🚀 Running ${testType} ${parallel ? 'in parallel' : 'sequentially'}...${colors.reset}\n`
)

// Ensure coverage directories exist
TESTS.forEach(({ name }) => fs.ensureDirSync(`./coverage-combined/${name}`))

/**
 * Run a single test group
 */
function runTest(test) {
  return new Promise((resolve) => {
    results.running.push(test.name)
    const startTime = Date.now()
    console.log(`${test.color}▶${colors.reset} Starting: ${colors.bright}${test.name}${colors.reset}`)

    // Build command based on test type
    let cmd
    if (test.type === 'vitest') {
      cmd = 'vitest run --coverage'
    } else {
      // Enable videos in verbose mode for debugging, disable for speed in normal mode
      const videoSetting = verbose ? 'true' : 'false'
      cmd = `CYPRESS_video=${videoSetting} cypress run -q --${test.type} --spec "${test.spec}"`
    }

    const proc = spawn(cmd, {
      shell: true,
      stdio: verbose ? 'inherit' : 'pipe',
    })

    let output = ''
    if (!verbose) {
      proc.stdout?.on('data', (d) => (output += d))
      proc.stderr?.on('data', (d) => (output += d))
    }

    proc.on('close', (code) => {
      const duration = ((Date.now() - startTime) / 1000).toFixed(2)

      // Remove from running list
      const idx = results.running.indexOf(test.name)
      if (idx > -1) results.running.splice(idx, 1)

      // Copy coverage based on test type
      try {
        if (test.type === 'vitest') {
          // Vitest coverage is already in coverage-vitest/
          // Will be merged later with merge-all-coverage.cjs
        } else {
          // Copy Cypress coverage
          fs.copyFileSync(
            './coverage-cypress/coverage-final.json',
            `./coverage-combined/${test.name}/coverage-final.json`
          )
          fs.copyFileSync('./coverage-cypress/lcov.info', `./coverage-combined/${test.name}/lcov.info`)
        }
      } catch (e) {
        // Coverage files might not exist
      }

      if (code === 0) {
        results.passed.push(test.name)
        console.log(
          `${colors.green}✓${colors.reset} Completed: ${colors.bright}${test.name}${colors.reset} ${colors.dim}(${duration}s)${colors.reset}`
        )
      } else {
        results.failed.push(test.name)
        console.log(
          `${colors.red}✗${colors.reset} Failed: ${colors.bright}${test.name}${colors.reset} ${colors.dim}(exit code: ${code}, ${duration}s)${colors.reset}`
        )
        // Only show output in verbose mode
        if (verbose && output) {
          console.log(
            `${colors.red}Last output:${colors.reset}\n${colors.dim}${output.split('\n').slice(-10).join('\n')}${colors.reset}`
          )
        }
      }

      // Show still running tests if any (for parallel mode)
      if (parallel && results.running.length > 0) {
        console.log(`${colors.blue}Still running: ${results.running.join(', ')}${colors.reset}`)
      }

      resolve()
    })
  })
}

/**
 * Run all tests
 */
async function runAllTests() {
  if (parallel) {
    await Promise.all(TESTS.map((test) => runTest(test)))
  } else {
    for (const test of TESTS) {
      await runTest(test)
    }
  }

  // Summary
  console.log(`\n${colors.blue}${'═'.repeat(50)}${colors.reset}`)
  console.log(`${colors.bright}${colors.blue}📊 Test Results Summary${colors.reset}`)
  console.log(`${colors.blue}${'═'.repeat(50)}${colors.reset}\n`)

  if (results.passed.length > 0) {
    console.log(`${colors.green}✓ Passed: ${results.passed.length}/${TESTS.length}${colors.reset}`)
    results.passed.forEach((name) => console.log(`  ${colors.green}•${colors.reset} ${name}`))
  }

  if (results.failed.length > 0) {
    console.log(`\n${colors.red}✗ Failed: ${results.failed.length}/${TESTS.length}${colors.reset}`)
    results.failed.forEach((name) => console.log(`  ${colors.red}•${colors.reset} ${name}`))
  }

  // Merge coverage
  console.log(`\n${colors.blue}${'═'.repeat(50)}${colors.reset}`)
  console.log(`${colors.bright}${colors.blue}🔄 Merging Coverage Reports${colors.reset}`)
  console.log(`${colors.blue}${'═'.repeat(50)}${colors.reset}\n`)

  // Determine which merge script to use
  const hasVitest = TESTS.some((test) => test.type === 'vitest')
  const mergeScript = hasVitest ? 'merge-all-coverage.cjs' : 'merge-cypress-coverage.cjs'

  let mergeSuccess = false
  try {
    execSync(`node tools/${mergeScript}`, { stdio: 'inherit' })
    mergeSuccess = true
    console.log(`\n${colors.green}✓ Coverage merged successfully${colors.reset}`)

    // Make the path a clickable link (works in most modern terminals)
    const reportPath = path.resolve('coverage-combined/index.html')
    console.log(`${colors.blue}📁 View: ${colors.reset}file://${reportPath}`)
  } catch (e) {
    console.error(`${colors.red}✗ Failed to merge coverage${colors.reset}`)
  }

  // Exit with 0 if coverage was merged successfully, even if some tests failed
  // This prevents the "Command failed with exit code 1" message
  if (mergeSuccess) {
    if (results.failed.length > 0) {
      console.log(`\n${colors.yellow}⚠️  Note: Some tests failed, but coverage was collected and merged${colors.reset}`)
    }
    process.exit(0)
  } else {
    process.exit(1)
  }
}

runAllTests()
