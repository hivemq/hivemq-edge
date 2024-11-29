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

export interface MappingValidation extends Pick<AlertProps, 'status'> {
  errors: string[]
}
