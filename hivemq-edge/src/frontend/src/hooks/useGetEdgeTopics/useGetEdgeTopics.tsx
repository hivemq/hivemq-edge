import { useMemo } from 'react'

import { ApiError } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.tsx'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.tsx'

import { mergeAllTopics } from '@/modules/Workspace/utils/topics-utils.ts'

interface EdgeTopics {
  data: string[]
  isLoading: boolean
  isError: boolean
  isSuccess: boolean
  error: ApiError | null
}

export interface EdgeTopicsOptions {
  publishOnly?: boolean
  branchOnly?: boolean
}

const defaultOptions: EdgeTopicsOptions = { publishOnly: true, branchOnly: false }

export const reduceTopicsBy = (options: EdgeTopicsOptions) => (prev: string[], cur: string) => {
  if (options.publishOnly && cur.match(/[+#$]/gi)) return prev
  if (options.branchOnly) {
    const branch = cur.split('/')
    const res = branch.slice(0, -1).join('/')
    if (res.length) prev.push(res)
    return prev
  }
  prev.push(cur)
  return prev
}

export const useGetEdgeTopics = (options?: EdgeTopicsOptions): EdgeTopics => {
  const {
    data: adapterTypes,
    isLoading: isAdapterTypesLoading,
    isError: isAdapterTypesError,
    isSuccess: isAdapterTypeSuccess,
    error: adapterTypesError,
  } = useGetAdapterTypes()
  const {
    data: adapters,
    isLoading: isAdapterLoading,
    isSuccess: isAdapterSuccess,
    isError: isAdapterError,
    error: adapterError,
  } = useListProtocolAdapters()
  const {
    data: bridges,
    isLoading: isBridgeLoading,
    isSuccess: isBridgeSuccess,
    isError: isBridgeError,
    error: bridgeError,
  } = useListBridges()

  const data = useMemo<string[]>(() => {
    const _options = { ...defaultOptions, ...options }

    // return mergeAllTopics(adapters, bridges).filter(filterTopicsBy(_options)).sort()
    return mergeAllTopics(adapterTypes, adapters, bridges).reduce<string[]>(reduceTopicsBy(_options), []).sort()
  }, [adapterTypes, adapters, bridges, options])

  return {
    data: data,
    isSuccess: isAdapterTypeSuccess && isAdapterSuccess && isBridgeSuccess,
    isLoading: isBridgeLoading || isAdapterLoading || isAdapterTypesLoading,
    isError: isBridgeError || isAdapterError || isAdapterTypesError,
    error: adapterTypesError || adapterError || bridgeError,
  }
}
