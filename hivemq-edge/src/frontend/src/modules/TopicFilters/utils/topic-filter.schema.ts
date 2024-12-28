// data:content/type;base64,
import { RJSFSchema } from '@rjsf/utils'
import { Accept } from 'react-dropzone'
import validator from '@rjsf/validator-ajv8'

import i18n from '@/config/i18n.config.ts'
import type { JSONSchema7 } from 'json-schema'
import type { AlertStatus } from '@chakra-ui/react'

export const MIMETYPE_JSON = 'application/json'
export const MIMETYPE_JSON_SCHEMA = 'application/schema+json'
export const ACCEPT_JSON_SCHEMA: Accept = {
  [MIMETYPE_JSON_SCHEMA]: ['.json'],
}
const DECODE_HEADER_SEPARATOR = ','
const DECODE_SCHEME_SEPARATOR = ':'
const DECODE_MEDIA_TYPES_SEPARATOR = ';'
const DECODE_DATA = 'data'
const DECODE_BASE64 = 'base64'

export interface UriInfo {
  mimeType: string
  options?: string[]
  body: RJSFSchema
}

export const decodeDataUriJsonSchema = (dataUrl: string) => {
  const [header, data] = dataUrl.split(DECODE_HEADER_SEPARATOR)
  if (!data || !header) throw new Error(i18n.t('topicFilter.error.schema.noDataUri'))

  const [scheme, mediaTypes] = header.split(DECODE_SCHEME_SEPARATOR)
  if (!mediaTypes) throw new Error(i18n.t('topicFilter.error.schema.noScheme'))
  if (scheme !== DECODE_DATA) throw new Error(i18n.t('topicFilter.error.schema.noSchemeData'))

  const options = mediaTypes.split(DECODE_MEDIA_TYPES_SEPARATOR)
  if (!options.includes(MIMETYPE_JSON)) throw new Error(i18n.t('topicFilter.error.schema.noJsonSchemaMimeType'))
  if (!options.includes(DECODE_BASE64)) throw new Error(i18n.t('topicFilter.error.schema.noBase64MediaType'))

  try {
    const decoded = atob(data)
    const json: RJSFSchema = JSON.parse(decoded)

    // This will take care of some of the basic json error but not of a valid JSONSchema
    validator.ajv.compile(json)

    // TODO[NVL] We need to decide what we want to require on the schema
    const { properties } = json
    if (!properties) throw new Error(i18n.t('topicFilter.error.schema.ajvNoProperties'))

    return { mimeType: MIMETYPE_JSON, options, body: json } as UriInfo
  } catch (error) {
    if (error instanceof SyntaxError) throw new Error(i18n.t('topicFilter.error.schema.noBase64Data'))
    if (error instanceof DOMException) throw new Error(i18n.t('topicFilter.error.schema.noJSON'))
    if (error instanceof Error) {
      throw new Error(`${error.message}`)
    }
  }
}

export const encodeDataUriJsonSchema = (schema: RJSFSchema) => {
  return `data:${MIMETYPE_JSON};base64,${btoa(JSON.stringify(schema))}`
}

export interface SchemaHandler {
  schema?: JSONSchema7
  error?: string
  status: AlertStatus
  message: string
}

export const validateSchemaFromDataURI = (topicFilterSchema: string | undefined): SchemaHandler => {
  if (!topicFilterSchema)
    return {
      status: 'warning',
      message: i18n.t('topicFilter.schema.status.missing'),
    }
  try {
    const schema = decodeDataUriJsonSchema(topicFilterSchema)
    if (!schema?.body)
      return {
        error: 'no body from the base64 payload',
        status: 'error',
        message: i18n.t('topicFilter.schema.status.internalError'),
      }
    return {
      schema: schema.body,
      status: 'success',
      message: i18n.t('topicFilter.schema.status.success'),
    }
  } catch (e) {
    return {
      error: (e as Error).message,
      status: 'error',
      message: i18n.t('topicFilter.schema.status.invalid'),
    }
  }
}
