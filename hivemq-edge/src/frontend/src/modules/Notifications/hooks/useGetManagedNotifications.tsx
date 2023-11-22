import { ChangeEvent, useMemo } from 'react'
import { Badge, Checkbox, HStack, Link, Text, UseToastOptions } from '@chakra-ui/react'
import { ExternalLinkIcon } from '@chakra-ui/icons'

import { useGetReleases } from '@/api/hooks/useGitHub/useGetReleases.tsx'
import { useGetNotifications } from '@/api/hooks/useFrontendServices/useGetNotifications.tsx'
import { useTranslation } from 'react-i18next'

// TODO[NVL] Need to check whether a "new release" notification might already be handled by BE
export const useGetManagedNotifications = () => {
  const { t } = useTranslation()
  const { data: releases } = useGetReleases()
  const { data: notification } = useGetNotifications()

  const notifications = useMemo<UseToastOptions[]>(() => {
    const list: UseToastOptions[] = []

    const handlePersistence = (event: ChangeEvent<HTMLInputElement>) => {
      console.log('XXXX ss', event.target.checked)
    }

    const clock = (
      <HStack justifyContent={'flex-end'}>
        <Checkbox onChange={handlePersistence} colorScheme="blackAlpha" borderColor={'blackAlpha.500'}>
          {t('notifications.toast.skipNotification')}
        </Checkbox>
      </HStack>
    )

    const defaults: Partial<UseToastOptions> = {
      duration: 30000,
      isClosable: true,
      variant: 'top-accent',
      position: 'top-right',
      status: 'info',
      description: clock,
    }

    if (notification?.items && notification.items.length > 0) {
      const toasts: UseToastOptions[] = notification.items.map((e) => ({
        ...defaults,
        id: e.title,
        status: e.level ? 'warning' : 'info',
        title: <Text>{e.title}</Text>,
        description: (
          <>
            <Text>{e.description}</Text>
            {clock}
          </>
        ),
      }))
      list.push(...toasts)
    }

    if (releases && releases.length > 0) {
      const { name, html_url } = releases[0]
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
            {clock}
          </>
        ),
      })
    }

    return list
  }, [notification, releases, t])

  return { notifications }
}
