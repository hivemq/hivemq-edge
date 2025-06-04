import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { IoNotifications } from 'react-icons/io5'
import type { UseToastOptions } from '@chakra-ui/react'
import { Icon, useColorModeValue, useToast } from '@chakra-ui/react'

import ButtonBadge from '@/components/Chakra/ButtonBadge.tsx'

import { useGetManagedNotifications } from './hooks/useGetManagedNotifications.tsx'
import { SkipNotification } from './components/SkipNotification.tsx'
import { BASE_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils'

const NotificationBadge: FC = () => {
  const { t } = useTranslation()
  const toast = useToast(BASE_TOAST_OPTION)
  const { notifications } = useGetManagedNotifications()
  const containerStyle = useColorModeValue(undefined, {
    bg: 'chakra-body-bg',
  })

  const handleClick = () => {
    notifications.forEach((notification) => {
      if (notification.id && !toast.isActive(notification.id)) {
        const notificationWithSkip: UseToastOptions = {
          ...notification,
          containerStyle: containerStyle,
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
      icon={<Icon as={IoNotifications} fontSize="2xl" />}
      onClick={handleClick}
    />
  )
}

export default NotificationBadge
