import { Icon } from '@chakra-ui/react'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'

import { GrStatusUnknown, GrValidate } from 'react-icons/gr'
import { LuFunctionSquare } from 'react-icons/lu'
import { MdPolicy, MdSchema } from 'react-icons/md'
import { TbTransitionRight } from 'react-icons/tb'
import { AiOutlineCloudServer, AiOutlineInteraction } from 'react-icons/ai'
import { SiMqtt } from 'react-icons/si'
import { PiBridgeThin, PiPlugsConnectedFill } from 'react-icons/pi'

import { DataHubNodeType } from '../../types.ts'

const iconMapping: Record<string, (label: string) => JSX.Element> = {
  [DataHubNodeType.ADAPTOR]: (label) => <Icon as={PiPlugsConnectedFill} boxSize="24px" aria-label={label} />,
  [DataHubNodeType.BRIDGE]: (label) => <Icon as={PiBridgeThin} boxSize="24px" aria-label={label} />,
  [DataHubNodeType.TOPIC_FILTER]: (label) => <Icon as={SiMqtt} boxSize="16px" aria-label={label} />,
  [DataHubNodeType.CLIENT_FILTER]: (label) => <Icon as={AiOutlineCloudServer} boxSize="24px" aria-label={label} />,
  [DataHubNodeType.DATA_POLICY]: (label) => <Icon as={MdPolicy} boxSize="24px" aria-label={label} />,
  [DataHubNodeType.BEHAVIOR_POLICY]: (label) => <Icon as={MdPolicy} boxSize="24px" aria-label={label} />,
  [DataHubNodeType.VALIDATOR]: (label) => <Icon as={GrValidate} boxSize="24px" aria-label={label} />,
  [DataHubNodeType.SCHEMA]: (label) => <Icon as={MdSchema} boxSize="24px" aria-label={label} />,
  [DataHubNodeType.OPERATION]: (label) => <Icon as={AiOutlineInteraction} boxSize="24px" aria-label={label} />,
  [DataHubNodeType.FUNCTION]: (label) => <Icon as={LuFunctionSquare} boxSize="24px" aria-label={label} />,
  [DataHubNodeType.TRANSITION]: (label) => <Icon as={TbTransitionRight} boxSize="24px" aria-label={label} />,
}

interface NodeIconProps {
  type: DataHubNodeType | string | undefined
}

const NodeIcon: FC<NodeIconProps> = ({ type }) => {
  const { t } = useTranslation('datahub')

  if (!type || !iconMapping[type]) {
    return <Icon as={GrStatusUnknown} boxSize="24px" aria-label={t('workspace.nodes.type')} />
  }
  return iconMapping[type](t('workspace.nodes.type', { context: type }))
}

export default NodeIcon
