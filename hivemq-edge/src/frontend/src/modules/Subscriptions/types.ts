import { GenericObjectType, type RJSFSchema, type UiSchema } from '@rjsf/utils'
import { AlertProps } from '@chakra-ui/react'

export interface SubscriptionManagerType {
  schema: RJSFSchema
  formData: GenericObjectType
  uiSchema: UiSchema
  onSubmit?: (data: unknown) => void
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface OutwardSubscription {
  node: string
  'mqtt-topic': string[]
  mapping: Mapping[]
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface Mapping {
  source: string[]
  destination: string
  transformation?: Transformation
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface Transformation {
  function: 'toString' | 'toInt' | 'join'
  params: string
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface DeviceTags {
  tag: string
  node?: string
  register?: { start: number; shift: number }
}

export interface MappingValidation extends Pick<AlertProps, 'status'> {
  errors: string[]
}
