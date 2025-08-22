# Edge OpenAPI Definition Starter

## Working on the OpenAPI specs

This project has been migrated to a [Redocly starter](https://redocly.com/docs/cli/openapi-starter), which provides a set of tools to help you work with OpenAPI definitions.
It is recommended to use `VSCode` with the [Redocly VS Code extension](https://redocly.com/docs/vscode) for the best experience.

### Install

1. Install [Node JS](https://nodejs.org/) and [pnpm](https://pnpm.io/installation) (or use `npm`).
    - If you are using `npm`, you can run `npm install -g pnpm` to install `pnpm`.
2. Run `pnpm install` in the repo root.

### Usage

The `.redocly.yaml` controls settings for various tools including the lint tool and the reference
docs engine.  Open it to find examples and [read the docs](https://redocly.com/docs/cli/configuration/) for more information.

#### `pnpm dev:start`
Starts the reference docs preview server.

#### `pnpm dev:build`
Bundles the definition to the dist folder.

#### `pnpm dev:test`
Validates the definition.

#### `pnpm prod:release`
Bundles the definition to the `./ext` folder of the Hive Edge repository. Ensure that the name of the output file matches the release version

#### Other commands

#### `redocly split  <source to file>/openAPI.yaml --outDir <destination folder>/`


## Contribution Guide

Below is a sample contribution guide.

The tools in the repository don't restrict you to any specific structure. Adjust the contribution guide to match your own structure.

### Structure

The main document has been split from the original source in order to reduce the size of a single document and to highlight individual points of concern.

The original split was done automatically (using `redocly`) and the files reflect the automation.

```
openAPI             
├── README.md
├── openapi.yaml        // root document of the OpenAPI specs
├── paths               // list of path files, organised by their route
│   ├── api_v1_auth_authenticate.yaml
│   ├── api_v1_auth_refresh-token.yaml
│   └── ...
│
└── components          // list of reusable definitions, grouped by type
    ├── headers
    │   ├── ETag.yaml
    │   └── ...
    ├── parameters
    │   ├── CombinerId.yaml
    │   └── ...
    ├── schemas
    │   ├── Adapter.yaml
    │   └── ...
    └── ...
```

There is no reason to maintain such naming convention as we manually add or edit the source.

### Schemas

#### Adding Schemas

1. Navigate to the `openapi/components/schemas` folder.
2. Add a file named as you wish to name the schema.
3. Define the schema.
4. Refer to the schema using the `$ref` (see example below).

##### Example Schema
This is a very simple schema example:
```yaml
type: string
description: The resource ID. Defaults to UUID v4
maxLength: 50
example: 4f6cf35x-2c4y-483z-a0a9-158621f77a21
```
This is a more complex schema example:
```yaml
type: object
properties:
    id:
        description: The customer identifier string
        readOnly: true
        allOf:
            - $ref: ./ResourceId.yaml
    websiteId:
        description: The website's ID
        allOf:
            - $ref: ./ResourceId.yaml
    paymentToken:
        type: string
        writeOnly: true
        description: |
            A write-only payment token; if supplied, it will be converted into a
            payment instrument and be set as the `defaultPaymentInstrument`. The
            value of this property will override the `defaultPaymentInstrument`
            in the case that both are supplied. The token may only be used once
            before it is expired.
    defaultPaymentInstrument:
        $ref: ./PaymentInstrument.yaml
    createdTime:
        description: The customer created time
        allOf:
            - $ref: ./ServerTimestamp.yaml
    updatedTime:
        description: The customer updated time
        allOf:
            - $ref: ./ServerTimestamp.yaml
    tags:
        description: A list of customer's tags
        readOnly: true
        type: array
        items:
            $ref: ./Tags/Tag.yaml
    revision:
        description: >
            The number of times the customer data has been modified.

            The revision is useful when analyzing webhook data to determine if the
            change takes precedence over the current representation.
        type: integer
        readOnly: true
    _links:
        type: array
        description: The links related to resource
        readOnly: true
        minItems: 3
        items:
            anyOf:
                - $ref: ./Links/SelfLink.yaml
                - $ref: ./Links/NotesLink.yaml
                - $ref: ./Links/DefaultPaymentInstrumentLink.yaml
                - $ref: ./Links/LeadSourceLink.yaml
                - $ref: ./Links/WebsiteLink.yaml
    _embedded:
        type: array
        description: >-
            Any embedded objects available that are requested by the `expand`
            querystring parameter.
        readOnly: true
        minItems: 1
        items:
            anyOf:
                - $ref: ./Embeds/LeadSourceEmbed.yaml

```

If you have an JSON example, you can convert it to JSON schema using Redocly's [JSON to JSON schema tool](https://redocly.com/tools/json-to-json-schema/).

##### Using the `$ref`

Notice in the complex example above the schema definition itself has `$ref` links to other schemas defined.

Here is a small excerpt with an example:

```yaml
defaultPaymentInstrument:
    $ref: ./PaymentInstrument.yaml
```

The value of the `$ref` is the path to the other schema definition.

You may use `$ref`s to compose schema from other existing schema to avoid duplication.

You will use `$ref`s to reference schema from your path definitions.

#### Editing Schemas

1. Navigate to the `openapi/components/schemas` folder.
2. Open the file you wish to edit.
3. Edit.

### Paths

#### Adding a Path

1. Navigate to the `openapi/paths` folder.
2. Add a new YAML file named like your URL endpoint except replacing `/` with `_` (or whichever character you prefer) and putting path parameters into curly braces like `{example}`.
3. Add the path and a ref to it inside of your `openapi.yaml` file inside of the `openapi` folder.

Example addition to the `openapi.yaml` file:
```yaml
'/customers/{id}':
    $ref: './paths/customers_{id}.yaml'
```

Here is an example of a YAML file named `customers_{id}.yaml` in the `paths` folder:

```yaml
get:
    tags:
        - Customers
    summary: Retrieve a list of customers
    operationId: GetCustomerCollection
    description: |
        You can have a markdown description here.
    parameters:
        - $ref: ../components/parameters/collectionLimit.yaml
        - $ref: ../components/parameters/collectionOffset.yaml
        - $ref: ../components/parameters/collectionFilter.yaml
        - $ref: ../components/parameters/collectionQuery.yaml
        - $ref: ../components/parameters/collectionExpand.yaml
        - $ref: ../components/parameters/collectionFields.yaml
    responses:
        '200':
            description: A list of Customers was retrieved successfully
            headers:
                Rate-Limit-Limit:
                    $ref: ../components/headers/Rate-Limit-Limit.yaml
                Rate-Limit-Remaining:
                    $ref: ../components/headers/Rate-Limit-Remaining.yaml
                Rate-Limit-Reset:
                    $ref: ../components/headers/Rate-Limit-Reset.yaml
                Pagination-Total:
                    $ref: ../components/headers/Pagination-Total.yaml
                Pagination-Limit:
                    $ref: ../components/headers/Pagination-Limit.yaml
                Pagination-Offset:
                    $ref: ../components/headers/Pagination-Offset.yaml
            content:
                application/json:
                    schema:
                        type: array
                        items:
                            $ref: ../components/schemas/Customer.yaml
                text/csv:
                    schema:
                        type: array
                        items:
                            $ref: ../components/schemas/Customer.yaml
        '401':
            $ref: ../components/responses/AccessForbidden.yaml
    x-code-samples:
        - lang: PHP
          source:
              $ref: ../code_samples/PHP/customers/get.php
post:
    tags:
        - Customers
    summary: Create a customer (without an ID)
    operationId: PostCustomer
    description: Another markdown description here.
    requestBody:
        $ref: ../components/requestBodies/Customer.yaml
    responses:
        '201':
            $ref: ../components/responses/Customer.yaml
        '401':
            $ref: ../components/responses/AccessForbidden.yaml
        '409':
            $ref: ../components/responses/Conflict.yaml
        '422':
            $ref: ../components/responses/InvalidDataError.yaml
    x-code-samples:
        - lang: PHP
          source:
              $ref: ../code_samples/PHP/customers/post.php
```

You'll see extensive usage of `$ref`s in this example to different types of components including schemas.

You'll also notice `$ref`s to code samples.

### Code samples

Automated code sample generations is enabled in the Redocly configuration file. Add manual code samples by the following process:

1. Navigate to the `openapi/code_samples` folder.
2. Navigate to the `<language>` (e.g. PHP) sub-folder.
3. Navigate to the `path` folder, and add ref to the code sample.

You can add languages by adding new folders at the appropriate path level.

More details inside the `code_samples` folder README.
