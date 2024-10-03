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
    value: ExportFormat.Type.MAPPINGS,
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

  // Only get the first of the mapping arrays (we are ignoring many other sources of mappins)
  const mappingPath = paths.find((path) => path.includes(`.${TOPIC_PATH_ITEMS_TOKEN}.`))
  if (!mappingPath) throw new AdapterExportError('protocolAdapter.export.error.noMapping')

  // Extract the path to the containing array property
  // TODO This is wrong: the "mappings" could be nested or the mqtt topic itself nested
  //  (e.g. root.mappings.*.nested.*.destination or root.remote.*.mappings.destination)
  const mappingRoot = mappingPath.split(`.${TOPIC_PATH_ITEMS_TOKEN}.`).shift()
  if (!mappingRoot) throw new AdapterExportError('protocolAdapter.export.error.noMapping')

  // Get the JSON Schema of the item of the containing array property
  const mappingSchema = getPropertiesFromPath(mappingPath, protocol.configSchema)

  if (!mappingSchema) throw new AdapterExportError('protocolAdapter.export.error.noSchema')

  // Get the data from the active adapter
  // TODO This is still wrong: no guarantees it's an array
  let rows = getValuesFromPath(mappingRoot, adapter.config || {}) as RJSFSchema[] | undefined

  if (!rows?.length) {
    // if empty, build a dummy row
    // TODO This is wrong: cells should be matching the types
    const entries = Object.keys(mappingSchema).map((property) => [property, ''])
    rows = [Object.fromEntries(entries)]
  } else {
    // Validate the extracted data to the extracted  schema
    const validate = validator.ajv.compile({
      type: 'array',
      // TODO This is wrong: required is missing. The extraction of the item's JSONSchema is not working. Get definitions!
      items: { properties: mappingSchema },
    })
    const isValid = validate(rows)
    if (!isValid) throw new AdapterExportError('protocolAdapter.export.error.notValid')
  }

  // generate worksheet and workbook
  const worksheet = XLSX.utils.json_to_sheet(rows)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, formatSheetName(mappingRoot))

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
