import { useMemo } from 'react'

import { ApiError } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.tsx'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.tsx'

import { mergeAllTopics } from '@/modules/EdgeVisualisation/utils/topics-utils.ts'

interface EdgeTopics {
  data: string[]
  isLoading: boolean
  isError: boolean
  error: ApiError | null
}

export interface EdgeTopicsOptions {
  publishOnly: boolean
}

export const publishOnlyFilter = (options: EdgeTopicsOptions) => (e: string) => {
  return !options.publishOnly || (options.publishOnly && !e.match(/[+#$]/gi))
}

export const useGetEdgeTopics = (options?: EdgeTopicsOptions): EdgeTopics => {
  const {
    // data: adapterTypes,
    isLoading: isAdapterTypesLoading,
    isError: isAdapterTypesError,
    error: adapterTypesError,
  } = useGetAdapterTypes()
  const {
    data: adapters,
    isLoading: isAdapterLoading,
    isError: isAdapterError,
    error: adapterError,
  } = useListProtocolAdapters()
  const { data: bridges, isLoading: isBridgeLoading, isError: isBridgeError, error: bridgeError } = useListBridges()

  const data = useMemo<string[]>(() => {
    const _options = { publishOnly: true, ...options }

    return mergeAllTopics(adapters, bridges).filter(publishOnlyFilter(_options)).sort()
  }, [adapters, bridges, options])

  return {
    data: data,
    isLoading: isBridgeLoading || isAdapterLoading || isAdapterTypesLoading,
    isError: isBridgeError || isAdapterError || isAdapterTypesError,
    error: adapterTypesError || adapterError || bridgeError,
  }
}
