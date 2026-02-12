import type { GenericObjectType } from '@rjsf/utils'
import { type RJSFSchema, type UiSchema } from '@rjsf/utils'
import type { AlertProps } from '@chakra-ui/react'
import type {
  ApiError,
  DataIdentifierReference,
  DomainTagList,
  EntityReference,
  TopicFilterList,
} from '@/api/__generated__'
import type { UseQueryResult } from '@tanstack/react-query'

export interface ManagerContextType<T> {
  schema?: RJSFSchema
  formData?: T
  uiSchema?: UiSchema
}

export enum MappingType {
  NORTHBOUND = 'NORTHBOUND',
  SOUTHBOUND = 'SOUTHBOUND',
  COMBINING = 'COMBINING',
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

/**
 * Explicit pairing of an entity with its data query.
 * Replaces implicit index-based relationship between parallel arrays.
 */
export interface EntityQuery {
  /** The entity (adapter, bridge, broker) */
  entity: EntityReference
  /** The query for this entity's integration points (tags or topic filters) */
  query: UseQueryResult<DomainTagList | TopicFilterList, Error>
}

/**
 * Selected sources with full ownership information.
 * Maintained in frontend context to support UX flow.
 */
export interface SelectedSources {
  /** Tags with scope (adapter ID) */
  tags: DataIdentifierReference[]
  /** Topic filters (scope always null) */
  topicFilters: DataIdentifierReference[]
}

/**
 * Context passed to combiner form components via RJSF formContext
 */
export interface CombinerContext {
  /**
   * Explicit entity-query pairings (replaces parallel entities/queries arrays).
   * Optional during migration period for backward compatibility.
   */
  entityQueries?: EntityQuery[]

  /**
   * Selected sources with full ownership information.
   * Tracks which tags/topicFilters are selected with their scope.
   * This is the source of truth for ownership in the frontend.
   */
  selectedSources?: SelectedSources

  /**
   * @deprecated Use entityQueries instead. Kept for backward compatibility during migration.
   */
  queries?: UseQueryResult<DomainTagList | TopicFilterList, Error>[]

  /**
   * @deprecated Use entityQueries instead. Kept for backward compatibility during migration.
   */
  entities?: EntityReference[]
}
