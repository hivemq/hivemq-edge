import type { FC } from 'react'
import { useState } from 'react'
import {
  Box,
  Button,
  Code,
  Collapse,
  HStack,
  Icon,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Text,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { LuChevronDown, LuChevronUp, LuCode2 } from 'react-icons/lu'

import type { PolicyPayload } from '@datahub/types.ts'
import CopyButton from './CopyButton.tsx'

export interface PolicyJsonViewProps {
  payload: PolicyPayload
}

/**
 * Displays the complete JSON payload for the policy and its resources.
 * Provides a collapsible, tabbed interface with syntax highlighting and copy functionality.
 * Collapsed by default to avoid overwhelming users with technical details.
 */
export const PolicyJsonView: FC<PolicyJsonViewProps> = ({ payload }) => {
  const { t } = useTranslation('datahub')
  const [isExpanded, setIsExpanded] = useState(false)

  // Prepare JSON strings for display
  const policyJson = JSON.stringify(payload.policy, null, 2)
  const schemasJson = JSON.stringify(payload.resources.schemas, null, 2)
  const scriptsJson = JSON.stringify(payload.resources.scripts, null, 2)
  const allJson = JSON.stringify(payload, null, 2)

  return (
    <Box borderWidth="1px" borderRadius="md" borderColor="gray.200" data-testid="policy-json-view">
      {/* Toggle Button */}
      <Button
        width="100%"
        onClick={() => setIsExpanded(!isExpanded)}
        rightIcon={<Icon as={isExpanded ? LuChevronUp : LuChevronDown} />}
        variant="ghost"
        justifyContent="space-between"
        borderRadius="md"
        data-testid="json-toggle-button"
        aria-expanded={isExpanded}
        aria-controls="json-payload-content"
      >
        <HStack>
          <Icon as={LuCode2} />
          <Text>
            {isExpanded
              ? t('workspace.dryRun.report.success.details.json.collapse')
              : t('workspace.dryRun.report.success.details.json.expand')}
          </Text>
        </HStack>
      </Button>

      {/* Collapsible Content */}
      <Collapse in={isExpanded} animateOpacity>
        <Box id="json-payload-content" p={4} borderTopWidth="1px" borderColor="gray.200">
          {/* Copy All Button */}
          <HStack justifyContent="flex-end" mb={2}>
            <CopyButton content={allJson} label="Copy All" data-testid="copy-all-button" />
          </HStack>

          {/* Tabbed Interface */}
          <Tabs size="sm" variant="enclosed" data-testid="json-tabs">
            <TabList>
              <Tab data-testid="tab-policy">{t('workspace.dryRun.report.success.details.json.tabs.policy')}</Tab>
              <Tab data-testid="tab-schemas">
                {t('workspace.dryRun.report.success.details.json.tabs.schemas')} ({payload.resources.schemas.length})
              </Tab>
              <Tab data-testid="tab-scripts">
                {t('workspace.dryRun.report.success.details.json.tabs.scripts')} ({payload.resources.scripts.length})
              </Tab>
            </TabList>

            <TabPanels>
              {/* Policy Tab */}
              <TabPanel px={0}>
                <Box position="relative">
                  <Box position="absolute" right={2} top={2} zIndex={1}>
                    <CopyButton
                      content={policyJson}
                      label={t('workspace.dryRun.report.success.details.json.copy')}
                      data-testid="copy-policy-button"
                    />
                  </Box>
                  <Code
                    display="block"
                    whiteSpace="pre"
                    p={4}
                    borderRadius="md"
                    fontSize="xs"
                    maxH="400px"
                    overflowY="auto"
                    bg="gray.50"
                    data-testid="json-policy-content"
                  >
                    {policyJson}
                  </Code>
                </Box>
              </TabPanel>

              {/* Schemas Tab */}
              <TabPanel px={0}>
                <Box position="relative">
                  <Box position="absolute" right={2} top={2} zIndex={1}>
                    <CopyButton
                      content={schemasJson}
                      label={t('workspace.dryRun.report.success.details.json.copy')}
                      data-testid="copy-schemas-button"
                    />
                  </Box>
                  <Code
                    display="block"
                    whiteSpace="pre"
                    p={4}
                    borderRadius="md"
                    fontSize="xs"
                    maxH="400px"
                    overflowY="auto"
                    bg="gray.50"
                    data-testid="json-schemas-content"
                  >
                    {schemasJson}
                  </Code>
                </Box>
              </TabPanel>

              {/* Scripts Tab */}
              <TabPanel px={0}>
                <Box position="relative">
                  <Box position="absolute" right={2} top={2} zIndex={1}>
                    <CopyButton
                      content={scriptsJson}
                      label={t('workspace.dryRun.report.success.details.json.copy')}
                      data-testid="copy-scripts-button"
                    />
                  </Box>
                  <Code
                    display="block"
                    whiteSpace="pre"
                    p={4}
                    borderRadius="md"
                    fontSize="xs"
                    maxH="400px"
                    overflowY="auto"
                    bg="gray.50"
                    data-testid="json-scripts-content"
                  >
                    {scriptsJson}
                  </Code>
                </Box>
              </TabPanel>
            </TabPanels>
          </Tabs>

          {/* Helper Text */}
          <Text fontSize="xs" color="gray.500" mt={3}>
            📋 Complete JSON payload ready for publishing
          </Text>
        </Box>
      </Collapse>
    </Box>
  )
}

export default PolicyJsonView
