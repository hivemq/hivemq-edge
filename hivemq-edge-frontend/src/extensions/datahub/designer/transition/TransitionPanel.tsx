import type { FC } from 'react'
import { useCallback, useMemo } from 'react'
import type { Node } from '@xyflow/react'
import { getIncomers } from '@xyflow/react'
import { Card, CardBody } from '@chakra-ui/react'
import type { IChangeEvent } from '@rjsf/core'

import type {
  BehaviorPolicyData,
  FiniteStateMachineSchema,
  FsmState,
  PanelProps,
  StateType,
  TransitionData,
  TransitionType,
} from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { MOCK_TRANSITION_SCHEMA } from '@datahub/designer/transition/TransitionData.ts'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'
import { FiniteStateMachineFlow } from '@datahub/components/fsm/FiniteStateMachineFlow.tsx'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'

export const TransitionPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes, edges } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  // TODO[NVL] Error messages?
  const parentPolicy = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<TransitionData> | undefined
    if (!adapterNode) {
      return undefined
    }

    const incomers = getIncomers(adapterNode, nodes, edges)
    if (!incomers) {
      return undefined
    }

    // TODO[18935] The case of multiple sources need to be sorted out
    if (incomers.length !== 1) {
      return undefined
    }

    const [behavior] = incomers
    if (behavior.type !== DataHubNodeType.BEHAVIOR_POLICY) {
      return undefined
    }

    return behavior as Node<BehaviorPolicyData>
  }, [edges, nodes, selectedNode])

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<TransitionData> | undefined
    if (!adapterNode) {
      return null
    }

    const { event, from, to, type } = adapterNode.data
    const tempData: TransitionData = {
      ...adapterNode.data,
      model: parentPolicy ? parentPolicy.data.model : undefined,
      // @ts-ignore
      event: `${event || ''}-${from || ''}-${to || ''}-${type || ''}`,
    }
    return tempData
  }, [nodes, parentPolicy, selectedNode])

  const options = useMemo(() => {
    if (!parentPolicy) {
      return null
    }

    const { model } = parentPolicy.data
    const definition = MOCK_BEHAVIOR_POLICY_SCHEMA.schema.definitions?.[model]
    if (!definition) {
      return null
    }

    const { metadata } = definition as FiniteStateMachineSchema
    const { states, transitions } = metadata
    if (!states || !transitions) {
      return null
    }

    return metadata
  }, [parentPolicy])

  const onSafeFormSubmit = useCallback(
    (data: IChangeEvent<TransitionData>) => {
      const initData = data
      const { formData } = initData
      if (formData) {
        const { event: originalEvent } = formData
        if (originalEvent) {
          const [event, from, to, type] = originalEvent.split('-') as [
            TransitionType,
            StateType,
            StateType,
            FsmState.Type | undefined,
          ]
          initData.formData = {
            ...initData.formData,
            event,
            from,
            to,
            type: type,
          }
        }
      }
      onFormSubmit?.(initData)
    },
    [onFormSubmit]
  )

  return (
    <Card>
      {guardAlert && <ErrorMessage status="info" type={guardAlert.title} message={guardAlert.description} />}
      <CardBody>
        <ReactFlowSchemaForm
          isNodeEditable={isNodeEditable}
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
        {options && <FiniteStateMachineFlow transitions={options.transitions} states={options.states} />}
      </CardBody>
    </Card>
  )
}
