import * as XLSX from 'xlsx'
import { RJSFSchema } from '@rjsf/utils'
import { Adapter, JsonNode, ProtocolAdapter } from '@/api/__generated__'
import { getPropertiesFromPath, getTopicPaths, TOPIC_PATH_ITEMS_TOKEN } from '@/modules/Workspace/utils/topics-utils.ts'
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

export const downloadTableData = (name: string, adapter: Adapter, protocol: ProtocolAdapter) => {
  const paths = getTopicPaths(protocol.configSchema || {})

  // We are ignoring potential multiple sources of subscriptions
  const subscriptionPath = paths.find((path) => path.includes(`.${TOPIC_PATH_ITEMS_TOKEN}.`))
  if (!subscriptionPath) throw new AdapterExportError('protocolAdapter.export.error.noSubscription')

  const subscription = subscriptionPath.split(`.${TOPIC_PATH_ITEMS_TOKEN}.`).shift()
  if (!subscription) throw new AdapterExportError('protocolAdapter.export.error.noSubscription')

  // TODO[NVL] Data rows and columns' schema should ve validated [?]

  let rows: RJSFSchema[] | undefined = adapter.config?.[subscription] as RJSFSchema[] | undefined
  if (!rows?.length) {
    const subscriptionSchema = getPropertiesFromPath(subscriptionPath, protocol.configSchema)
    if (!subscriptionSchema) throw new AdapterExportError('protocolAdapter.export.error.noSchema')

    const entries = Object.keys(subscriptionSchema).map((property) => [property, ''])
    rows = [Object.fromEntries(entries)]
  }

  // generate worksheet and workbook
  const worksheet = XLSX.utils.json_to_sheet(rows)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, subscription)

  // calculate column width
  const colSizes = rows.reduce<number[]>((acc, currentRow) => {
    return Object.values(currentRow).map((cellContent, index) => {
      return Math.max(10, cellContent.toString().length, acc[index] || 0)
    })
  }, [])
  worksheet['!cols'] = colSizes.map((width) => ({ wch: width }))

  // TODO[NVL] With xlsx output, we should be able to define type and enums for the columns!

  // create the output file, type based on extension
  XLSX.writeFile(workbook, name, { compression: true })
}
