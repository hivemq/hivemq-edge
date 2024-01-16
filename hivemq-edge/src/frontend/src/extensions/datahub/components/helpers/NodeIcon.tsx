import { Icon } from '@chakra-ui/react'
import { FC } from 'react'

import { GrConnect, GrStatusUnknown, GrValidate } from 'react-icons/gr'
import { LuFunctionSquare } from 'react-icons/lu'
import { MdPolicy, MdSchema } from 'react-icons/md'
import { TbTransitionRight } from 'react-icons/tb'
import { AiOutlineCloudServer } from 'react-icons/ai'
import { SiMqtt } from 'react-icons/si'

import { DataHubNodeType } from '../../types.ts'
import { useTranslation } from 'react-i18next'

const iconMapping: Record<string, (label: string) => JSX.Element> = {
  [DataHubNodeType.ADAPTOR]: (label) => <Icon as={GrConnect} color={'black'} boxSize={'24px'} aria-label={label} />,
  [DataHubNodeType.TOPIC_FILTER]: (label) => <Icon as={SiMqtt} color={'black'} boxSize={'16px'} aria-label={label} />,
  [DataHubNodeType.CLIENT_FILTER]: (label) => (
    <Icon as={AiOutlineCloudServer} color={'black'} boxSize={'24px'} aria-label={label} />
  ),
  [DataHubNodeType.DATA_POLICY]: (label) => <Icon as={MdPolicy} color={'black'} boxSize={'24px'} aria-label={label} />,
  [DataHubNodeType.BEHAVIOR_POLICY]: (label) => (
    <Icon as={MdPolicy} color={'black'} boxSize={'24px'} aria-label={label} />
  ),
  [DataHubNodeType.VALIDATOR]: (label) => <Icon as={GrValidate} color={'black'} boxSize={'24px'} aria-label={label} />,
  [DataHubNodeType.SCHEMA]: (label) => <Icon as={MdSchema} color={'black'} boxSize={'24px'} aria-label={label} />,
  [DataHubNodeType.ACTION]: (label) => (
    <Icon as={LuFunctionSquare} color={'black'} boxSize={'24px'} aria-label={label} />
  ),
  [DataHubNodeType.TRANSITION]: (label) => (
    <Icon as={TbTransitionRight} color={'black'} boxSize={'24px'} aria-label={label} />
  ),
}

interface NodeIconProps {
  type: DataHubNodeType | string | undefined
}

const NodeIcon: FC<NodeIconProps> = ({ type }) => {
  const { t } = useTranslation('datahub')

  if (!type || !iconMapping[type]) {
    return (
      <Icon as={GrStatusUnknown} color={'black'} boxSize={'24px'} aria-label={t('workspace.nodes.type') as string} />
    )
  }
  return iconMapping[type](t('workspace.nodes.type', { context: type }))
}

export default NodeIcon
