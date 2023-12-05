import { DataPoint, Metric } from '@/api/__generated__'
import { mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import { DateTime } from 'luxon'

export const MOCK_METRICS: Array<Metric> = [
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.forward.publish.count` },
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.forward.publish.excluded.count` },
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.forward.publish.failed.count` },
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.forward.publish.loop-hops-exceeded.count` },
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.local.publish.count` },
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.local.publish.failed.count` },
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.local.publish.no-subscriber-present.count` },
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.local.publish.received.count` },
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.remote.publish.loop-hops-exceeded.count` },
  { name: `com.hivemq.edge.bridge.${mockBridgeId}.remote.publish.received.count` },
  { name: `com.hivemq.edge.messages.dropped.count` },
  { name: `com.hivemq.edge.messages.incoming.connect.count` },
  { name: `com.hivemq.edge.messages.incoming.publish.count` },
  { name: `com.hivemq.edge.messages.incoming.total.count` },
  { name: `com.hivemq.edge.messages.outgoing.publish.count` },
  { name: `com.hivemq.edge.messages.outgoing.total.count` },
  { name: `com.hivemq.edge.messages.retained.current` },
  { name: `com.hivemq.edge.messages.will.count.current` },
  { name: `com.hivemq.edge.messages.will.published.count.total` },
  { name: `com.hivemq.edge.mqtt.connection.not-writable.current` },
  { name: `com.hivemq.edge.networking.bytes.read.total` },
  { name: `com.hivemq.edge.networking.bytes.write.total` },
  { name: `com.hivemq.edge.networking.connections-closed.total.count` },
  { name: `com.hivemq.edge.networking.connections.current` },
  { name: `com.hivemq.edge.persistence.client-session.subscriptions.in-memory.total-size` },
  { name: `com.hivemq.edge.persistence.client-sessions.in-memory.total-size` },
  { name: `com.hivemq.edge.persistence.queued-messages.in-memory.total-size` },
  { name: `com.hivemq.edge.persistence.retained-messages.in-memory.total-size` },
  { name: `com.hivemq.edge.protocol-adapters.opc-ua-client.${MOCK_ADAPTER_ID}.connection.failed.count` },
  { name: `com.hivemq.edge.protocol-adapters.opc-ua-client.${MOCK_ADAPTER_ID}.connection.success.count` },
  { name: `com.hivemq.edge.protocol-adapters.opc-ua-client.${MOCK_ADAPTER_ID}.read.publish.failed.count` },
  { name: `com.hivemq.edge.protocol-adapters.opc-ua-client.${MOCK_ADAPTER_ID}.read.publish.success.count` },
  { name: `com.hivemq.edge.protocol-adapters.opc-ua-client.${MOCK_ADAPTER_ID}.subscription.transfer.failed.count` },
  { name: `com.hivemq.edge.protocol-adapters.simulation.${MOCK_ADAPTER_ID}.connection.failed.count` },
  { name: `com.hivemq.edge.protocol-adapters.simulation.${MOCK_ADAPTER_ID}.connection.success.count` },
  { name: `com.hivemq.edge.protocol-adapters.simulation.${MOCK_ADAPTER_ID}.read.publish.failed.count` },
  { name: `com.hivemq.edge.protocol-adapters.simulation.${MOCK_ADAPTER_ID}.read.publish.success.count` },
  { name: `com.hivemq.edge.sessions.overall.current` },
  { name: `com.hivemq.edge.subscriptions.overall.current` },
  { name: `com.hivemq.messages.governance.count` },
]

// main metrics
export const MOCK_METRIC_BRIDGE = MOCK_METRICS[0].name as string
export const MOCK_METRIC_ADAPTER = MOCK_METRICS[28].name as string

// not in use at the moment
export const MOCK_METRIC_MESSAGE = MOCK_METRICS[10].name as string
export const MOCK_METRIC_NETWORKING = MOCK_METRICS[20].name as string
export const MOCK_METRIC_MQTT = MOCK_METRICS[19].name as string
export const MOCK_METRIC_PERSISTENCE = MOCK_METRICS[24].name as string

export const MOCK_METRIC_SAMPLE: DataPoint = {
  sampleTime: '2023-11-18T00:00:00Z',
  value: 50000,
}

export const MOCK_METRIC_SAMPLE_ARRAY: DataPoint[] = Array.from(Array(10), (_, x) => ({
  sampleTime: DateTime.fromISO(MOCK_METRIC_SAMPLE.sampleTime as string)
    .plus({ hour: x })
    .toISO(),
  value: (MOCK_METRIC_SAMPLE.value as number) + 1000 * x,
}))
