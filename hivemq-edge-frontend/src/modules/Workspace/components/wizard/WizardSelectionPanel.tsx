/**
 * Wizard Selection Panel
 *
 * Floating panel that displays selected nodes during selection steps.
 * Uses React Flow Panel to avoid blocking the canvas.
 */

import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Panel } from '@xyflow/react'
import {
  Box,
  Heading,
  Text,
  Button,
  VStack,
  Alert,
  AlertIcon,
  Card,
  CardBody,
  CardHeader,
  HStack,
  IconButton,
  Badge,
  List,
  ListItem,
} from '@chakra-ui/react'
import { CloseIcon } from '@chakra-ui/icons'
import { useReactFlow } from '@xyflow/react'

import { useWizardState, useWizardActions } from '@/modules/Workspace/hooks/useWizardStore'
import { NodeTypes } from '@/modules/Workspace/types'

/**
 * Floating panel showing selected nodes with validation and navigation
 * Positioned on the right side but doesn't block canvas interaction
 */
const WizardSelectionPanel: FC = () => {
  const { t } = useTranslation()
  const { isActive, entityType, selectedNodeIds, selectionConstraints } = useWizardState()
  const { nextStep, cancelWizard, deselectNode } = useWizardActions()
  const { getNodes } = useReactFlow()

  // Only show during selection steps
  if (!isActive || !selectionConstraints) {
    return null
  }

  // Get selected node objects
  const selectedNodes = getNodes().filter((n) => selectedNodeIds.includes(n.id))

  // Check if this is Asset Mapper with auto-included Pulse Agent
  const isAssetMapper = entityType === 'ASSET_MAPPER'

  // Extract constraints with defaults
  const { minNodes = 0, maxNodes = Infinity, allowedNodeTypes = [] } = selectionConstraints || {}

  // Validation
  const hasMinimum = selectedNodeIds.length >= minNodes
  const withinMaximum = selectedNodeIds.length <= maxNodes
  const canProceed = hasMinimum && withinMaximum

  // Progress text
  const getProgressText = () => {
    if (maxNodes === Infinity) {
      return `${selectedNodeIds.length} (min: ${minNodes})`
    }
    return `${selectedNodeIds.length} / ${maxNodes}`
  }

  return (
    <Panel position="top-right" style={{ margin: '10px', pointerEvents: 'all' }}>
      <Card minW="320px" maxW="380px" maxH="calc(100vh - 200px)" boxShadow="xl" bg="white" _dark={{ bg: 'gray.800' }}>
        {/* Header */}
        <CardHeader borderBottomWidth="1px" pb={3}>
          <HStack justify="space-between" align="start">
            <VStack align="start" spacing={1} flex={1}>
              <Heading size="sm">{t('workspace.wizard.selection.title')}</Heading>
              <Text fontSize="xs" color="gray.600">
                {t('workspace.wizard.selection.description', { count: minNodes })}
              </Text>
            </VStack>
            <IconButton icon={<CloseIcon />} size="sm" variant="ghost" onClick={cancelWizard} aria-label="Close" />
          </HStack>
        </CardHeader>

        {/* Body - scrollable */}
        <CardBody overflowY="auto" maxH="calc(100vh - 350px)">
          <VStack spacing={3} align="stretch">
            {/* Selection Progress */}
            <HStack justify="space-between">
              <Text fontSize="sm" fontWeight="medium">
                {t('workspace.wizard.selection.selected')}
              </Text>
              <Badge colorScheme={canProceed ? 'green' : 'orange'} fontSize="sm">
                {getProgressText()}
              </Badge>
            </HStack>

            {/* Asset Mapper Info */}
            {isAssetMapper && (
              <Alert status="info" fontSize="sm">
                <AlertIcon />
                <Text fontSize="sm">{t('workspace.wizard.assetMapper.pulseAgentRequired')}</Text>
              </Alert>
            )}

            {/* Empty State */}
            {selectedNodes.length === 0 && (
              <Alert status="info" fontSize="sm">
                <AlertIcon />
                <Text fontSize="sm">{t('workspace.wizard.selection.clickToSelect')}</Text>
              </Alert>
            )}

            {/* Selected Nodes List - Scrollable */}
            {selectedNodes.length > 0 && (
              <List spacing={2} maxH="300px" overflowY="auto">
                {selectedNodes.map((node) => {
                  const isPulseAgent = node.type === NodeTypes.PULSE_NODE
                  const canRemove = !(isAssetMapper && isPulseAgent)

                  return (
                    <ListItem key={node.id}>
                      <Card variant="outline" size="sm">
                        <CardBody py={2} px={3}>
                          <HStack justify="space-between">
                            <VStack align="start" spacing={0} flex={1} minW={0}>
                              <HStack spacing={2}>
                                <Text fontSize="sm" fontWeight="medium" noOfLines={1}>
                                  {String(node.data?.label || node.id)}
                                </Text>
                                {isAssetMapper && isPulseAgent && (
                                  <Badge colorScheme="blue" fontSize="xs">
                                    Required
                                  </Badge>
                                )}
                              </HStack>
                              <Text fontSize="xs" color="gray.500">
                                {node.type?.replace('_NODE', '')}
                              </Text>
                            </VStack>
                            <IconButton
                              icon={<CloseIcon />}
                              size="xs"
                              variant="ghost"
                              onClick={() => deselectNode(node.id)}
                              aria-label={t('workspace.wizard.selection.remove')}
                              flexShrink={0}
                              isDisabled={!canRemove}
                              opacity={canRemove ? 1 : 0.5}
                              cursor={canRemove ? 'pointer' : 'not-allowed'}
                              title={canRemove ? undefined : t('workspace.wizard.assetMapper.pulseAgentRequired')}
                            />
                          </HStack>
                        </CardBody>
                      </Card>
                    </ListItem>
                  )
                })}
              </List>
            )}

            {/* Validation Warnings */}
            {!hasMinimum && selectedNodeIds.length > 0 && (
              <Alert status="warning" fontSize="sm">
                <AlertIcon />
                <Text fontSize="sm">
                  {t('workspace.wizard.selection.minWarning', { count: minNodes - selectedNodeIds.length })}
                </Text>
              </Alert>
            )}

            {!withinMaximum && (
              <Alert status="error" fontSize="sm">
                <AlertIcon />
                <Text fontSize="sm">{t('workspace.wizard.selection.maxExceeded', { count: maxNodes })}</Text>
              </Alert>
            )}

            {/* Helpful Info */}
            {allowedNodeTypes.length > 0 && selectedNodeIds.length === 0 && (
              <Alert status="info" variant="subtle" fontSize="xs">
                <VStack align="start" spacing={1} w="full">
                  <Text fontWeight="medium" fontSize="xs">
                    {t('workspace.wizard.selection.allowedTypes')}
                  </Text>
                  <Text fontSize="xs">{allowedNodeTypes.map((type) => type.replace('_NODE', '')).join(', ')}</Text>
                </VStack>
              </Alert>
            )}
          </VStack>
        </CardBody>

        {/* Footer with Next button */}
        <Box borderTopWidth="1px" p={3}>
          <Button
            variant="primary"
            isDisabled={!canProceed}
            onClick={nextStep}
            width="100%"
            size="sm"
            title={!canProceed ? t('workspace.wizard.selection.selectMoreTooltip', { count: minNodes }) : undefined}
          >
            {t('workspace.wizard.selection.next')}
          </Button>
        </Box>
      </Card>
    </Panel>
  )
}

export default WizardSelectionPanel
