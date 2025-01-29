import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocalStorage } from '@uidotdev/usehooks'
import type { UseToastOptions } from '@chakra-ui/react'
import { Badge, Link, Text } from '@chakra-ui/react'
import { ExternalLinkIcon } from '@chakra-ui/icons'

import { useGetReleases } from '@/api/hooks/useGitHub/useGetReleases.ts'
import { useGetNotifications } from '@/api/hooks/useFrontendServices/useGetNotifications.ts'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.ts'
import { CAPABILITY, useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'

export const useGetManagedNotifications = () => {
  const { t } = useTranslation()
  const { data: configuration } = useGetConfiguration()
  const { data: releases, isSuccess: isReleasesSuccess } = useGetReleases()
  const { data: notification, isSuccess: isNotificationsSuccess } = useGetNotifications()
  const isWritableConfig = useGetCapability(CAPABILITY.WRITEABLE_CONFIG)
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

    if (!isWritableConfig && !skip.includes(CAPABILITY.WRITEABLE_CONFIG)) {
      // TODO[EDGE] The important feature is when the config is NOT writable (API request will fail)
      //  The question is whether undefined (because it's not found) and undefined (because it is not supported)
      //  have the same effect

      list.push({
        ...defaults,
        id: CAPABILITY.WRITEABLE_CONFIG,
        status: 'warning',
        title: <Text>{t('capabilities.WRITEABLE_CONFIG.title')} </Text>,
        description: <Text>{t('capabilities.WRITEABLE_CONFIG.description')} </Text>,
        onCloseComplete: () => handleReadNotification(CAPABILITY.WRITEABLE_CONFIG),
      })
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
  }, [notification?.items, isWritableConfig, skip, configuration, releases, readNotifications, t])

  return { notifications, isSuccess: isNotificationsSuccess && isReleasesSuccess, readNotifications }
}
