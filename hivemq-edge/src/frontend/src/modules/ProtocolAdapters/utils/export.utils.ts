import * as XLSX from 'xlsx'
import { RJSFSchema } from '@rjsf/utils'
import { Adapter, JsonNode, ProtocolAdapter } from '@/api/__generated__'
import {
  getPropertiesFromPath,
  getTopicPaths,
  getValuesFromPath,
  TOPIC_PATH_ITEMS_TOKEN,
} from '@/modules/Workspace/utils/topics-utils.ts'
import { AdapterExportError, ExportFormat } from '@/modules/ProtocolAdapters/types.ts'
import { acceptMimeTypes } from '@/components/rjsf/BatchSubscription/utils/config.utils.ts'
import { downloadJSON, downloadTimeStamp } from '@datahub/utils/download.utils.ts'
import validator from '@rjsf/validator-ajv8'

// XLSX has a limit in sheet name length
const MAX_SHEET_NAME_CHARS = 31

// TODO[NVL] a better name generator ?
export const formatSheetName = (name: string) => name.substring(0, MAX_SHEET_NAME_CHARS)

export const adapterExportFormats: ExportFormat[] = [
  {
    value: ExportFormat.Type.CONFIGURATION,
    formats: ['.json'],
    downloader: (name, _ext, source, _protocol, callback) => {
      downloadJSON<JsonNode>(name, source)
      callback?.()
    },
  },
  {
    value: ExportFormat.Type.SUBSCRIPTIONS,
    formats: Object.values(acceptMimeTypes).flat(),
    isDisabled: (protocol?: ProtocolAdapter) => {
      if (!protocol) return true
      const paths = getTopicPaths(protocol.configSchema || {})
      return !paths.some((path) => path.includes(`.${TOPIC_PATH_ITEMS_TOKEN}.`))
    },
    downloader: (name, ext, source, protocol, callback) => {
      downloadTableData(`${name}-${downloadTimeStamp()}${ext}`, source, protocol)
      callback?.()
    },
  },
]

export const downloadTableData = (name: string, adapter: Adapter, protocol: ProtocolAdapter) => {
  // Get the list of "mqtt topic" xPaths from the protocol
  const paths = getTopicPaths(protocol.configSchema || {})

  // Only get the first of the subscription arrays (we are ignoring many other sources of subscriptions)
  const mappingPath = paths.find((path) => path.includes(`.${TOPIC_PATH_ITEMS_TOKEN}.`))
  if (!mappingPath) throw new AdapterExportError('protocolAdapter.export.error.noSubscription')

  // Extract the path to the containing array property
  // TODO This is wrong: the "subscription" could be nested or the mqtt topic itself nested
  //  (e.g. root.subscription.*.nested.*.destination or root.remote.*.subscription.destination)
  const subscriptionRoot = mappingPath.split(`.${TOPIC_PATH_ITEMS_TOKEN}.`).shift()
  if (!subscriptionRoot) throw new AdapterExportError('protocolAdapter.export.error.noSubscription')

  // Get the JSON Schema of the item of the containing array property
  const subscriptionSchema = getPropertiesFromPath(mappingPath, protocol.configSchema)

  if (!subscriptionSchema) throw new AdapterExportError('protocolAdapter.export.error.noSchema')

  // Get the data from the active adapter
  // TODO This is still wrong: no guarantees it's an array
  let rows = getValuesFromPath(subscriptionRoot, adapter.config || {}) as RJSFSchema[] | undefined

  if (!rows?.length) {
    // if empty, build a dummy row
    // TODO This is wrong: cells should be matching the types
    const entries = Object.keys(subscriptionSchema).map((property) => [property, ''])
    rows = [Object.fromEntries(entries)]
  } else {
    // Validate the extracted data to the extracted  schema
    const validate = validator.ajv.compile({
      type: 'array',
      // TODO This is wrong: required is missing. The extraction of the item's JSONSchema is not working. Get definitions!
      items: { properties: subscriptionSchema },
    })
    const isValid = validate(rows)
    if (!isValid) throw new AdapterExportError('protocolAdapter.export.error.notValid')
  }

  // generate worksheet and workbook
  const worksheet = XLSX.utils.json_to_sheet(rows)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, formatSheetName(subscriptionRoot))

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
