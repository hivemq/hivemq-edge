import { enumFromStringValue } from '@/utils/types.utils.ts'

// The id of protocol adapters are now all lowercase and unique
export enum MockAdapterType {
  BACNET = 'bacnetip',
  S7 = 's7',
  MODBUS = 'modbus',
  FILE_INPUT = 'file_input',
  HTTP = 'http',
  SIMULATION = 'simulation',
  EIP = 'eip',
  OPC_UA = 'opcua',
  ADS = 'ads',
}

export const mockBiDirectionals = [MockAdapterType.OPC_UA, MockAdapterType.MODBUS]

/**
 * @deprecated This is a mock, replacing the missing WRITE capability from the adapters
 */
export const isMockAdapterTypeBidirectional = (type: string | undefined) => {
  if (!type) return false
  const typed = enumFromStringValue(MockAdapterType, type)
  return typed && mockBiDirectionals.includes(typed)
}
