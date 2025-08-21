import { type IconType } from 'react-icons'
import { TbSettingsAutomation } from 'react-icons/tb'
import { FaIndustry } from 'react-icons/fa6'
import { GrConnectivity } from 'react-icons/gr'
import { AiFillExperiment } from 'react-icons/ai'
import { RiCompassDiscoverLine } from 'react-icons/ri'
import { HqCombiner } from '@/components/Icons'

import { type ProtocolAdapter } from '@/api/__generated__'
import { HmInput, HmOutput } from '@/components/react-icons/hm'

export const isBidirectional = (adapter: ProtocolAdapter | undefined) => {
  return Boolean(adapter?.capabilities?.includes('WRITE'))
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
  ['COMBINE']: HqCombiner,
}
