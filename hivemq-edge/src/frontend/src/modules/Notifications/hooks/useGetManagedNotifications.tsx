import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocalStorage } from '@uidotdev/usehooks'
import { Badge, Link, Text, UseToastOptions } from '@chakra-ui/react'
import { ExternalLinkIcon } from '@chakra-ui/icons'

import { useGetReleases } from '@/api/hooks/useGitHub/useGetReleases.tsx'
import { useGetNotifications } from '@/api/hooks/useFrontendServices/useGetNotifications.tsx'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.tsx'

export const useGetManagedNotifications = () => {
  const { t } = useTranslation()
  const { data: configuration } = useGetConfiguration()
  const { data: releases, isSuccess: isReleasesSuccess } = useGetReleases()
  const { data: notification, isSuccess: isNotificationsSuccess } = useGetNotifications()
  const [readNotifications, setReadNotifications] = useState<string[]>([])
  const [skip] = useLocalStorage<string[]>('edge.notifications', [])

  const notifications = useMemo<UseToastOptions[]>(() => {
    const list: UseToastOptions[] = []

    const handleReadNotification = (id: string) => {
      setReadNotifications((old) => Array.from(new Set([...old, id])))
    }

    const defaults: Partial<UseToastOptions> = {
      // Should it be manual closing only?
      duration: 60 * 1000,
      isClosable: true,
      variant: 'top-accent',
      position: 'top-right',
      status: 'info',
      description: '',
    }

    if (notification?.items && notification.items.length > 0) {
      const toasts: UseToastOptions[] = notification.items
        .filter(
          (notification) =>
            !readNotifications.includes(notification.title as string) && !skip.includes(notification.title as string)
        )
        .map((notification) => ({
          ...defaults,
          id: notification.title,
          status: notification.level ? 'warning' : 'info',
          title: <Text>{notification.title}</Text>,
          description: <Text>{notification.description}</Text>,
          onCloseComplete: () => handleReadNotification(notification.title as string),
        }))
      list.push(...toasts)
    }

    if (configuration && releases && releases.length > 0) {
      const { name, html_url } = releases[0]
      const currentVersion = configuration.environment?.properties?.version
      if (currentVersion !== name && !readNotifications.includes(name) && !skip.includes(name))
        list.push({
          ...defaults,
          id: name,
          title: (
            <Text>
              <Text as="span">{t('notifications.releases.title')} </Text>
              <Badge colorScheme="yellow">{name}</Badge>
            </Text>
          ),
          description: (
            <Text>
              {t('notifications.releases.description')}{' '}
              <Link href={html_url} isExternal>
                <ExternalLinkIcon mx="2px" mb="4px" /> hivemq/hivemq-edge
              </Link>
            </Text>
          ),
          onCloseComplete: () => handleReadNotification(name),
        })
    }

    return list
  }, [notification?.items, configuration, releases, readNotifications, skip, t])

  return { notifications, isSuccess: isNotificationsSuccess && isReleasesSuccess, readNotifications }
}
