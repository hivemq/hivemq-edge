import { FC } from 'react'
import {
  Alert,
  AlertDescription,
  AlertIcon,
  AlertStatus,
  AlertTitle,
  Box,
  CloseButton,
  Link,
  Text,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { ConditionalWrapper } from '@/components/ConditonalWrapper.tsx'
import { PolicyDryRunStatus } from '@datahub/types.ts'

interface PolicySummaryReportProps {
  status: PolicyDryRunStatus
  onOpenPublish?: () => void
  onClearPolicy?: () => void
}

const PolicySummaryReport: FC<PolicySummaryReportProps> = ({ status, onOpenPublish, onClearPolicy }) => {
  const { t } = useTranslation('datahub')
  const alertStatus: AlertStatus = status === PolicyDryRunStatus.SUCCESS ? 'success' : 'warning'

  return (
    <Alert status={alertStatus} data-testid="toolbox-policy-check-status">
      <AlertIcon />
      <Box whiteSpace="normal">
        <AlertTitle>
          <Text as="span">{t('workspace.dryRun.report.success.title', { context: alertStatus })}</Text>
        </AlertTitle>
        <AlertDescription>
          <ConditionalWrapper
            condition={status === PolicyDryRunStatus.SUCCESS}
            wrapper={(children) => (
              <Link aria-label={t('workspace.toolbox.navigation.goPublish') as string} onClick={onOpenPublish}>
                {children}
              </Link>
            )}
          >
            <Text as="span">{t('workspace.dryRun.report.success.description', { context: alertStatus })}</Text>
          </ConditionalWrapper>
        </AlertDescription>
      </Box>
      <CloseButton alignSelf="flex-start" position="relative" right={-1} top={-1} onClick={onClearPolicy} />
    </Alert>
  )
}

export default PolicySummaryReport
