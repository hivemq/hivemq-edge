import { FC } from 'react'
import { Alert, AlertDescription, AlertIcon, AlertTitle, Button, VStack } from '@chakra-ui/react'
import { StepRendererProps } from '@/components/rjsf/BatchModeMappings/types.ts'
import { useTranslation } from 'react-i18next'

const ConfirmStep: FC<StepRendererProps> = ({ onBatchUpload, onClose, onContinue, store }) => {
  const { t } = useTranslation('components')

  const handleConfirm = () => {
    if (store.subscriptions) {
      const subscriptions = store.subscriptions.map(({ errors, isError, row, ...subs }) => subs)
      onBatchUpload?.(store.idSchema, subscriptions)
      // Confusing. Get a "clear" method instead or change the onClose
      onContinue({ fileName: undefined, subscriptions: undefined, mapping: undefined, worksheet: undefined })
      onClose?.()
    }
  }

  const isBundleValid = store.subscriptions && store.subscriptions.length > 0
  return (
    <VStack minHeight="calc(450px - 2rem)" display="flex" justifyContent="space-evenly" alignItems="center">
      <Alert
        status={isBundleValid ? 'success' : 'error'}
        variant="subtle"
        flexDirection="column"
        alignItems="center"
        justifyContent="center"
        textAlign="center"
        height="200px"
      >
        <AlertIcon boxSize="40px" mr={0} />
        <AlertTitle mt={4} mb={1} fontSize="lg" data-testid="batch-confirm-title">
          {t('rjsf.batchUpload.confirm.alert.title', { count: store.subscriptions?.length || 0 })}
        </AlertTitle>
        <AlertDescription maxWidth="sm" data-testid="batch-confirm-description">
          {t('rjsf.batchUpload.confirm.alert.description', { count: store.subscriptions?.length || 0 })}
        </AlertDescription>
      </Alert>

      <Button variant="primary" onClick={handleConfirm} isDisabled={!isBundleValid} data-testid="batch-confirm-submit">
        {t('rjsf.batchUpload.confirm.action.upload')}
      </Button>
    </VStack>
  )
}

export default ConfirmStep
