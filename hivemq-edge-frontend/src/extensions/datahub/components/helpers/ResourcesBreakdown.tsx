import type { FC } from 'react'
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Badge,
  Box,
  HStack,
  Icon,
  Text,
  VStack,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { LuFileJson, LuFileCode, LuPlus, LuRefreshCw } from 'react-icons/lu'

import type { ResourceSummary } from '@datahub/types.ts'
import { groupResourcesByType } from '@datahub/utils/policy-summary.utils.ts'

export interface ResourcesBreakdownProps {
  resources: ResourceSummary[]
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
 * Displays a breakdown of resources (schemas and scripts) being created or modified.
 * Uses an accordion pattern similar to PolicyErrorReport for consistency.
 */
export const ResourcesBreakdown: FC<ResourcesBreakdownProps> = ({
  resources,
  newBadgeColorScheme = 'blue',
  updateBadgeColorScheme = 'orange',
}) => {
  const { t } = useTranslation('datahub')
  const { schemas, scripts } = groupResourcesByType(resources)

  // Empty state
  if (resources.length === 0) {
    return (
      <Box
        p={4}
        borderWidth="1px"
        borderRadius="md"
        borderColor="gray.200"
        bg="gray.50"
        data-testid="resources-empty-state"
      >
        <Text fontSize="sm" color="gray.600" textAlign="center">
          {t('workspace.dryRun.report.success.details.resources.empty')}
        </Text>
      </Box>
    )
  }

  const statusLabel = (isNew: boolean) =>
    isNew
      ? t('workspace.dryRun.report.success.details.resources.status_new')
      : t('workspace.dryRun.report.success.details.resources.status_update')

  return (
    <Box data-testid="resources-breakdown">
      <Accordion allowMultiple defaultIndex={[0, 1]}>
        {/* Schemas Section */}
        {schemas.length > 0 && (
          <AccordionItem borderColor="gray.200">
            <h2>
              <AccordionButton
                aria-label={t('workspace.dryRun.report.success.details.resources.schemas_count', {
                  count: schemas.length,
                })}
              >
                <Box flex="1" textAlign="left" fontWeight="medium" data-testid="schemas-header">
                  {t('workspace.dryRun.report.success.details.resources.schemas_count', { count: schemas.length })}
                </Box>
                <AccordionIcon />
              </AccordionButton>
            </h2>
            <AccordionPanel pb={4}>
              <VStack align="stretch" spacing={3} data-testid="schemas-list">
                {schemas.map((schema) => (
                  <HStack key={schema.id} spacing={3}>
                    <Icon as={LuFileJson} color="purple.500" boxSize="20px" flexShrink={0} />
                    <VStack align="stretch" spacing={0} flex={1}>
                      <Text fontWeight="medium" fontSize="sm" data-testid={`schema-${schema.id}`}>
                        {schema.id}
                      </Text>
                      <HStack fontSize="xs" color="gray.600" spacing={2}>
                        <Badge
                          size="sm"
                          colorScheme={schema.isNew ? newBadgeColorScheme : updateBadgeColorScheme}
                          display="inline-flex"
                          alignItems="center"
                          gap={1}
                        >
                          <Icon as={schema.isNew ? LuPlus : LuRefreshCw} boxSize="8px" />
                          {statusLabel(schema.isNew)}
                        </Badge>
                        <Text>v{schema.version}</Text>
                        <Text>•</Text>
                        <Text>{schema.metadata.schemaType}</Text>
                      </HStack>
                    </VStack>
                  </HStack>
                ))}
              </VStack>
            </AccordionPanel>
          </AccordionItem>
        )}

        {/* Scripts Section */}
        {scripts.length > 0 && (
          <AccordionItem borderColor="gray.200">
            <h2>
              <AccordionButton
                aria-label={t('workspace.dryRun.report.success.details.resources.scripts_count', {
                  count: scripts.length,
                })}
              >
                <Box flex="1" textAlign="left" fontWeight="medium" data-testid="scripts-header">
                  {t('workspace.dryRun.report.success.details.resources.scripts_count', { count: scripts.length })}
                </Box>
                <AccordionIcon />
              </AccordionButton>
            </h2>
            <AccordionPanel pb={4}>
              <VStack align="stretch" spacing={3} data-testid="scripts-list">
                {scripts.map((script) => (
                  <HStack key={script.id} spacing={3}>
                    <Icon as={LuFileCode} color="orange.500" boxSize="20px" flexShrink={0} />
                    <VStack align="stretch" spacing={0} flex={1}>
                      <Text fontWeight="medium" fontSize="sm" data-testid={`script-${script.id}`}>
                        {script.id}
                      </Text>
                      <HStack fontSize="xs" color="gray.600" spacing={2}>
                        <Badge
                          size="sm"
                          colorScheme={script.isNew ? newBadgeColorScheme : updateBadgeColorScheme}
                          display="inline-flex"
                          alignItems="center"
                          gap={1}
                        >
                          <Icon as={script.isNew ? LuPlus : LuRefreshCw} boxSize="8px" />
                          {statusLabel(script.isNew)}
                        </Badge>
                        <Text>v{script.version}</Text>
                        <Text>•</Text>
                        <Text>{script.metadata.functionType}</Text>
                      </HStack>
                    </VStack>
                  </HStack>
                ))}
              </VStack>
            </AccordionPanel>
          </AccordionItem>
        )}
      </Accordion>
    </Box>
  )
}

export default ResourcesBreakdown
