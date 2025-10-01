import type { RJSFSchema } from '@rjsf/utils'
import type { Accept } from 'react-dropzone'
import validator from '@rjsf/validator-ajv8'
import type { JSONSchema7 } from 'json-schema'
import type { AlertStatus } from '@chakra-ui/react'

import i18n from '@/config/i18n.config.ts'

import { DataIdentifierReference } from '@/api/__generated__'
import type { SelectEntityType } from '@/components/MQTT/types'

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
  if (!data || !header) throw new Error(i18n.t('schema.validation.noDataUri'))

  const [scheme, mediaTypes] = header.split(DECODE_SCHEME_SEPARATOR)
  if (!mediaTypes) throw new Error(i18n.t('schema.validation.noScheme'))
  if (scheme !== DECODE_DATA) throw new Error(i18n.t('schema.validation.noSchemeData'))

  const options = mediaTypes.split(DECODE_MEDIA_TYPES_SEPARATOR)
  const testSchema = options.includes(MIMETYPE_JSON_SCHEMA) || options.includes(MIMETYPE_JSON)
  if (!testSchema) throw new Error(i18n.t('schema.validation.noJsonSchemaMimeType'))
  if (!options.includes(DECODE_BASE64)) throw new Error(i18n.t('schema.validation.noBase64MediaType'))

  try {
    const decoded = atob(data)
    const json: RJSFSchema = JSON.parse(decoded)

    // This will take care of some of the basic json error but not of a valid JSONSchema
    validator.ajv.compile(json)

    // TODO[NVL] We need to decide what we want to require on the schema
    const { properties } = json
    if (!properties) throw new Error(i18n.t('schema.validation.ajvNoProperties'))
    if (!Object.keys(properties).length) throw new Error(i18n.t('schema.validation.ajvEmptyProperties'))

    return { mimeType: MIMETYPE_JSON, options, body: json } as UriInfo
  } catch (error) {
    if (error instanceof SyntaxError) throw new Error(i18n.t('schema.validation.noBase64Data'))
    if (error instanceof DOMException) throw new Error(i18n.t('schema.validation.noJSON'))
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

export const validateSchemaFromDataURI = (
  topicFilterSchema: string | undefined,
  // TODO[NVL] This is a real hack; find a better way of handling extension of enums
  type: DataIdentifierReference.type | SelectEntityType.TOPIC = DataIdentifierReference.type.TOPIC_FILTER
): SchemaHandler => {
  if (!topicFilterSchema)
    return {
      status: 'warning',
      message: i18n.t('schema.status.missing', { context: type }),
    }
  try {
    const schema = decodeDataUriJsonSchema(topicFilterSchema)
    if (!schema?.body)
      return {
        error: 'no body from the base64 payload',
        status: 'error',
        message: i18n.t('schema.status.internalError', { context: type }),
      }
    return {
      schema: schema.body,
      status: 'success',
      message: i18n.t('schema.status.success', { context: type }),
    }
  } catch (e) {
    return {
      error: (e as Error).message,
      status: 'error',
      message: i18n.t('schema.status.invalid', { context: type }),
    }
  }
}
