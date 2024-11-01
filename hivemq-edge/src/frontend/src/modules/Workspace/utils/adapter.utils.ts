import { type RJSFSchema } from '@rjsf/utils'
import { type IconType } from 'react-icons'
import { TbSettingsAutomation } from 'react-icons/tb'
import { FaIndustry } from 'react-icons/fa6'
import { GrConnectivity } from 'react-icons/gr'
import { AiFillExperiment } from 'react-icons/ai'
import { RiCompassDiscoverLine } from 'react-icons/ri'

import { type ProtocolAdapter, Status } from '@/api/__generated__'
import { HmInput, HmOutput } from '@/components/react-icons/hm'

import i18n from '@/config/i18n.config.ts'

const MQTT_PROPERTY_STUB = {
  inward: 'ToMqtt',
  outward: 'mqttTo',
  mapping: 'Mappings',
}

const capitalize = (s: string) => s && s[0].toUpperCase() + s.slice(1)

export const getInwardMappingRootProperty = (adapterType: string) => `${adapterType}${MQTT_PROPERTY_STUB.inward}`
export const getOutwardMappingRootProperty = (adapterType: string) =>
  `${MQTT_PROPERTY_STUB.outward}${capitalize(adapterType)}`

export const isBidirectional = (adapter: ProtocolAdapter | undefined) => {
  return Boolean(adapter?.capabilities?.includes('WRITE'))
}

export const getInwardMappingSchema = (adapter: ProtocolAdapter | undefined): RJSFSchema => {
  if (!adapter || !adapter.id) throw new Error(i18n.t('protocolAdapter.export.error.noAdapterConfig'))
  const root = getInwardMappingRootProperty(adapter.id)
  const { properties } = adapter.configSchema as RJSFSchema
  if (!properties) throw new Error(i18n.t('protocolAdapter.export.error.noMapping'))

  const mapping = (properties as RJSFSchema)[root] as RJSFSchema
  const mappingProps = mapping.properties as RJSFSchema
  if (!mappingProps) throw new Error(i18n.t('protocolAdapter.export.error.notValid'))

  return mappingProps[`${root}${MQTT_PROPERTY_STUB.mapping}`]
}

/**
 * @deprecated This is a mock, should be in the OpenAPI spec, https://hivemq.kanbanize.com/ctrl_board/57/cards/25259/details/
 * @see ProtocolAdapterCategory
 */
export enum ProtocolAdapterCategoryName {
  BUILDING_AUTOMATION = 'BUILDING_AUTOMATION',
  INDUSTRIAL = 'INDUSTRIAL',
  CONNECTIVITY = 'CONNECTIVITY',
  SIMULATION = 'SIMULATION',
}

/**
 * @deprecated This is a mock, mapping should be based on ProtocolAdapterCategory and image property
 * @see ProtocolAdapterCategory
 */
export const deviceCategoryIcon: Record<string, IconType> = {
  BUILDING_AUTOMATION: TbSettingsAutomation,
  INDUSTRIAL: FaIndustry,
  CONNECTIVITY: GrConnectivity,
  SIMULATION: AiFillExperiment,
}

type ArrayElement<ArrayType extends Array<unknown>> = ArrayType[number]
type CapabilitiesArray = NonNullable<ProtocolAdapter['capabilities']>
type CapabilityType = ArrayElement<CapabilitiesArray>

export const deviceCapabilityIcon: Record<CapabilityType, IconType> = {
  ['READ']: HmOutput,
  ['DISCOVER']: RiCompassDiscoverLine,
  ['WRITE']: HmInput,
}

export const statusMapping = {
  [Status.runtime.STOPPED]: { text: 'STOPPED', color: 'status.error' },
  [Status.connection.ERROR]: { text: 'ERROR', color: 'status.error' },
  [Status.connection.UNKNOWN]: { text: 'UNKNOWN', color: 'status.error' },
  [Status.connection.CONNECTED]: { text: 'CONNECTED', color: 'status.connected' },
  [Status.connection.DISCONNECTED]: { text: 'DISCONNECTED', color: 'status.disconnected' },
  [Status.connection.STATELESS]: { text: 'STATELESS', color: 'status.stateless' },
}
