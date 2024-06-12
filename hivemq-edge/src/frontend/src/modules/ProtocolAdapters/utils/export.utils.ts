import { ExportFormat } from '@/modules/ProtocolAdapters/types.ts'

export const adapterExportFormats: ExportFormat[] = [
  {
    value: ExportFormat.Type.CONFIGURATION,
  },
  {
    value: ExportFormat.Type.SUBSCRIPTIONS,
    formats: ['xlsx, csv'],
  },
]
