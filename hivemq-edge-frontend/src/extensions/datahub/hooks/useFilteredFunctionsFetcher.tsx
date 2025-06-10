import { useCallback } from 'react'

import type { BehaviorPolicyTransitionEvent, FunctionSpecs } from '@/api/__generated__'
import { DataHubNodeType } from '@datahub/types.ts'
import { useGetAllFunctionSpecs } from '@datahub/api/hooks/DataHubFunctionsService/useGetAllFunctionSpecs.ts'

/**
 * This is a composite function that implements a combined serialiser, deserialiser and user-defined scripts
 * in a single Operation node
 * As such, it is only supporting data policies
 */
export const MqttTransformFunction: FunctionSpecs = {
  functionId: 'DataHub.transform',
  metadata: {
    inLicenseAllowed: true,
    isTerminal: false,
    isDataOnly: true,
    hasArguments: true,
  },
  schema: {
    title: 'Transformation',
    description:
      'The list of Javascript functions used in this transformation operation. Add them directly on the graph',
    properties: {
      transform: {
        type: 'array',
        title: 'Execution order',
        description: 'Change the order in which the transform functions will be executed',
        items: {
          type: 'string',
          title: 'Function name',
        },
      },
    },
  },
}

export const useFilteredFunctionsFetcher = () => {
  // Use the existing hook without parameters to get all possible data
  const { isError, error, isLoading, isSuccess, data } = useGetAllFunctionSpecs()

  // Return a function that filters the data based on provided parameters
  const getFilteredFunctions = useCallback(
    (type: DataHubNodeType = DataHubNodeType.DATA_POLICY, transition?: BehaviorPolicyTransitionEvent) => {
      if (!data || !data.items?.length || isLoading) return []

      const filteredFunctions = data.items.filter((functionSpec) => {
        // Check license allowance
        if (!functionSpec.metadata.inLicenseAllowed) return false

        // Check supported events if transition is provided
        if (
          functionSpec.metadata.supportedEvents?.length &&
          transition &&
          !functionSpec.metadata.supportedEvents.includes(transition)
        ) {
          return false
        }

        // Check if data-only functions should be included
        if (functionSpec.metadata.isDataOnly && type !== DataHubNodeType.DATA_POLICY) {
          return false
        }

        // Function passes all filters
        return true
      })
      return [...filteredFunctions, MqttTransformFunction]
    },
    [data, isLoading]
  )

  return {
    getFilteredFunctions,
    isLoading,
    isError,
    error,
    isSuccess,
  }
}
