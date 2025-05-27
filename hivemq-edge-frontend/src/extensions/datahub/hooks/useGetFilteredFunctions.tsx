import { useGetAllFunctions } from '@datahub/api/hooks/DataHubFunctionsService/useGetAllFunctions.ts'
import { useMemo } from 'react'

import { BehaviorPolicyTransitionEvent, type FunctionSpecs } from '@/api/__generated__'
import { DataHubNodeType } from '@datahub/types.ts'
import { useGetAllFunctionSpecs } from '@datahub/api/hooks/DataHubFunctionsService/useGetAllFunctionSpecs.ts'

export const MqttTransformFunction: FunctionSpecs = {
  functionId: 'DataHub.transform',
  metadata: {
    inLicenseAllowed: true,
    isTerminal: false,
    isDataOnly: false,
    hasArguments: true,
    supportedEvents: [
      BehaviorPolicyTransitionEvent.EVENT_ON_ANY,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_CONNECT,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
      BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_DISCONNECT,
      BehaviorPolicyTransitionEvent.CONNECTION_ON_DISCONNECT,
    ],
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

  return { data: [...filteredFunctions, MqttTransformFunction], isError, error, isLoading, isSuccess }
}
