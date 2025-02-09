/* istanbul ignore file -- @preserve */
/**
 * This script merges the coverage reports from Cypress and Jest into a single one,
 * inside the "coverage" folder
 */

// eslint-disable-next-line @typescript-eslint/no-var-requires,no-undef
const { execSync } = require('child_process')
// eslint-disable-next-line @typescript-eslint/no-var-requires,no-undef
const fs = require('fs-extra')

const REPORTS_FOLDER = 'reports'
const FINAL_OUTPUT_FOLDER = 'combined-coverage'

// eslint-disable-next-line @typescript-eslint/no-var-requires,no-undef
const { program } = require('commander')

program
  .option('-e --e2e-cov-dir <dir>', 'Directory for e2e coverage', 'e2e/coverage')
  .option('-c --ct-cov-dir <dir>', 'Directory for cypress-ct coverage', 'coverage')
  .option('-u --unit-cov-dir <dir>', 'Directory for unit test coverage', 'dist/coverage')

program.parse()
const options = program.opts()

console.log('Running merge with options:', options)

const run = (commands) => {
  commands.forEach((command) => execSync(command, { stdio: 'inherit' }))
}

// Create the reports folder and move the reports from cypress and jest inside it
fs.emptyDirSync(REPORTS_FOLDER)
fs.copyFileSync(options.ctCovDir + '/coverage-final.json', `${REPORTS_FOLDER}/from-cypress-ct.json`)
fs.copyFileSync(options.unitCovDir + '/coverage-final.json', `${REPORTS_FOLDER}/from-jest.json`)

fs.emptyDirSync('.nyc_output')
fs.emptyDirSync(FINAL_OUTPUT_FOLDER)

// Run "nyc merge" inside the reports folder, merging the two coverage files into one,
// then generate the final report on the coverage folder
run([
  // "nyc merge" will create a "coverage.json" file on the root, we move it to .nyc_output
  `npx nyc merge ${REPORTS_FOLDER} && mv coverage.json .nyc_output/out.json`,
  `npx nyc report --reporter lcov --report-dir ${FINAL_OUTPUT_FOLDER}`,
])
