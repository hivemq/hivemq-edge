import { FC } from 'react'
import { FiMail } from 'react-icons/fi'
import { useToast } from '@chakra-ui/react'

import ButtonBadge from '@/components/Chakra/ButtonBadge.tsx'
import { useGetManagedNotifications } from './hooks/useGetManagedNotifications.tsx'

const NotificationBadge: FC = () => {
  const toast = useToast()
  const { notifications } = useGetManagedNotifications()

  const handleClick = () => {
    notifications.forEach((notification) => {
      if (notification.id && !toast.isActive(notification.id)) toast(notification)
    })
  }

  return (
    <ButtonBadge
      aria-label={'dd'}
      badgeLabel={notifications.length}
      isDisabled={!notifications.length}
      icon={<FiMail />}
      onClick={handleClick}
    />
  )
}

export default NotificationBadge
