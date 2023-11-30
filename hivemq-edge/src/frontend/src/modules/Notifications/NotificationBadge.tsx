import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { FiMail } from 'react-icons/fi'
import { useToast, UseToastOptions } from '@chakra-ui/react'

import ButtonBadge from '@/components/Chakra/ButtonBadge.tsx'

import { useGetManagedNotifications } from './hooks/useGetManagedNotifications.tsx'
import { SkipNotification } from './components/SkipNotification.tsx'

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
      icon={<FiMail />}
      onClick={handleClick}
    />
  )
}

export default NotificationBadge
