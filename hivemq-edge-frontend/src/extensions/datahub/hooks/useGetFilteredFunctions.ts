import { useMemo } from 'react'

import type { BehaviorPolicyTransitionEvent } from '@/api/__generated__'
import { DataHubNodeType } from '@datahub/types.ts'
import { useGetAllFunctionSpecs } from '@datahub/api/hooks/DataHubFunctionsService/useGetAllFunctionSpecs.ts'
import { filterFunctionSpecsByContext, MqttTransformFunction } from '@datahub/hooks/useFilteredFunctionsFetcher.ts'

/**
 * @deprecated Use the fetcher-based hook instead
 * @see useFilteredFunctionsFetcher
 * @param type
 * @param transition
 */
export const useGetFilteredFunction = (
  type: DataHubNodeType = DataHubNodeType.DATA_POLICY,
  transition?: BehaviorPolicyTransitionEvent
) => {
  const { isError, error, isLoading, isSuccess, data } = useGetAllFunctionSpecs()

  const filteredFunctions = useMemo(() => {
    if (!data || !data.items?.length) return []

    return data.items.filter(filterFunctionSpecsByContext(type, transition))
  }, [data, transition, type])

  return { data: [...filteredFunctions, MqttTransformFunction], isError, error, isLoading, isSuccess }
}
