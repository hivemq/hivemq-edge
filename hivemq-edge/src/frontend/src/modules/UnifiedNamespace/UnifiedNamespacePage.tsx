import { Box, Flex, SimpleGrid } from '@chakra-ui/react'
import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Outlet, useNavigate } from 'react-router-dom'

import { mockISA95ApiBean } from '@/api/hooks/useUnifiedNamespace/__handlers__'
import { useGetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useGetUnifiedNamespace.tsx'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'
import ButtonCTA from '@/components/Chakra/ButtonCTA.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import PageContainer from '@/components/PageContainer.tsx'
import InfoPanel from '@/modules/UnifiedNamespace/components/panels/InfoPanel.tsx'
import PrefixPanel from '@/modules/UnifiedNamespace/components/panels/PrefixPanel.tsx'
import RecommendationPanel from '@/modules/UnifiedNamespace/components/panels/RecommendationPanel.tsx'
import { BiPlus } from 'react-icons/bi'

const UnifiedNamespacePage: FC = () => {
  const { t } = useTranslation()
  const { data, isError, isLoading, error } = useGetUnifiedNamespace()
  const navigate = useNavigate()

  const safeData = data ? data : mockISA95ApiBean

  // if (isError) {
  //   return (
  //     <Box mt={8}>
  //       <ErrorMessage type={error?.message} message={(error?.body as ProblemDetails)?.title} />
  //     </Box>
  //   )
  // }
  //
  // if (isLoading || !data) {
  //   return (
  //     <Flex flexDirection={'row'} flexWrap={'wrap'} gap={'20px'}>
  //       <Skeleton width={250} height={100}></Skeleton>
  //     </Flex>
  //   )
  // }

  return (
    <PageContainer
      title={t('unifiedNamespace.title') as string}
      subtitle={t('unifiedNamespace.description') as string}
      cta={
        <Flex height={'100%'} justifyContent={'flex-end'} alignItems={'flex-end'} pb={6}>
          <ButtonCTA
            leftIcon={<BiPlus />}
            onClick={() => navigate('/namespace/edit')}
            isDisabled={isLoading || isError}
          >
            {t('unifiedNamespace.action.define')}
          </ButtonCTA>
        </Flex>
      }
    >
      {isError && (
        <Box mt={'20%'} mx={'20%'} alignItems={'center'}>
          <ErrorMessage
            type={error?.message}
            message={(error?.body as ProblemDetails)?.title || (t('unifiedNamespace.error.loading') as string)}
          />
        </Box>
      )}
      {!isError && (
        <>
          <SimpleGrid mt={8} spacing={6} templateColumns={{ base: 'repeat(1, 1fr)', lg: 'repeat(2, 1fr)' }} gap={6}>
            <InfoPanel />
            <SimpleGrid spacing={4} templateRows={{ base: 'repeat(1, 1fr)' }} gap={6}>
              <RecommendationPanel />
              <PrefixPanel data={safeData} isLoading={isLoading} />
            </SimpleGrid>
          </SimpleGrid>
          <Outlet />
        </>
      )}
    </PageContainer>
  )
}

export default UnifiedNamespacePage
