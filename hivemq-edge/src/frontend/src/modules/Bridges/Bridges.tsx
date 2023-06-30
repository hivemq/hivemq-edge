import { FC, useMemo } from 'react'
import { Box, Flex, SimpleGrid, Skeleton } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import BridgeEmptyLogo from '@/assets/app/bridge-empty.svg'

import { ProblemDetails } from '@/api/types/http-problem-details.ts'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.tsx'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import ErrorMessage from '@/components/ErrorMessage.tsx'

import BridgeCard from '@/modules/Bridges/components/overview/BridgeCard.tsx'
import WarningMessage from '@/components/WarningMessage.tsx'

const Bridges: FC = () => {
  const { data, isLoading, isError, error } = useListBridges()
  const { t } = useTranslation()
  const isEmpty = useMemo(() => !data || data.length === 0, [data])

  if (isError) {
    return (
      <Box mt={8}>
        <ErrorMessage type={error?.message} message={(error?.body as ProblemDetails)?.title} />
      </Box>
    )
  }
  if (isLoading) {
    return (
      <Flex flexDirection={'row'} flexWrap={'wrap'} gap={'20px'}>
        <Skeleton>
          <BridgeCard {...mockBridge} />
        </Skeleton>
      </Flex>
    )
  }
  if (isEmpty)
    return (
      <WarningMessage image={BridgeEmptyLogo} prompt={t('bridge.noDataWarning.description')} alt={t('bridge.title')} />
    )

  return (
    <SimpleGrid mt={8} spacing={4} templateColumns={{ base: 'repeat(1, 1fr)', lg: 'repeat(2, 1fr)' }} gap={6}>
      {data?.map((bridge, i) => (
        <BridgeCard key={`${bridge.id}-${i}`} {...bridge} />
      ))}
    </SimpleGrid>
  )
}

export default Bridges
