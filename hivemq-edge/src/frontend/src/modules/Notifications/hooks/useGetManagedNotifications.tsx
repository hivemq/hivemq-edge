import { ChangeEvent, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Link, Text, UseToastOptions } from '@chakra-ui/react'
import { ExternalLinkIcon } from '@chakra-ui/icons'

import { useGetReleases } from '@/api/hooks/useGitHub/useGetReleases.tsx'
import { useGetNotifications } from '@/api/hooks/useFrontendServices/useGetNotifications.tsx'

import { SkipNotification } from '../components/SkipNotification.tsx'

// TODO[NVL] Need to check whether a "new release" notification might already be handled by BE
export const useGetManagedNotifications = () => {
  const { t } = useTranslation()
  const { data: releases, isSuccess: isReleasesSuccess } = useGetReleases()
  const { data: notification, isSuccess: isNotificationsSuccess } = useGetNotifications()
  const [readNotifications, setReadNotifications] = useState<string[]>([])

  const notifications = useMemo<UseToastOptions[]>(() => {
    const list: UseToastOptions[] = []

    const handlePersistence = (event: ChangeEvent<HTMLInputElement>) => {
      console.log('XXXX handlePersistence', event.target.checked)
    }

    const handleReadNotification = (id: string) => {
      console.log('XXXXX handleReadNotification', id)
      setReadNotifications((old) => Array.from(new Set([...old, id])))
    }

    const defaults: Partial<UseToastOptions> = {
      // Should it be manual closing only?
      duration: 30 * 1000,
      isClosable: true,
      variant: 'top-accent',
      position: 'top-right',
      status: 'info',
      description: <SkipNotification onChange={handlePersistence} />,
    }

    if (notification?.items && notification.items.length > 0) {
      const toasts: UseToastOptions[] = notification.items
        .filter((e) => !readNotifications.includes(e.title as string))
        .map((e) => ({
          ...defaults,
          id: e.title,
          status: e.level ? 'warning' : 'info',
          title: <Text>{e.title}</Text>,
          description: (
            <>
              <Text>{e.description}</Text>
              <SkipNotification onChange={handlePersistence} />
            </>
          ),
          onCloseComplete: () => handleReadNotification(e.title as string),
        }))
      list.push(...toasts)
    }

    if (releases && releases.length > 0) {
      const { name, html_url } = releases[0]
      if (!readNotifications.includes(name))
        list.push({
          ...defaults,
          id: name,
          title: (
            <Text>
              <Text as={'span'}>{t('notifications.releases.title')} </Text>
              <Badge colorScheme={'yellow'}>{name}</Badge>
            </Text>
          ),
          description: (
            <>
              <Text>
                {t('notifications.releases.description')}{' '}
                <Link href={html_url} isExternal>
                  <ExternalLinkIcon mx="2px" mb={'4px'} /> hivemq/hivemq-edge
                </Link>
              </Text>
              <SkipNotification onChange={handlePersistence} />
            </>
          ),
          onCloseComplete: () => handleReadNotification(name),
        })
    }

    return list
  }, [readNotifications, notification, releases, t])

  return { notifications, isSuccess: isNotificationsSuccess && isReleasesSuccess, readNotifications }
}
