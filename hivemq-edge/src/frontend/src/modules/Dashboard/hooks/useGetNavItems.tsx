import { useTranslation } from 'react-i18next'

import { NavLinksBlockType } from '../types.ts'
import { useGetConfiguration } from '@/api/hooks/useGatewayPortal/useGetConfiguration.tsx'
import { BiListUl, BsIntersect, GoLinkExternal, HiOutlinePuzzle, IoHomeOutline, IoLinkOutline } from 'react-icons/all'

const useGetNavItems = (): NavLinksBlockType[] => {
  const { t } = useTranslation()
  const { data } = useGetConfiguration()

  return [
    {
      title: t('translation:navigation.gateway.title'),
      items: [
        {
          icon: <IoHomeOutline />,
          href: '/',
          label: t('translation:navigation.gateway.routes.home') as string,
        },
        {
          icon: <IoLinkOutline />,
          href: '/mqtt-bridges',
          label: t('translation:navigation.gateway.routes.bridges') as string,
        },
        {
          icon: <IoLinkOutline />,
          href: '/protocol-adapters',
          label: t('translation:navigation.gateway.routes.protocolAdapters') as string,
        },
        {
          icon: <BiListUl />,
          href: '/event-logs',
          isDisabled: true,
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
}

export default useGetNavItems
