import { Box, Button, Flex } from '@chakra-ui/react'
import { FC } from 'react'
import { SubmitHandler, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'

import { Bridge } from '@/api/__generated__'
import { useCreateBridge } from '@/api/hooks/useGetBridges/useCreateBridge.tsx'
import { ProblemDetails } from '@/api/types/http-problem-details.ts'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import NamePanel from '@/modules/Bridges/components/panels/NamePanel.tsx'

import { useBridgeSetup } from '../../hooks/useBridgeConfig.tsx'

const FinalCreateStep: FC = () => {
  const { t } = useTranslation()
  const { bridge } = useBridgeSetup()
  const { mutateAsync, isError, error: submitError, isLoading } = useCreateBridge()

  const form = useForm<Bridge>({
    defaultValues: bridge,
  })

  const onSubmit: SubmitHandler<Bridge> = (data) => {
    const { id } = data

    mutateAsync({ ...bridge, id }).then(() => undefined)
  }

  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      <Flex alignItems={'center'} flexDirection={'column'} gap={4} mt={20}>
        <Flex>
          <NamePanel form={form} />
        </Flex>
        <Box mt={5}>
          <Button
            type="submit"
            // isDisabled={isNameEmpty || isNameError || isError || isSuccess}
            isLoading={isLoading}
            loadingText={t('bridge.action.submitting')}
          >
            {t('bridge.action.create')}
          </Button>
        </Box>
        {isError && (
          <Box mt={8}>
            <ErrorMessage type={submitError?.message} message={(submitError?.body as ProblemDetails)?.title} />
          </Box>
        )}
      </Flex>
    </form>
  )
}

export default FinalCreateStep
