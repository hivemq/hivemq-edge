import { beforeEach, describe, expect, vi } from 'vitest'
import * as XLSX from 'xlsx'
import {
  adapterExportFormats,
  downloadTableData,
  formatSheetName,
} from '@/modules/ProtocolAdapters/utils/export.utils.ts'
import { ExportFormat } from '@/modules/ProtocolAdapters/types.ts'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

// `writeFile` is the one step that touches the outside world. It used to throw 'cannot save file'
// under the test environment, and these tests leaned on that to assert "something happened"; it now
// succeeds, so stub it and assert the export itself instead.
vi.mock('xlsx', async (importOriginal) => ({
  ...(await importOriginal<typeof XLSX>()),
  writeFile: vi.fn(),
}))

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
    sub.downloader?.('test', '.csv', mockAdapter, mockProtocolAdapter, callback)
    expect(callback).toHaveBeenCalled()
    expect(XLSX.writeFile).toHaveBeenCalledWith(
      expect.anything(),
      expect.stringMatching(/^test-(.*)\.csv$/),
      expect.anything()
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
  beforeEach(() => {
    vi.mocked(XLSX.writeFile).mockClear()
  })

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

    expect(() => downloadTableData('test.xlsx', adapterWithEmptyConfig, mockProtocolAdapter)).not.toThrow()
    // A single placeholder row, every column blank, so the export still carries the headers
    const [sheet] = XLSX.utils.sheet_to_json<Record<string, unknown>>(
      vi.mocked(XLSX.writeFile).mock.calls[0][0].Sheets[
        vi.mocked(XLSX.writeFile).mock.calls[0][0].SheetNames[0]
      ] as XLSX.WorkSheet,
      { defval: '' }
    )
    expect(Object.values(sheet ?? {}).every((value) => value === '')).toBe(true)
  })

  it('should validate rows against schema', () => {
    const adapterWithInvalidData = {
      ...mockAdapter,
      config: {
        // mqttTopic is declared as a string, so a number fails the generated array schema
        simulationToMqtt: { simulationToMqttMappings: [{ mqttTopic: 123 }] },
      },
    }

    expect(() => downloadTableData('test.xlsx', adapterWithInvalidData, mockProtocolAdapter)).toThrow(
      'protocolAdapter.export.error.notValid'
    )
  })
})
