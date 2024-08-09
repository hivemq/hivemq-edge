import { useTranslation } from 'react-i18next'
import { Icon } from '@chakra-ui/react'

import { IoHomeOutline } from 'react-icons/io5'
import { PiBridgeThin, PiPlugsConnectedFill } from 'react-icons/pi'
import { BsIntersect } from 'react-icons/bs'
import { GoLinkExternal } from 'react-icons/go'
import { MdOutlineEventNote, MdPolicy } from 'react-icons/md'

import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.ts'
import WorkspaceIcon from '@/components/Icons/WorkspaceIcon.tsx'

import config from '@/config'

import { NavLinksBlockType } from '../types.ts'

const useGetNavItems = (): { data: NavLinksBlockType[]; isSuccess: boolean } => {
  const { t } = useTranslation()
  const { data, isSuccess } = useGetConfiguration()

  const workspaceLink = config.features.WORKSPACE_FLOW_PANEL
    ? [
        {
          icon: <WorkspaceIcon boxSize={4} />,
          href: '/workspace',
          label: t('translation:navigation.gateway.routes.workspace'),
        },
      ]
    : []

  const menu = [
    {
      title: t('translation:navigation.gateway.title'),
      items: [
        {
          icon: <IoHomeOutline />,
          href: '/',
          label: t('translation:navigation.gateway.routes.home'),
        },
        ...workspaceLink,
        {
          icon: <Icon as={PiBridgeThin} fontSize="20px" />,
          href: '/mqtt-bridges',
          label: t('translation:navigation.gateway.routes.bridges'),
        },
        {
          icon: <PiPlugsConnectedFill />,
          href: '/protocol-adapters',
          label: t('translation:navigation.gateway.routes.protocolAdapters'),
        },
        {
          icon: <MdOutlineEventNote />,
          href: '/event-logs',
          label: t('translation:navigation.gateway.routes.eventLogs'),
        },
        {
          icon: <Icon as={MdPolicy} fontSize="16px" />,
          href: '/datahub',
          label: t('datahub:navigation.mainPage'),
        },
        {
          icon: <BsIntersect />,
          href: '/namespace',
          label: t('translation:navigation.extensions.routes.namespace'),
        },
      ],
    },
    {
      title: t('translation:navigation.resources.title'),
      items: [
        {
          icon: <GoLinkExternal />,
          // TODO[NVL] Change to a proper link when defined
          href: data?.resources?.items?.[0]?.url as string,
          isExternal: true,
          label: t('translation:navigation.resources.routes.articles'),
        },
        {
          icon: <GoLinkExternal />,
          href: data?.gitHubLink?.url as string,
          isExternal: true,
          label: t('translation:navigation.resources.routes.github'),
        },
        {
          icon: <GoLinkExternal />,
          href: data?.documentationLink?.url as string,
          isExternal: true,
          label: t('translation:navigation.resources.routes.help'),
        },
      ],
    },
  ]

  return { data: menu, isSuccess }
}

export default useGetNavItems
