import { type ProtocolAdapter } from '@/api/__generated__'
import { type IconType } from 'react-icons'
import { TbSettingsAutomation } from 'react-icons/tb'
import { FaIndustry } from 'react-icons/fa6'
import { GrConnectivity } from 'react-icons/gr'
import { AiFillExperiment } from 'react-icons/ai'
import { MdOutlineFindInPage } from 'react-icons/md'
import { HmInput, HmOutput } from '@/components/react-icons/hm'

/**
 * @deprecated This is a mock, replacing the missing WRITE capability from the adapters
 * @param adapter Adapter | undefined
 */
export const isBidirectional = (adapter: ProtocolAdapter | undefined) => {
  return Boolean(adapter?.id?.includes('opc-ua-client'))
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
type CapabilityType = ArrayElement<CapabilitiesArray> | 'WRITE'

/**
 * @deprecated This is a mock, replacing the missing WRITE capability from the adapters
 */
export const deviceCapabilityIcon: Record<CapabilityType, IconType> = {
  ['READ']: HmOutput,
  ['DISCOVER']: MdOutlineFindInPage,
  ['WRITE']: HmInput,
}
