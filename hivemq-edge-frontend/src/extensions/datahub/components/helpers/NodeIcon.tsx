import type { IconProps } from '@chakra-ui/react'
import { Icon } from '@chakra-ui/react'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { IconType } from 'react-icons'

import { GrStatusUnknown, GrValidate } from 'react-icons/gr'
import { LuFunctionSquare } from 'react-icons/lu'
import { MdPolicy, MdSchema } from 'react-icons/md'
import { TbTransitionRight } from 'react-icons/tb'
import { AiOutlineCloudServer, AiOutlineInteraction } from 'react-icons/ai'
import { SiMqtt } from 'react-icons/si'
import { PiBridgeThin, PiPlugsConnectedFill } from 'react-icons/pi'

import { DataHubNodeType } from '../../types.ts'

// Simple mapping of node types to icon configuration
const iconMapping: Record<string, { icon: IconType; boxSize: string }> = {
  [DataHubNodeType.ADAPTOR]: { icon: PiPlugsConnectedFill, boxSize: '24px' },
  [DataHubNodeType.BRIDGE]: { icon: PiBridgeThin, boxSize: '24px' },
  [DataHubNodeType.TOPIC_FILTER]: { icon: SiMqtt, boxSize: '16px' },
  [DataHubNodeType.CLIENT_FILTER]: { icon: AiOutlineCloudServer, boxSize: '24px' },
  [DataHubNodeType.DATA_POLICY]: { icon: MdPolicy, boxSize: '24px' },
  [DataHubNodeType.BEHAVIOR_POLICY]: { icon: MdPolicy, boxSize: '24px' },
  [DataHubNodeType.VALIDATOR]: { icon: GrValidate, boxSize: '24px' },
  [DataHubNodeType.SCHEMA]: { icon: MdSchema, boxSize: '24px' },
  [DataHubNodeType.OPERATION]: { icon: AiOutlineInteraction, boxSize: '24px' },
  [DataHubNodeType.FUNCTION]: { icon: LuFunctionSquare, boxSize: '24px' },
  [DataHubNodeType.TRANSITION]: { icon: TbTransitionRight, boxSize: '24px' },
}

interface NodeIconProps extends Omit<IconProps, 'as'> {
  type: DataHubNodeType | string | undefined
}

const NodeIcon: FC<NodeIconProps> = ({ type, ...iconProps }) => {
  const { t } = useTranslation('datahub')

  // Get icon configuration or use default
  const config = type && iconMapping[type] ? iconMapping[type] : { icon: GrStatusUnknown, boxSize: '24px' }
  const ariaLabel = type ? t('workspace.nodes.type', { context: type }) : t('workspace.nodes.type')

  return <Icon as={config.icon} boxSize={config.boxSize} {...iconProps} aria-label={ariaLabel} />
}

export default NodeIcon
