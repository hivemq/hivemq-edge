import { ExportFormat } from '@/modules/ProtocolAdapters/types.ts'
import { downloadJSON, downloadTimeStamp } from '@datahub/utils/download.utils.ts'

export const adapterExportFormats: ExportFormat[] = [
  {
    value: ExportFormat.Type.CONFIGURATION,
    formats: ['.json'],
    downloader: (name, _, source) => downloadJSON<JsonNode>(name, source),
  },
  {
    value: ExportFormat.Type.SUBSCRIPTIONS,
    formats: ['xlsx, csv'],
  },
]
