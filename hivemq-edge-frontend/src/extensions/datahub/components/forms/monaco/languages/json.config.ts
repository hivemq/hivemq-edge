import debug from 'debug'
import type { MonacoInstance } from '../types'

const debugLogger = debug('DataHub:monaco')

/**
 * Common schema properties shared across all JSON Schema versions
 */
const COMMON_SCHEMA_PROPERTIES = {
  $schema: { type: 'string', format: 'uri' },
  $id: { type: 'string', format: 'uri-reference' },
  $ref: { type: 'string', format: 'uri-reference' },
  $comment: { type: 'string' },
  title: { type: 'string' },
  description: { type: 'string' },
  default: true,
  examples: { type: 'array' },
  // Type
  type: {
    anyOf: [
      {
        type: 'string',
        enum: ['null', 'boolean', 'object', 'array', 'number', 'string', 'integer'],
      },
      {
        type: 'array',
        items: {
          type: 'string',
          enum: ['null', 'boolean', 'object', 'array', 'number', 'string', 'integer'],
        },
      },
    ],
  },
  enum: { type: 'array' },
  const: true,
  // Numbers
  multipleOf: { type: 'number', exclusiveMinimum: 0 },
  maximum: { type: 'number' },
  exclusiveMaximum: { type: 'number' },
  minimum: { type: 'number' },
  exclusiveMinimum: { type: 'number' },
  // Strings
  maxLength: { type: 'integer', minimum: 0 },
  minLength: { type: 'integer', minimum: 0 },
  pattern: { type: 'string' },
  format: { type: 'string' },
  // Arrays
  items: { $ref: '#' },
  maxItems: { type: 'integer', minimum: 0 },
  minItems: { type: 'integer', minimum: 0 },
  uniqueItems: { type: 'boolean' },
  contains: { $ref: '#' },
  // Objects - THE KEY ONE for nested properties!
  properties: {
    type: 'object',
    additionalProperties: { $ref: '#' },
  },
  patternProperties: { type: 'object', additionalProperties: { $ref: '#' } },
  additionalProperties: { $ref: '#' },
  required: { type: 'array', items: { type: 'string' } },
  // Composition
  allOf: { type: 'array', items: { $ref: '#' } },
  anyOf: { type: 'array', items: { $ref: '#' } },
  oneOf: { type: 'array', items: { $ref: '#' } },
  not: { $ref: '#' },
}

/**
 * JSON Schema Draft-04 meta-schema
 */
const JSON_SCHEMA_DRAFT_04 = {
  $schema: 'http://json-schema.org/draft-04/schema#',
  id: 'http://json-schema.org/draft-04/schema#',
  type: 'object',
  properties: {
    ...COMMON_SCHEMA_PROPERTIES,
    id: { type: 'string', format: 'uri' },
    definitions: { type: 'object', additionalProperties: { $ref: '#' } },
    dependencies: { type: 'object' },
  },
}

/**
 * JSON Schema Draft-07 meta-schema
 */
const JSON_SCHEMA_DRAFT_07 = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  $id: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    ...COMMON_SCHEMA_PROPERTIES,
    $defs: { type: 'object', additionalProperties: { $ref: '#' } },
    definitions: { type: 'object', additionalProperties: { $ref: '#' } },
    readOnly: { type: 'boolean' },
    writeOnly: { type: 'boolean' },
    contentMediaType: { type: 'string' },
    contentEncoding: { type: 'string' },
    if: { $ref: '#' },
    then: { $ref: '#' },
    else: { $ref: '#' },
    propertyNames: { $ref: '#' },
    maxProperties: { type: 'integer', minimum: 0 },
    minProperties: { type: 'integer', minimum: 0 },
    dependencies: { type: 'object' },
  },
}

/**
 * JSON Schema Draft 2020-12 meta-schema
 */
const JSON_SCHEMA_2020_12 = {
  $schema: 'https://json-schema.org/draft/2020-12/schema',
  $id: 'https://json-schema.org/draft/2020-12/schema',
  type: 'object',
  properties: {
    ...COMMON_SCHEMA_PROPERTIES,
    $defs: { type: 'object', additionalProperties: { $ref: '#' } },
    deprecated: { type: 'boolean' },
    readOnly: { type: 'boolean' },
    writeOnly: { type: 'boolean' },
    // Draft 2020-12 specific
    prefixItems: { type: 'array', items: { $ref: '#' } },
    maxContains: { type: 'integer', minimum: 0 },
    minContains: { type: 'integer', minimum: 0 },
    propertyNames: { $ref: '#' },
    maxProperties: { type: 'integer', minimum: 0 },
    minProperties: { type: 'integer', minimum: 0 },
    dependentRequired: { type: 'object' },
    dependentSchemas: { type: 'object', additionalProperties: { $ref: '#' } },
    if: { $ref: '#' },
    then: { $ref: '#' },
    else: { $ref: '#' },
  },
}

/**
 * Configure JSON language with schema validation for multiple JSON Schema versions
 */
export const configureJSON = (monaco: MonacoInstance) => {
  debugLogger(`[json] Configuring JSON language with JSON Schema meta-schemas (Draft-04, Draft-07, 2020-12)...`)

  // Register multiple JSON Schema meta-schemas
  monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
    validate: true,
    allowComments: false,
    schemas: [
      {
        uri: 'http://json-schema.org/draft-04/schema#',
        fileMatch: ['*'],
        schema: JSON_SCHEMA_DRAFT_04,
      },
      {
        uri: 'http://json-schema.org/draft-07/schema#',
        fileMatch: ['*'],
        schema: JSON_SCHEMA_DRAFT_07,
      },
      {
        uri: 'https://json-schema.org/draft/2020-12/schema',
        fileMatch: ['*'],
        schema: JSON_SCHEMA_2020_12,
      },
    ],
    enableSchemaRequest: false,
    schemaValidation: 'warning',
  })

  // Configure IntelliSense behavior
  monaco.languages.json.jsonDefaults.setModeConfiguration({
    documentFormattingEdits: true,
    documentRangeFormattingEdits: true,
    completionItems: true,
    hovers: true,
    documentSymbols: true,
    tokens: true,
    colors: true,
    foldingRanges: true,
    diagnostics: true,
    selectionRanges: true,
  })

  debugLogger('[json] Configuration complete - 3 JSON Schema versions registered')
}
