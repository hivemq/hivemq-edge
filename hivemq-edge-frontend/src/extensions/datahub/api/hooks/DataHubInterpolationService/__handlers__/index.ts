import type { HttpHandler } from 'msw'
import { http, HttpResponse } from 'msw'
import type { InterpolationVariableList } from '@/api/__generated__'
import { InterpolationVariable, PolicyType } from '@/api/__generated__'

export const MOCK_INTERPOLATION_VARIABLES: InterpolationVariableList = {
  items: [
    {
      variable: 'clientId',
      type: InterpolationVariable.type.STRING,
      description: 'The MQTT client ID',
      policyType: [PolicyType.DATA_POLICY, PolicyType.BEHAVIOR_POLICY],
    },
    {
      variable: 'topic',
      type: InterpolationVariable.type.STRING,
      description: 'The MQTT topic to which the MQTT message was published',
      policyType: [PolicyType.DATA_POLICY],
    },
    {
      description: 'The id of the policy that is currently executed',
      policyType: [PolicyType.DATA_POLICY, PolicyType.BEHAVIOR_POLICY],
      type: InterpolationVariable.type.STRING,
      variable: 'policyId',
    },
    {
      variable: 'validationResult',
      type: InterpolationVariable.type.STRING,
      description:
        'A textual description of the validation result. This text can contain schema validation errors for further debugging.',
      policyType: [PolicyType.DATA_POLICY],
    },
    {
      variable: 'fromState',
      type: InterpolationVariable.type.STRING,
      description: 'Textual representation of the state of the state machine before the transition.',
      policyType: [PolicyType.BEHAVIOR_POLICY],
    },
    {
      variable: 'toState',
      type: InterpolationVariable.type.STRING,
      description: 'Textual representation of the state to which the state machine transitions.',
      policyType: [PolicyType.BEHAVIOR_POLICY],
    },
    {
      variable: 'triggerEvent',
      type: InterpolationVariable.type.STRING,
      description: 'Textual representation of the event that triggered the state machine transition.',
      policyType: [PolicyType.BEHAVIOR_POLICY],
    },
    {
      variable: 'timestamp',
      type: InterpolationVariable.type.LONG,
      description: 'Current time in milliseconds since the UNIX epoch (Jan 1, 1970).',
      policyType: [PolicyType.DATA_POLICY, PolicyType.BEHAVIOR_POLICY],
    },
  ],
}

export const handlers: HttpHandler[] = [
  http.get('*/data-hub/interpolation-variables', () => {
    return HttpResponse.json<InterpolationVariableList>(MOCK_INTERPOLATION_VARIABLES, { status: 200 })
  }),
]
