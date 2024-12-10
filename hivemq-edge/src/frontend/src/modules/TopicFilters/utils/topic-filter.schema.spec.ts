import { describe, expect } from 'vitest'
import {
  decodeDataUriJsonSchema,
  encodeDataUriJsonSchema,
  SchemaHandler,
  validateSchemaFromDataURI,
} from '@/modules/TopicFilters/utils/topic-filter.schema.ts'
import { MOCK_TOPIC_FILTER_SCHEMA_VALID } from '@/api/hooks/useTopicFilters/__handlers__'

interface Suite {
  data: string
  error: string
}

describe('decodeDataUriJsonSchema', () => {
  const tests: Suite[] = [
    { data: '', error: 'Not a valid data-url encoded JSONSchema' },
    { data: '123', error: 'Not a valid data-url encoded JSONSchema' },
    { data: '123,456', error: 'No scheme defined in the URI' },
    { data: '123:test,456', error: "The scheme of the uri is not defined as 'data'" },
    { data: 'data:test,456', error: "The media types doesn't include the mandatory `application/schema+json`" },
    { data: 'data:application/json,456', error: "The media types doesn't include the mandatory `base64`" },
    { data: 'data:application/json;base64,456', error: 'The data is not properly encoded as a `base64` string' },
  ]

  it.each<Suite>(tests)('$data should throw $error', ({ data, error }) => {
    expect(() => decodeDataUriJsonSchema(data)).toThrowError(error)
  })

  it('should return a valid json object', () => {
    expect(decodeDataUriJsonSchema(MOCK_TOPIC_FILTER_SCHEMA_VALID)).toBeFalsy
  })
})

describe('encodeDataUriJsonSchema', () => {
  it('should return a data-url', () => {
    const schema = { test: 1 }
    expect(encodeDataUriJsonSchema(schema)).toStrictEqual<string>(
      `data:application/json;base64,${btoa(JSON.stringify(schema))}`
    )
  })
})

describe('validateSchemaFromDataURI', () => {
  it('should return a warning if not assigned', () => {
    expect(validateSchemaFromDataURI(undefined)).toStrictEqual<SchemaHandler>({
      status: 'warning',
      message: expect.stringContaining(''),
    })
  })

  it('should return a schema', () => {
    expect(validateSchemaFromDataURI(MOCK_TOPIC_FILTER_SCHEMA_VALID)).toStrictEqual<SchemaHandler>({
      message: expect.stringContaining(''),
      status: 'success',
      schema: expect.objectContaining({}),
    })
  })
})
