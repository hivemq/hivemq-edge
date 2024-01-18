import { PanelSpecs, StateType, TransitionType } from '@/extensions/datahub/types.ts'

export const MOCK_TRANSITION_SCHEMA: PanelSpecs = {
  schema: {
    type: 'object',
    required: ['type'],
    description:
      'The HiveMQ Data Hub uses state machines to model the behavior of an MQTT client as it moves through your HiveMQ broker.',
    properties: {
      type: {
        title: 'Transition',
        description:
          'The movement of the MQTT client from one state to another. Each transition consists of a from state, a to state and a specific event.',
        enum: [
          TransitionType.ON_ANY,
          TransitionType.ON_DISCONNECT,
          TransitionType.ON_INBOUND_CONNECT,
          TransitionType.ON_INBOUND_DISCONNECT,
          TransitionType.ON_INBOUND_PUBLISH,
          TransitionType.ON_INBOUND_SUBSCRIBE,
        ],
        default: TransitionType.ON_ANY,
      },
      from: { $ref: '#/$defs/state', title: 'From State' },
      to: { $ref: '#/$defs/state', title: 'To State' },
    },
    $defs: {
      state: {
        title: 'State',
        description: 'Shows the current state of the MQTT client in the state machine',
        enum: [
          StateType.Any,
          StateType.Initial,
          StateType.Connected,
          StateType.Disconnected,
          StateType.Duplicated,
          StateType.NotDuplicated,
          StateType.Violated,
          StateType.Publishing,
        ],
        default: StateType.Any,
      },
    },
  },
}
