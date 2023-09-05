import { FC } from 'react'
import { Alert, Icon, Text } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { LuConstruction } from 'react-icons/lu'

const DisclaimerWIP: FC = () => {
  const { t } = useTranslation()

  return (
    <Alert status="info">
      <Icon as={LuConstruction} boxSize={30} m={2} />
      <Text> {t('error.wip.message')}</Text>
    </Alert>
  )
}

export default DisclaimerWIP
