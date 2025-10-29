#!/usr/bin/env node
/* istanbul ignore file -- @preserve */
/**
 * Merge Cypress coverage reports from matrix test runs
 */

const { execSync } = require('node:child_process')
const fs = require('fs-extra')
const path = require('node:path')

const NYC_OUTPUT = '.nyc_output'
const TEMP_DIR = path.join(NYC_OUTPUT, 'temp')
const COMBINED_DIR = 'coverage-combined'

// Clean up
fs.emptyDirSync(NYC_OUTPUT)
fs.ensureDirSync(TEMP_DIR)

// Copy all coverage files
const sources = ['e2e', 'components', 'extensions', 'modules']
let found = 0

sources.forEach((name) => {
  const file = `./coverage-combined/${name}/coverage-final.json`
  if (fs.existsSync(file)) {
    fs.copyFileSync(file, `${TEMP_DIR}/${name}.json`)
    found++
  }
})

if (found === 0) {
  console.error('No coverage files found')
  process.exit(1)
}

console.log(`Merging ${found} coverage file(s)...`)

// Merge and generate reports
execSync(`npx nyc merge ${TEMP_DIR} ${NYC_OUTPUT}/coverage.json`, { stdio: 'inherit' })
execSync(
  `npx nyc report --reporter=lcov --reporter=json --reporter=html --report-dir=${COMBINED_DIR} --check-coverage=false`,
  {
    stdio: 'inherit',
  }
)

console.log('\n✓ Coverage merged')
