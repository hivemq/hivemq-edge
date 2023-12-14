import { Icon, UseToastOptions, useToast } from '@chakra-ui/react'
import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { IoNotifications } from 'react-icons/io5'

import ButtonBadge from '@/components/Chakra/ButtonBadge.tsx'

import { SkipNotification } from './components/SkipNotification.tsx'
import { useGetManagedNotifications } from './hooks/useGetManagedNotifications.tsx'

const NotificationBadge: FC = () => {
  const { t } = useTranslation()
  const toast = useToast()
  const { notifications } = useGetManagedNotifications()

  const handleClick = () => {
    notifications.forEach((notification) => {
      if (notification.id && !toast.isActive(notification.id)) {
        const notificationWithSkip: UseToastOptions = {
          ...notification,
          description: (
            <>
              {notification.description}
              <SkipNotification id={notification.id.toString()} />
            </>
          ),
        }
        toast(notificationWithSkip)
      }
    })
  }

  return (
    <ButtonBadge
      aria-label={t('notifications.badge.ariaLabel')}
      badgeCount={notifications.length}
      isDisabled={!notifications.length}
      icon={<Icon as={IoNotifications} fontSize={'2xl'} />}
      onClick={handleClick}
    />
  )
}

export default NotificationBadge
