import { JsonNode } from '@/api/__generated__'
import mqtt, { MqttClient } from 'mqtt'

/* istanbul ignore next -- @preserve */
export enum MqttClientStatus {
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  RECONNECTING = 'RECONNECTING',
  CONNECTED = 'CONNECTED',
  CLIENT_ERROR = 'CLIENT_ERROR',
  SAMPLING = 'SAMPLING',
}

export interface MQTTSample {
  topic: string
  payload: JsonNode
}

export interface PrivateMqttClientType {
  state:
    | undefined
    | {
        client: MqttClient | null
        connectStatus: MqttClientStatus
        error: Error | mqtt.ErrorWithReasonCode | null
        samples: {
          topic: string
          payload: JsonNode
        }[]
      }

  actions:
    | undefined
    | {
        onSampling: (topicFilter: string) => Promise<MQTTSample[]> | undefined
      }
}
