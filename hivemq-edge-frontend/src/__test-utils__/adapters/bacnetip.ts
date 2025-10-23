import type { ProtocolAdapter } from '@/api/__generated__'

export const MOCK_PROTOCOL_BACNET_IP: ProtocolAdapter = {
  id: 'bacnetip',
  protocol: 'bacnetip',
  name: 'BACnet/IP Protocol Adapter',
  description: 'Connects HiveMQ Edge to existing BACnet/IP devices.',
  url: 'https://docs.hivemq.com/hivemq-edge/protocol-adapters.html#bacnetip',
  version: '2025.15',
  logoUrl: 'https://raw.githubusercontent.com/hivemq/hivemq-edge/master/hivemq-edge/images/bacnet-adapter-logo.png',
  provisioningUrl: 'https://github.com/hivemq/hivemq-edge/releases',
  author: 'HiveMQ',
  installed: false,
  capabilities: [],
}
