import { describe, expect, vi } from 'vitest'
import { adapterExportFormats, formatSheetName } from '@/modules/ProtocolAdapters/utils/export.utils.ts'
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
        value: 'SUBSCRIPTIONS',
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
    sub.downloader?.('sssss', '.json', mockAdapter, mockProtocolAdapter, callback)
    expect(callback).toHaveBeenCalled()
  })

  it('should run the subscription downloader', () => {
    const sub = adapterExportFormats[1]
    const callback = vi.fn()
    expect(sub.value).toStrictEqual(ExportFormat.Type.SUBSCRIPTIONS)
    expect(sub.isDisabled?.(mockProtocolAdapter)).toBeFalsy()
    expect(sub.downloader).not.toBeUndefined()
    expect(callback).not.toHaveBeenCalled()
    // Test the exact error message
    expect(() => sub.downloader?.('test', '.csv', mockAdapter, mockProtocolAdapter, callback)).toThrowError(
      /^cannot save file test-(.*)\.csv$/
    )
  })
})
