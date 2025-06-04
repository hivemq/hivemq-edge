import type { UseFormReturn, FieldValues } from 'react-hook-form'
import type { IdSchema } from '@rjsf/utils'
import type { Adapter, ProtocolAdapter } from '@/api/__generated__'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import type { Dispatch, SetStateAction } from 'react'

export enum WorkspaceAdapterCommand {
  VIEW = 'VIEW',
  TAGS = 'TAGS',
  TOPIC_FILTERS = 'TOPIC_FILTERS',
  MAPPINGS = 'MAPPINGS',
}

export type SubscriptionType = 'remoteSubscriptions' | 'localSubscriptions'

export interface GenericPanelType<T extends FieldValues> {
  isNew?: boolean
  form: UseFormReturn<T>
}

export interface AdapterType {
  adapterType?: string
}

export interface ProtocolFacetType {
  search?: string | null
  filter?: { key: keyof ProtocolAdapter; value: string } | null
}

export enum ProtocolAdapterTabIndex {
  PROTOCOLS,
  ADAPTERS,
}

export interface AdapterNavigateState {
  protocolAdapterTabIndex: ProtocolAdapterTabIndex
  protocolAdapterType?: string
  selectedActiveAdapter?: {
    isNew: boolean
    isOpen: boolean
    adapterId: string
  }
}

export type AdapterConfig = NonNullable<Adapter['config']>

export interface AdapterContext {
  // TODO[NVL] Is that good enough for ANY form data?
  onBatchUpload?: (idSchema: IdSchema<unknown>, batch: Record<string, unknown>[]) => void
  isEditAdapter: boolean
  isDiscoverable: boolean
  adapterType?: string
  adapterId?: string
}

export interface MappingContext extends AdapterContext {
  validationSchemas: [FlatJSONSchema7[] | undefined, Dispatch<SetStateAction<FlatJSONSchema7[] | undefined>>]
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace ExportFormat {
  export enum Type {
    CONFIGURATION = 'CONFIGURATION',
    MAPPINGS = 'MAPPINGS',
  }
}

export interface ExportFormat {
  value: ExportFormat.Type
  formats?: string[]
  isDisabled?: (protocol?: ProtocolAdapter) => boolean
  downloader?: (name: string, ext: string, source: Adapter, protocol: ProtocolAdapter, callback?: () => void) => void
}

export interface ExportFormatDisplay extends ExportFormat {
  label: string
  description: string
}

export class AdapterExportError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'AdapterExportError'
  }
}
