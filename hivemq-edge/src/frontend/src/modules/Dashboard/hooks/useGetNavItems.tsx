import { useTranslation } from 'react-i18next'
import { Icon } from '@chakra-ui/react'

import { IoHomeOutline } from 'react-icons/io5'
import { PiBridgeThin, PiPlugsConnectedFill } from 'react-icons/pi'
import { BsIntersect } from 'react-icons/bs'
import { HiOutlinePuzzle } from 'react-icons/hi'
import { GoLinkExternal } from 'react-icons/go'
import { MdOutlineEventNote } from 'react-icons/md'

import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.tsx'
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
          href: '/edge-flow',
          label: t('translation:navigation.gateway.routes.workspace') as string,
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
          label: t('translation:navigation.gateway.routes.home') as string,
        },
        ...workspaceLink,
        {
          icon: <Icon as={PiBridgeThin} fontSize={'20px'} />,
          href: '/mqtt-bridges',
          label: t('translation:navigation.gateway.routes.bridges') as string,
        },
        {
          icon: <PiPlugsConnectedFill />,
          href: '/protocol-adapters',
          label: t('translation:navigation.gateway.routes.protocolAdapters') as string,
        },
        {
          icon: <MdOutlineEventNote />,
          href: '/event-logs',
          label: t('translation:navigation.gateway.routes.eventLogs') as string,
        },
      ],
    },
    {
      title: t('translation:navigation.extensions.title'),
      items: [
        {
          icon: <HiOutlinePuzzle />,
          href: '/modules',
          isDisabled: true,
          label: t('translation:navigation.extensions.routes.modules') as string,
        },
        {
          icon: <BsIntersect />,
          href: '/namespace',
          label: t('translation:navigation.extensions.routes.namespace') as string,
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
          label: t('translation:navigation.resources.routes.articles') as string,
        },
        {
          icon: <GoLinkExternal />,
          href: data?.gitHubLink?.url as string,
          isExternal: true,
          label: t('translation:navigation.resources.routes.github') as string,
        },
        {
          icon: <GoLinkExternal />,
          href: data?.documentationLink?.url as string,
          isExternal: true,
          label: t('translation:navigation.resources.routes.help') as string,
        },
      ],
    },
  ]

  return { data: menu, isSuccess }
}

export default useGetNavItems
