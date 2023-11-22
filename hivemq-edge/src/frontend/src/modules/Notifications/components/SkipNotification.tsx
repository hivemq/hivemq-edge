import { ChangeEvent, FC } from 'react'
import { Checkbox, HStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

interface SkipNotificationProps {
  onChange: (event: ChangeEvent<HTMLInputElement>) => void
}
export const SkipNotification: FC<SkipNotificationProps> = ({ onChange }) => {
  const { t } = useTranslation()

  return (
    <HStack justifyContent={'flex-end'}>
      <Checkbox onChange={onChange} colorScheme="blackAlpha" borderColor={'blackAlpha.500'} isDisabled>
        {t('notifications.toast.skipNotification')}
      </Checkbox>
    </HStack>
  )
}
