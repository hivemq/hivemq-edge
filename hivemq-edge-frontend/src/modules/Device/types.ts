import type { ProtocolAdapter } from '@/api/__generated__'

export interface DeviceTagListContext {
  adapterId: string
  capabilities?: ProtocolAdapter['capabilities']
}
