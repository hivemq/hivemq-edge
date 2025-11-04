import type { FC } from 'react'
import {
  Badge,
  Card,
  CardBody,
  CardHeader,
  Heading,
  HStack,
  Icon,
  List,
  ListItem,
  Text,
  VStack,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { LuPlus, LuRefreshCw } from 'react-icons/lu'

import { DataHubNodeType, type PolicySummary } from '@datahub/types.ts'
import NodeIcon from '@datahub/components/helpers/NodeIcon.tsx'

export interface PolicyOverviewProps {
  summary: PolicySummary
  /**
   * Color scheme for "new" status badge (default: "blue")
   * Made configurable for easy design changes
   */
  newBadgeColorScheme?: string
  /**
   * Color scheme for "update" status badge (default: "orange")
   * Made configurable for easy design changes
   */
  updateBadgeColorScheme?: string
}

/**
 * Displays an overview of the policy being published, including:
 * - Status badge (New/Update)
 * - Policy type and ID
 * - Key characteristics (topic filters for Data Policies, transitions for Behavior Policies)
 */
export const PolicyOverview: FC<PolicyOverviewProps> = ({
  summary,
  newBadgeColorScheme = 'blue',
  updateBadgeColorScheme = 'orange',
}) => {
  const { t } = useTranslation('datahub')

  const policyTypeLabel = t('workspace.nodes.type', { context: summary.type })
  const statusLabel = summary.isNew
    ? t('workspace.dryRun.report.success.details.policy.status_new')
    : t('workspace.dryRun.report.success.details.policy.status_update')

  return (
    <Card size="sm" data-testid="policy-overview">
      <CardHeader>
        <Heading size="sm">{t('workspace.dryRun.report.success.details.policy.title')}</Heading>
      </CardHeader>
      <CardBody>
        <VStack align="stretch" spacing={4}>
          {/* Status Badge and Policy Type */}
          <HStack justify="space-between">
            <HStack spacing={2}>
              <Badge
                colorScheme={summary.isNew ? newBadgeColorScheme : updateBadgeColorScheme}
                display="inline-flex"
                alignItems="center"
                gap={1}
                data-testid="policy-status-badge"
              >
                <Icon as={summary.isNew ? LuPlus : LuRefreshCw} boxSize="10px" />
                {statusLabel}
              </Badge>
              <Text fontWeight="semibold" data-testid="policy-type">
                {policyTypeLabel}
              </Text>
            </HStack>
            <NodeIcon type={summary.type} data-testid="policy-icon" />
          </HStack>

          {/* Policy ID */}
          <VStack align="stretch" spacing={1}>
            <Text fontSize="sm" fontWeight="medium" color="gray.600">
              {t('workspace.dryRun.report.success.details.policy.id')}
            </Text>
            <Text fontSize="sm" fontFamily="mono" noOfLines={1} title={summary.id} data-testid="policy-id">
              {summary.id}
            </Text>
          </VStack>

          {/* Data Policy: Topic Filters */}
          {summary.type === DataHubNodeType.DATA_POLICY && summary.topicFilters && summary.topicFilters.length > 0 && (
            <VStack align="stretch" spacing={1}>
              <Text fontSize="sm" fontWeight="medium" color="gray.600">
                {t('workspace.dryRun.report.success.details.policy.topicFilters')} ({summary.topicFilters.length})
              </Text>
              <List spacing={1} ml={4} data-testid="topic-filters-list">
                {summary.topicFilters.map((filter, index) => (
                  <ListItem key={index} fontSize="sm" color="gray.600">
                    • {filter}
                  </ListItem>
                ))}
              </List>
            </VStack>
          )}

          {/* Behavior Policy: Transitions */}
          {summary.type === DataHubNodeType.BEHAVIOR_POLICY &&
            summary.transitions &&
            summary.transitions.length > 0 && (
              <VStack align="stretch" spacing={1}>
                <Text fontSize="sm" fontWeight="medium" color="gray.600">
                  {t('workspace.dryRun.report.success.details.policy.transitions')} ({summary.transitions.length})
                </Text>
                <List spacing={1} ml={4} data-testid="transitions-list">
                  {summary.transitions.map((transition, index) => (
                    <ListItem key={index} fontSize="sm" color="gray.600">
                      • {transition}
                    </ListItem>
                  ))}
                </List>
              </VStack>
            )}
        </VStack>
      </CardBody>
    </Card>
  )
}

export default PolicyOverview
