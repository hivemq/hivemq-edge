import { FC } from 'react'
import { Icon, type IconProps } from '@chakra-ui/react'
import { SiMqtt } from 'react-icons/si'
import { useTranslation } from 'react-i18next'
import { FaTags } from 'react-icons/fa6'
import { AiOutlineCloudServer } from 'react-icons/ai'
import { PiGraphFill } from 'react-icons/pi'

export const TopicIcon: FC<IconProps> = (props) => {
  const { t } = useTranslation('components')

  return <Icon as={SiMqtt} boxSize={4} aria-label={t('iconLabel.topic')} {...props} />
}

export const PLCTagIcon: FC<IconProps> = (props) => {
  const { t } = useTranslation('components')

  return <Icon as={FaTags} boxSize={4} aria-label={t('iconLabel.tag')} {...props} />
}

export const ClientIcon: FC<IconProps> = (props) => {
  const { t } = useTranslation('components')

  return <Icon as={AiOutlineCloudServer} boxSize={4} aria-label={t('iconLabel.client')} {...props} />
}

export const WorkspaceIcon: FC<IconProps> = (props) => {
  const { t } = useTranslation('components')

  return <Icon as={PiGraphFill} boxSize={6} aria-label={t('iconLabel.workspace')} {...props} />
}
