import { FC, useMemo } from 'react'
import { Box, Flex, SimpleGrid } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'

import BridgeEmptyLogo from '@/assets/app/bridge-empty.svg'

import { ProblemDetails } from '@/api/types/http-problem-details.ts'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.tsx'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'

import BridgeCard from '@/modules/Bridges/components/overview/BridgeCard.tsx'

const Bridges: FC = () => {
  const { data, isLoading, isError, error } = useListBridges()
  const { t } = useTranslation()
  const isEmpty = useMemo(() => !data || data.length === 0, [data])
  const navigate = useNavigate()

  const handleNavigate = (route: string) => {
    navigate(route)
  }

  if (isError) {
    return (
      <Box mt={'20%'} mx={'20%'} alignItems={'center'}>
        <ErrorMessage
          type={error?.message}
          message={(error?.body as ProblemDetails)?.title || (t('bridge.error.loading') as string)}
        />
      </Box>
    )
  }

  if (isLoading) {
    return (
      <Flex mt={8} flexDirection={'row'} flexWrap={'wrap'} gap={'20px'}>
        <BridgeCard {...mockBridge} isLoading />
      </Flex>
    )
  }
  if (isEmpty)
    return (
      <WarningMessage
        image={BridgeEmptyLogo}
        prompt={t('bridge.noDataWarning.description')}
        title={t('bridge.noDataWarning.title') as string}
        alt={t('bridge.title')}
        mt={10}
      />
    )

  return (
    <SimpleGrid
      mt={8}
      spacing={4}
      templateColumns={{ base: 'repeat(1, 1fr)', lg: 'repeat(2, 1fr)', '2xl': 'repeat(3, 1fr)' }}
      gap={6}
      role={'list'}
      aria-label={t('bridge.list') as string}
    >
      {data?.map((bridge, i) => (
        <BridgeCard key={`${bridge.id}-${i}`} {...bridge} onNavigate={handleNavigate} role={'listitem'} />
      ))}
    </SimpleGrid>
  )
}

export default Bridges
