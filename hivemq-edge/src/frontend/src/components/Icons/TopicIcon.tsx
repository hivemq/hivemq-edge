import { Icon, type IconProps } from '@chakra-ui/react'
import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { SiMqtt } from 'react-icons/si'

const TopicIcon: FC<IconProps> = (props) => {
  const { t } = useTranslation('components')

  return <Icon as={SiMqtt} boxSize={4} aria-label={t('topic.iconLabel') as string} {...props} />
}

export default TopicIcon
