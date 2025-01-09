import { FC } from 'react'
import { Alert, AlertDescription, AlertIcon, AlertStatus, AlertTitle, Box, Text } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { PolicyDryRunStatus } from '@datahub/types.ts'

interface PolicySummaryReportProps {
  status: PolicyDryRunStatus | undefined
  onOpenPublish?: () => void
  onClearPolicy?: () => void
}

const PolicySummaryReport: FC<PolicySummaryReportProps> = ({ status }) => {
  const { t } = useTranslation('datahub')
  const alertStatus: AlertStatus =
    status === PolicyDryRunStatus.SUCCESS ? 'success' : status === PolicyDryRunStatus.FAILURE ? 'warning' : 'error'

  return (
    <Alert status={alertStatus} data-testid="toolbox-policy-check-status">
      <AlertIcon />
      <Box whiteSpace="normal">
        <AlertTitle>
          <Text as="span">{t('workspace.dryRun.report.success.title', { context: alertStatus })}</Text>
        </AlertTitle>
        <AlertDescription>
          <Text as="span">{t('workspace.dryRun.report.success.description', { context: alertStatus })}</Text>
        </AlertDescription>
      </Box>
    </Alert>
  )
}

export default PolicySummaryReport
