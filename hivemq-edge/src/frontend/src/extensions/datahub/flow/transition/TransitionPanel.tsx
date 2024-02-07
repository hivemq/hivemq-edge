import { FC, useCallback, useMemo } from 'react'
import { getIncomers, Node } from 'reactflow'
import { Box, Card, CardBody } from '@chakra-ui/react'
import { IChangeEvent } from '@rjsf/core'

import {
  BehaviorPolicyData,
  DataHubNodeType,
  FiniteStateMachineSchema,
  PanelProps,
  StateType,
  TransitionData,
  TransitionType,
} from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_TRANSITION_SCHEMA } from '@datahub/flow/transition/TransitionData.ts'
import { datahubRJSFWidgets } from '@datahub/flow/datahubRJSFWidgets.tsx'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/flow/behavior_policy/BehaviorPolicySchema.ts'
import { FiniteStateMachineFlow } from '@datahub/components/fsm/FiniteStateMachineFlow.tsx'

export const TransitionPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes, edges } = useDataHubDraftStore()

  // TODO[NVL] Error messages?
  const parentPolicy = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<TransitionData> | undefined
    if (!adapterNode) return undefined
    const incomers = getIncomers(adapterNode, nodes, edges)
    if (!incomers) return undefined
    if (incomers.length !== 1) return undefined
    const [behavior] = incomers
    if (behavior.type !== DataHubNodeType.BEHAVIOR_POLICY) return undefined

    return behavior as Node<BehaviorPolicyData>
  }, [edges, nodes, selectedNode])

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<TransitionData> | undefined
    if (!adapterNode) return null

    const { event, from, to } = adapterNode.data
    const tempData: TransitionData = {
      ...adapterNode.data,
      model: parentPolicy ? parentPolicy.data.model : undefined,
      // @ts-ignore
      event: `${event || ''}-${from || ''}-${to || ''}`,
    }
    return tempData
  }, [nodes, parentPolicy, selectedNode])

  const options = useMemo(() => {
    if (!parentPolicy) return null
    const { model } = parentPolicy.data

    const definition = MOCK_BEHAVIOR_POLICY_SCHEMA.schema.definitions?.[model]
    if (!definition) return null

    const { metadata } = definition as FiniteStateMachineSchema
    const { states, transitions } = metadata
    if (!states || !transitions) return null

    return metadata
  }, [parentPolicy])

  const onSafeFormSubmit = useCallback(
    (data: IChangeEvent<TransitionData>) => {
      const initData = data
      const { formData } = initData
      if (formData) {
        const { event } = formData
        if (event) {
          const [e, from, to] = event.split('-')
          initData.formData = {
            ...initData.formData,
            event: e as unknown as TransitionType,
            from: from as unknown as StateType,
            to: to as unknown as StateType,
          }
        }
      }
      onFormSubmit?.(initData)
    },
    [onFormSubmit]
  )

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_TRANSITION_SCHEMA.schema}
          uiSchema={{
            ...MOCK_TRANSITION_SCHEMA.uiSchema,
            event: {
              ...MOCK_TRANSITION_SCHEMA.uiSchema?.event,
              'ui:options': {
                metadata: options,
              },
            },
          }}
          formData={data}
          widgets={datahubRJSFWidgets}
          onSubmit={onSafeFormSubmit}
        />
        {options && (
          <Box w="100%" height="400px">
            {/*<Mermaid text={template} />*/}
            <FiniteStateMachineFlow transitions={options.transitions} states={options.states} />
          </Box>
        )}
      </CardBody>
    </Card>
  )
}
