import { FC } from 'react'
import { useTranslation } from 'react-i18next'

import PageContainer from '@/components/PageContainer.tsx'

import NamespaceForm from './components/NamespaceForm.tsx'
import { useGetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useGetUnifiedNamespace.tsx'
import { Box, Flex, Skeleton, Text, useToast, UseToastOptions } from '@chakra-ui/react'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { ProblemDetails, ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'
import { SubmitHandler } from 'react-hook-form'
import { ApiError, ISA95ApiBean } from '@/api/__generated__'
import { useSetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useSetUnifiedNamespace.tsx'

const DEFAULT_TOAST_OPTION: UseToastOptions = {
  status: 'success',
  duration: 3000,
  isClosable: true,
  position: 'top-right',
}

const UnifiedNamespacePage: FC = () => {
  const { t } = useTranslation()
  const { data, isError, isLoading, error } = useGetUnifiedNamespace()
  const setNamespace = useSetUnifiedNamespace()
  const createToast = useToast()

  const handleOnSubmit: SubmitHandler<ISA95ApiBean> = (data) => {
    setNamespace
      .mutateAsync({ requestBody: data })
      .then(() => {
        createToast({
          ...DEFAULT_TOAST_OPTION,
          title: t('unifiedNamespace.toast.update.title'),
          description: t('unifiedNamespace.toast.update.description'),
        })
      })
      .catch((err: ApiError) => {
        const { body } = err
        createToast({
          ...DEFAULT_TOAST_OPTION,
          status: 'error',
          title: t('unifiedNamespace.toast.update.title'),
          description: (
            <>
              <Text>{t('bridge.toast.update.error')}</Text>
              {body.errors?.map((e: ProblemDetailsExtended) => (
                <Text key={e.fieldName as string}>
                  {e.fieldName as string} : {e.detail}
                </Text>
              ))}
            </>
          ),
        })
      })
  }

  if (isError) {
    return (
      <Box mt={8}>
        <ErrorMessage type={error?.message} message={(error?.body as ProblemDetails)?.title} />
      </Box>
    )
  }

  if (isLoading || !data) {
    return (
      <Flex flexDirection={'row'} flexWrap={'wrap'} gap={'20px'}>
        <Skeleton width={250} height={100}></Skeleton>
      </Flex>
    )
  }

  return (
    <PageContainer title={t('unifiedNamespace.title') as string} subtitle={t('unifiedNamespace.description') as string}>
      <NamespaceForm defaultValues={data} onSubmit={handleOnSubmit} />
    </PageContainer>
  )
}

export default UnifiedNamespacePage
