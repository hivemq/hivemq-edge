# hivemq-edge frontend

HiveMQ Edge Edition

## Libraries

| Library               | Description                                                                       | Link                                                |
| --------------------- | --------------------------------------------------------------------------------- | --------------------------------------------------- |
| React                 | Main UI Framework to build reactive frontend applications                         | https://react.dev/                                  |
| React Query           | Data-fetching library for web applications                                        | https://tanstack.com/query/latest/docs/react        |
| Axios                 | Use modern network handler to send, resend or intercept network request           | https://github.com/axios/axios                      |
| Chakra UI             | Simple, modular and accessible UI component library                               | https://chakra-ui.com/                              |
| React icons           | Icons with ES6 imports                                                            | https://react-icons.github.io/react-icons/          |
| i18next               | Internationalisation framework                                                    | https://react.i18next.com/                          |
| react-jsonschema-form | A React component for building Web forms from JSON Schema. <br/>Supports ChakraUI | https://github.com/rjsf-team/react-jsonschema-form/ |
| chakra-react-select   | A Chakra UI themed wrapper for the popular library React Select                   | https://github.com/csandman/chakra-react-select     |
| reactflow             | Highly customizable library for building interactive node-based UI                | https://reactflow.dev/                              |
| d3                    | The JavaScript library for bespoke data visualization                             | https://d3js.org/                                   |
| luxon                 | A powerful, modern, and friendly wrapper for JavaScript dates and times.          | https://moment.github.io/luxon/#/                   |

## Development

Install the following dependencies to start the development of the project.

| Tool | Description                                           | Link                                                  |
| ---- | ----------------------------------------------------- | ----------------------------------------------------- |
| PNPM | Node package manager to install frontend dependencies | https://pnpm.io/installation                          |
| NVM  | Node.js version manager                               | https://github.com/nvm-sh/nvm#installing-and-updating |

On a MacOS, you can simply use the following two commands

```shell
brew install pnpm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash
```

Then you can install the node.js version of the project by running

```shell
nvm install
```

After that you need to install the `node_modules` of the project by running the following command:

```shell
npm install -g pnpm@latest-8
pnpm install --frozen-lockfile
```

**That's it happy development** 🎉

### Configuration

The application uses the "dotenv" pattern for configuring environment variables, see https://vitejs.dev/guide/env-and-mode.html.
Additional environment variables are loaded from the following files:

```
.env                # loaded in all cases
.env.local          # loaded in all cases, ignored by git
.env.[mode]         # only loaded in specified mode
.env.[mode].local   # only loaded in specified mode, ignored by git
```

The `.env` file contains the main variables safe to be committed in git.
A `env.example` file has been created with all the values expected to be created  
either in a `.env.local` or `.env.production.local`, both of them excluded from git.

Add the following keys into your `.env.local` :

```dotenv
VITE_API_BASE_URL="the base URL of the API server, ie http://localhost:8080"
```

### Run

Start the application locally by running

```shell
pnpm dev
```

The web app will be running on http://localhost:3000/app/login

### Deployment

To build a version ready for deployment, run the command

```shell
# Relative path for production.
pnpm run build
# Absolute path for dev / test.
pnpm run build --base=/
```

## OpenAPI

The frontend is using the `OpenApi` specs to automatically generate a functional client for the code.
The `Node` library used for this purpose is [openapi-typescript-codegen](https://github.com/ferdikoomen/openapi-typescript-codegen)

The command used for the generation is saved in the `package` scripts:

```shell
pnpm run dev:openAPI
```

```shell
openapi --input '../../../../hivemq-edge/ext/hivemq-edge-openapi-2023.7.yaml' \
    -o ./src/api/__generated__ \
    -c axios \
    --name HiveMqClient \
    --exportSchemas true
```

with the following options:

- the `OpenAPI` spec is given as a local path, assuming deployment of the backend locally. It could also be a URL or even the string content.
  The current spec can be found in `/hivemq-edge/ext/hivemq-edge-openapi-2023.7.yaml`)
- all files are created in the `./src/api/__generated__` folder and are not expected to be modified manually (safe from `eslint` and `prettier`)
- `axios` is used for the HTTP client
- a custom client, to configure individual instances, is created as `HiveMqClient`

## Pipeline

Two frontend-specific workflows have been added to the repository

- `.github/workflows/frontend-cli.yml` for the main CI/CD pipeline
- `.github/workflows/frontend-visual.yml` for visual testing (Percy)

Together. they contain — and enforce — parts of the testing pyramid for frontend applications:

- Code quality checks (ESLint, Prettier)
- Interaction testing (Cypress - Component)
- Scenario testing (Cypress - E2E)
- Visual testing (Percy - integrated within Cypress)
- Unit testing (Vitest)

## Testing

### Cypress - E2E and Component testing

### Axe - Accessibility testing

### Percy - Visual testing

See github actions

### Lighthouse - Performance testing

### SonarQube Cloud

## Code Coverage

This project uses a comprehensive coverage setup that merges reports from multiple test sources:

- **Vitest** for unit tests
- **Cypress** for E2E and component tests (with matrix execution)

### Quick Commands

```bash
# Run all tests and get combined coverage
pnpm run coverage:report:all

# Run only Cypress matrix tests and merge
pnpm run cypress:coverage:report

# Merge existing coverage without re-running tests
pnpm run coverage:merge:cypress     # Cypress only
pnpm run coverage:merge:all         # Vitest + Cypress

# Test the coverage merge setup
./tools/test-coverage-merge.sh
```

### Coverage Reports Location

- **Individual Reports**:

  - `coverage-vitest/` - Vitest unit tests
  - `coverage-cypress/` - Cypress main output
  - `coverage-combined/e2e/` - E2E matrix tests
  - `coverage-combined/components/` - Component matrix tests
  - `coverage-combined/extensions/` - Extension matrix tests
  - `coverage-combined/modules/` - Module matrix tests

- **Merged Reports**:
  - `coverage-combined/index.html` - Combined HTML report
  - `coverage-combined/lcov.info` - Combined LCOV report
  - `coverage-combined/coverage-final.json` - Combined JSON report

📚 **For detailed coverage documentation**, see [COVERAGE-SETUP.md](./COVERAGE-SETUP.md)

Just some random change
