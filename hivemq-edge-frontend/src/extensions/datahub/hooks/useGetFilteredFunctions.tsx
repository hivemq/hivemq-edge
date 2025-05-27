import { useGetAllFunctions } from '@datahub/api/hooks/DataHubFunctionsService/useGetAllFunctions.ts'
import { useMemo } from 'react'

import type { BehaviorPolicyTransitionEvent } from '@/api/__generated__'
import { DataHubNodeType } from '@datahub/types.ts'
import { useGetAllFunctionSpecs } from '@datahub/api/hooks/DataHubFunctionsService/useGetAllFunctionSpecs.ts'

export const useGetFilteredFunction = (
  type: DataHubNodeType = DataHubNodeType.DATA_POLICY,
  transition?: BehaviorPolicyTransitionEvent
) => {
  const { isError, error, isLoading, isSuccess, data } = useGetAllFunctionSpecs()
  const { data: xxxxxx } = useGetAllFunctions()

  const filteredFunctions = useMemo(() => {
    console.log(
      'XXXX filteredFunctions 1',
      xxxxxx,
      data?.items?.map((e) => e.metadata)
    )

    if (!data || !data.items?.length) return []

    return data.items.filter((functionSpec) => {
      if (!functionSpec.metadata.inLicenseAllowed) return false
      if (
        functionSpec.metadata.supportedEvents?.length &&
        transition &&
        !functionSpec.metadata.supportedEvents.includes(transition)
      )
        return false
      if (functionSpec.metadata.isDataOnly && type !== DataHubNodeType.DATA_POLICY) return false

      // In all other cases, the function is valid
      return true
    })
  }, [data, transition, type])

  return { data: filteredFunctions, isError, error, isLoading, isSuccess }
}
