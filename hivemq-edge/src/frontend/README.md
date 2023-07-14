# hivemq-edge frontend

HiveMQ Edge Edition

## Libraries

| Library     | Description                                                             | Link                                         |
| ----------- | ----------------------------------------------------------------------- | -------------------------------------------- |
| React       | Main UI Framework to build reactive frontend applications               | https://react.dev/                           |
| React Query | Data-fetching library for web applications                              | https://tanstack.com/query/latest/docs/react |
| Axios       | Use modern network handler to send, resend or intercept network request | https://github.com/axios/axios               |
| Chakra UI   | Simple, modular and accessible UI component library                     | https://chakra-ui.com/                       |
| React icons | Icons with ES6 imports                                                  | https://react-icons.github.io/react-icons/   |
| i18next     | Internationalisation framework                                          | https://react.i18next.com/                   |

## Development

Install the following dependencies to start the development of the project.

| Tool | Description                                           | Link                                                  |
| ---- | ----------------------------------------------------- | ----------------------------------------------------- |
| PNPM | Node package manager to install frontend dependencies | https://pnpm.io/installation                          |
| NVM  | Node.js version manager                               | https://github.com/nvm-sh/nvm#installing-and-updating |

Then you can install the node.js version of the project by running

```shell
nvm install
```

After that you need to install the `node_modules` of the project by running the following command:

```shell
pnpm install --frozen-lockfile
```

**That's it happy development** 🎉

### Run

Start the application locally by running

```shell
pnpm dev
```

The web app will be running on `http://localhost:5173/

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
They are :

```dotenv
VITE_API_BASE_URL="the base URL of the API server, ie http://localhost:8080"
```

### Deployment

To build a version ready for deployment, run the command

```shell
pnpm run build
```

## OpenAPI

The frontend is using the `OpenApi` specs to automatically generate a functional client for the code.
The `Node` library used for this purpose is [openapi-typescript-codegen](https://github.com/ferdikoomen/openapi-typescript-codegen)

The command used for the generation is saved in the `package` scripts:

```shell
pnpm run dev:openAPI
```

```shell
openapi --input <path>/hivemq-edge-openapi.yaml \
    -o ./src/api/__generated__ \
    -c axios  \
    --name HiveMqClient
```

with the following options:

- the `OpenAPI` spec is given as a local path, assuming deployment of the backend locally. It could also be a URL or even the string content.
  The current spec can be found in [hivemq/hivemq-edge-test](https://github.com/hivemq/hivemq-edge-test/blob/master/src/test/resources/hivemq-edge-openapi.yaml)
- all files are created in the `./src/api/__generated__` folder and are not expected to be modified manually (safe from `eslint` and `prettier`)
- `axios` is used for the HTTP client
- a custom client, to configure individual instances, is created as `HiveMqClient`

## Pipeline

See Github actions. Does it work now? XXXX

## Testing

### Cypress - E2E and Component testing

### Axe - Accessibility testing

### Percy - Visual testing More Test

https://percy.io/f896bbdc/hivemq-edge
