import { GenericObjectType, type RJSFSchema, type UiSchema } from '@rjsf/utils'
import { AlertProps } from '@chakra-ui/react'

export interface ManagerContextType {
  schema?: RJSFSchema
  formData?: GenericObjectType
  uiSchema?: UiSchema
}

export enum MappingType {
  INWARD = 'INWARD',
  OUTWARD = 'OUTWARD',
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export interface MappingManagerType<T = any> {
  schema: RJSFSchema
  formData?: GenericObjectType
  uiSchema: UiSchema
  onSubmit?: (data: T) => void
  onError?: (e: Error) => void
  errors?: string
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface OutwardMapping {
  mqttTopicFilter: string
  tag: string
  fieldMapping: FieldMapping[]
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface FieldMapping {
  source: FieldMappingDefinition
  destination: FieldMappingDefinition
  transformation?: FieldTransformation
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface FieldMappingDefinition {
  propertyPath: string
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface FieldTransformation {
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
