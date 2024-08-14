import { ProtocolAdapter } from '@/api/__generated__'

/**
 * @deprecated This is a mock, replacing the missing WRITE capability from the adapters
 * @param adapter Adapter | undefined
 */
export const isBidirectional = (adapter: ProtocolAdapter | undefined) => {
  return Boolean(adapter?.id?.includes('opc-ua-client'))
}
