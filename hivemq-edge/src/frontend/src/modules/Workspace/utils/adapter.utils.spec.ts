import { expect } from 'vitest'
import {
  getInwardMappingRootProperty,
  getOutwardMappingRootProperty,
  isBidirectional,
} from '@/modules/Workspace/utils/adapter.utils.ts'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

describe('isBidirectional', () => {
  it('should return the layout characteristics of a group', async () => {
    expect(isBidirectional(mockProtocolAdapter)).toStrictEqual(false)
    expect(isBidirectional({ ...mockProtocolAdapter, capabilities: ['READ', 'DISCOVER'] })).toStrictEqual(false)
    expect(isBidirectional({ ...mockProtocolAdapter, capabilities: ['READ', 'WRITE'] })).toStrictEqual(true)
  })
})

describe('getInwardMappingProperty', () => {
  it('should return the root property for inward mappings', async () => {
    expect(getInwardMappingRootProperty(MockAdapterType.BACNET)).toStrictEqual('bacnetipToMqtt')
    expect(getInwardMappingRootProperty(MockAdapterType.MODBUS)).toStrictEqual('modbusToMqtt')
    expect(getInwardMappingRootProperty(MockAdapterType.FILE)).toStrictEqual('fileToMqtt')
  })
})

describe('getOutwardMappingProperty', () => {
  it('should return the root property for outward mappings', async () => {
    expect(getOutwardMappingRootProperty(MockAdapterType.MODBUS)).toStrictEqual('mqttToModbus')
    expect(getOutwardMappingRootProperty(MockAdapterType.OPC_UA)).toStrictEqual('mqttToOpcua')
    expect(getOutwardMappingRootProperty(MockAdapterType.FILE)).toStrictEqual('mqttToFile')
  })
})
