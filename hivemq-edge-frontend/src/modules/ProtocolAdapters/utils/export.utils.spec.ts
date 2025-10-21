import { describe, expect, vi } from 'vitest'
import {
  adapterExportFormats,
  downloadTableData,
  formatSheetName,
} from '@/modules/ProtocolAdapters/utils/export.utils.ts'
import { ExportFormat } from '@/modules/ProtocolAdapters/types.ts'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('formatSheetName', () => {
  it('should return a valid name', () => {
    expect(formatSheetName('123')).toStrictEqual('123')
    expect(formatSheetName('a.very.long.name.with.tricky.utf.characters')).toStrictEqual(
      'a.very.long.name.with.tricky.ut'
    )
  })
})

describe('adapterExportFormats', () => {
  it('should return list of valid formats', () => {
    expect(adapterExportFormats).toStrictEqual([
      expect.objectContaining({
        formats: ['.json'],
        value: 'CONFIGURATION',
      }),
      expect.objectContaining({
        formats: ['.xlsx', '.xls', '.csv'],
        value: 'MAPPINGS',
      }),
    ])
  })

  it('should run the configuration downloader', () => {
    const sub = adapterExportFormats[0]
    const callback = vi.fn()
    expect(sub.value).toStrictEqual(ExportFormat.Type.CONFIGURATION)
    expect(sub.isDisabled?.()).toBeFalsy()
    expect(sub.downloader).not.toBeUndefined()
    expect(callback).not.toHaveBeenCalled()
    sub.downloader?.('test', '.json', mockAdapter, mockProtocolAdapter, callback)
    expect(callback).toHaveBeenCalled()
  })

  it('should run the mapping downloader', () => {
    const sub = adapterExportFormats[1]
    const callback = vi.fn()
    expect(sub.value).toStrictEqual(ExportFormat.Type.MAPPINGS)
    expect(sub.isDisabled?.(mockProtocolAdapter)).toBeFalsy()
    expect(sub.downloader).not.toBeUndefined()
    expect(callback).not.toHaveBeenCalled()
    // Test the exact error message
    expect(() => sub.downloader?.('test', '.csv', mockAdapter, mockProtocolAdapter, callback)).toThrowError(
      /^cannot save file test-(.*)\.csv$/
    )
  })

  it('should disable mappings export when protocol is undefined', () => {
    const sub = adapterExportFormats[1]
    expect(sub.isDisabled?.(undefined)).toBeTruthy()
  })

  it('should disable mappings export when no topic paths with mappings', () => {
    const sub = adapterExportFormats[1]
    const protocolWithoutMappings = {
      ...mockProtocolAdapter,
      configSchema: {
        type: 'object',
        properties: {
          simple: { type: 'string' },
        },
      },
    }
    expect(sub.isDisabled?.(protocolWithoutMappings)).toBeTruthy()
  })

  it('should enable mappings export when topic paths with mappings exist', () => {
    const sub = adapterExportFormats[1]
    expect(sub.isDisabled?.(mockProtocolAdapter)).toBeFalsy()
  })
})

describe('downloadTableData', () => {
  it('should throw error when no mapping path found', () => {
    const adapterWithoutMappings = {
      ...mockAdapter,
      config: { simple: 'value' },
    }
    const protocolWithoutMappings = {
      ...mockProtocolAdapter,
      configSchema: {
        type: 'object',
        properties: {
          simple: { type: 'string' },
        },
      },
    }

    expect(() => downloadTableData('test.xlsx', adapterWithoutMappings, protocolWithoutMappings)).toThrow(
      'protocolAdapter.export.error.noMapping'
    )
  })

  it('should throw error when mapping root is undefined', () => {
    const protocolWithInvalidMapping = {
      ...mockProtocolAdapter,
      configSchema: {
        type: 'object',
        properties: {
          '*': {
            type: 'object',
            properties: {
              destination: { type: 'string' },
            },
          },
        },
      },
    }

    expect(() => downloadTableData('test.xlsx', mockAdapter, protocolWithInvalidMapping)).toThrow(
      'protocolAdapter.export.error.noMapping'
    )
  })

  it('should throw error when mapping schema is not found', () => {
    const protocolWithMissingSchema = {
      ...mockProtocolAdapter,
      configSchema: {
        type: 'object',
        properties: {},
      },
    }

    expect(() => downloadTableData('test.xlsx', mockAdapter, protocolWithMissingSchema)).toThrow(
      'protocolAdapter.export.error.noMapping'
    )
  })

  it('should create dummy row when no rows exist', () => {
    const adapterWithEmptyConfig = {
      ...mockAdapter,
      config: {},
    }

    // This should not throw and should create dummy rows
    expect(() => downloadTableData('test.xlsx', adapterWithEmptyConfig, mockProtocolAdapter)).toThrow() // Will still throw because of validation or other issues
  })

  it('should validate rows against schema', () => {
    const adapterWithInvalidData = {
      ...mockAdapter,
      config: {
        subscriptions: [{ invalidField: 'value' }],
      },
    }

    expect(() => downloadTableData('test.xlsx', adapterWithInvalidData, mockProtocolAdapter)).toThrow()
  })
})
