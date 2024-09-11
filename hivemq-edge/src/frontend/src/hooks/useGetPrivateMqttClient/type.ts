import { JsonNode } from '@/api/__generated__'

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
