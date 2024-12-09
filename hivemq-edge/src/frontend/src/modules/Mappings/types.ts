import { GenericObjectType, type RJSFSchema, type UiSchema } from '@rjsf/utils'
import { AlertProps } from '@chakra-ui/react'
import { ApiError } from '@/api/__generated__'

export interface ManagerContextType<T> {
  schema?: RJSFSchema
  formData?: T
  uiSchema?: UiSchema
}

export enum MappingType {
  NORTHBOUND = 'NORTHBOUND',
  SOUTHBOUND = 'SOUTHBOUND',
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export interface MappingManagerType<T = any, U = any> {
  context: ManagerContextType<U>
  data: T | undefined
  onUpdateCollection: (tags: T) => Promise<unknown> | undefined
  onClose: () => void
  isLoading: boolean
  isError: boolean
  isPending: boolean
  error: ApiError | null
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export interface DeprecatedMappingManagerType<T = any> {
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
