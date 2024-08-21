import { useMemo } from 'react'

import { ApiError } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'
import { useListClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useListClientSubscriptions.ts'

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
  useOrigin?: boolean
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
  const {
    data: clients,
    isLoading: isClientLoading,
    isSuccess: isClientSuccess,
    isError: isClientError,
    error: clientError,
  } = useListClientSubscriptions()

  const data = useMemo<string[]>(() => {
    const _options = { ...defaultOptions, ...options }

    return mergeAllTopics(adapterTypes, adapters, bridges, clients, options?.useOrigin)
      .reduce<string[]>(reduceTopicsBy(_options), [])
      .sort()
  }, [adapterTypes, adapters, bridges, options, clients])

  return {
    data: data,
    isSuccess: isAdapterTypeSuccess && isAdapterSuccess && isBridgeSuccess && isClientSuccess,
    isLoading: isBridgeLoading || isAdapterLoading || isAdapterTypesLoading || isClientLoading,
    isError: isBridgeError || isAdapterError || isAdapterTypesError || isClientError,
    error: adapterTypesError || adapterError || bridgeError || clientError,
  }
}
