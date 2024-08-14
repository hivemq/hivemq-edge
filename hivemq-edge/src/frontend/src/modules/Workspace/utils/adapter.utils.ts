import { ProtocolAdapter } from '@/api/__generated__'
import { IconType } from 'react-icons'
import { TbSettingsAutomation } from 'react-icons/tb'
import { FaIndustry } from 'react-icons/fa6'
import { GrConnectivity } from 'react-icons/gr'
import { AiFillExperiment } from 'react-icons/ai'
import { MdInput, MdOutlineFindInPage, MdOutput } from 'react-icons/md'

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

// https://stackoverflow.com/questions/41253310/typescript-retrieve-element-type-information-from-array-type
type ArrayElement<ArrayType extends readonly unknown[]> = ArrayType extends readonly (infer ElementType)[]
  ? ElementType
  : never

type CapabilitiesArray = NonNullable<ProtocolAdapter['capabilities']>
type CapabilityType = ArrayElement<CapabilitiesArray> | 'WRITE'

/**
 * @deprecated This is a mock, replacing the missing WRITE capability from the adapters
 */
export const deviceCapabilityIcon: Record<CapabilityType, IconType> = {
  ['READ']: MdOutput,
  ['DISCOVER']: MdOutlineFindInPage,
  ['WRITE']: MdInput,
}