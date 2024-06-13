import { ExportFormat } from '@/modules/ProtocolAdapters/types.ts'
import { Adapter, JsonNode, ProtocolAdapter } from '@/api/__generated__'
import { getTopicPaths, TOPIC_PATH_ITEMS_TOKEN } from '@/modules/Workspace/utils/topics-utils.ts'
import { AdapterExportError, ExportFormat } from '@/modules/ProtocolAdapters/types.ts'
import { acceptMimeTypes } from '@/components/rjsf/BatchSubscription/utils/config.utils.ts'
import { downloadJSON, downloadTimeStamp } from '@datahub/utils/download.utils.ts'

export const adapterExportFormats: ExportFormat[] = [
  {
    value: ExportFormat.Type.CONFIGURATION,
    formats: ['.json'],
    downloader: (name, _, source) => downloadJSON<JsonNode>(name, source),
  },
  {
    value: ExportFormat.Type.SUBSCRIPTIONS,
    formats: Object.values(acceptMimeTypes).flat(),
    isDisabled: (protocol?: ProtocolAdapter) => {
      if (!protocol) return true
      const paths = getTopicPaths(protocol.configSchema || {})
      return !paths.some((path) => path.includes(`.${TOPIC_PATH_ITEMS_TOKEN}.`))
    },
    downloader: (name, ext, source, protocol) => {
      downloadTableData(`${name}-${downloadTimeStamp()}${ext}`, source, protocol)
    },
  },
]
