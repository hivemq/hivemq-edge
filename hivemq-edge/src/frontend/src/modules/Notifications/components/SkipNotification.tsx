import { ChangeEvent, FC } from 'react'
import { Checkbox, HStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { useLocalStorage } from '@uidotdev/usehooks'

interface SkipNotificationProps {
  id: string
}

export const SkipNotification: FC<SkipNotificationProps> = ({ id }) => {
  const { t } = useTranslation()
  const [, setSkip] = useLocalStorage<string[]>('edge.notifications', [])

  const handleOnChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSkip((old) => [...old, id])
    } else {
      setSkip((old) => old.filter((e) => e != id))
    }
  }
  return (
    <HStack justifyContent={'flex-end'}>
      <Checkbox
        onChange={handleOnChange}
        colorScheme="blackAlpha"
        borderColor={'blackAlpha.500'}
        data-testid={'notification-skip'}
      >
        {t('notifications.toast.skipNotification')}
      </Checkbox>
    </HStack>
  )
}
