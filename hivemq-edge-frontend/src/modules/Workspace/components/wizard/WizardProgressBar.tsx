/**
 * Wizard Progress Bar
 *
 * Displays wizard progress at the bottom-center of the canvas.
 * Shows current step, step description, and provides cancel action.
 */

import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, HStack, Text, Button, Icon, Progress, ButtonGroup } from '@chakra-ui/react'
import { CloseIcon, ChevronLeftIcon, ChevronRightIcon } from '@chakra-ui/icons'

import Panel from '@/components/react-flow/Panel.tsx'
import { useWizardState, useWizardActions } from '@/modules/Workspace/hooks/useWizardStore'
import { getStepDescriptionKey, getWizardStep } from './utils/wizardMetadata'

/**
 * Progress bar component for the wizard
 * Only renders when wizard is active
 */
const WizardProgressBar: FC = () => {
  const { t } = useTranslation()
  const { isActive, entityType, currentStep, totalSteps, selectedNodeIds, selectionConstraints } = useWizardState()
  const { cancelWizard, nextStep, previousStep } = useWizardActions()

  // Don't render if wizard is not active
  if (!isActive || !entityType) {
    return null
  }

  const isFirstStep = currentStep === 0
  const isLastStep = currentStep === totalSteps - 1

  // Get the step description key from metadata
  const stepDescriptionKey = getStepDescriptionKey(entityType, currentStep)
  const stepDescription = stepDescriptionKey ? t('workspace.wizard.progress.step', { context: stepDescriptionKey }) : ''

  // Calculate progress percentage
  const progressPercent = totalSteps > 0 ? ((currentStep + 1) / totalSteps) * 100 : 0

  // Check if current step has selection requirements
  const stepConfig = getWizardStep(entityType, currentStep)
  const hasSelectionRequirements = stepConfig?.requiresSelection && selectionConstraints

  // Validate selection if step requires it
  let canProceed = true
  if (hasSelectionRequirements) {
    const { minNodes = 0, maxNodes = Infinity } = selectionConstraints
    const hasMinimum = selectedNodeIds.length >= minNodes
    const withinMaximum = selectedNodeIds.length <= maxNodes
    canProceed = hasMinimum && withinMaximum
  }

  return (
    <Panel
      position="bottom-center"
      data-testid="wizard-progress-bar"
      role="region"
      aria-label={t('workspace.wizard.progress.ariaLabel')}
    >
      <Box
        bg="white"
        _dark={{ bg: 'gray.800' }}
        boxShadow="lg"
        borderRadius="md"
        p={4}
        minW={{ base: '90vw', md: '500px', lg: '600px' }}
        maxW="800px"
      >
        <HStack spacing={4} align="center" justify="space-between">
          <Box flex="1">
            <HStack spacing={2} mb={2}>
              <Text fontWeight="bold" fontSize="sm" color="gray.600" _dark={{ color: 'gray.400' }}>
                {t('workspace.wizard.progress.stepLabel', {
                  current: currentStep + 1,
                  total: totalSteps,
                })}
              </Text>
            </HStack>

            <Progress
              value={Math.round(progressPercent)}
              size="sm"
              colorScheme="blue"
              borderRadius="full"
              mb={2}
              aria-label={t('workspace.wizard.progress.progressAriaLabel', {
                percent: Math.round(progressPercent),
              })}
            />

            {stepDescription && (
              <Text fontSize="sm" color="gray.700" _dark={{ color: 'gray.300' }}>
                {stepDescription}
              </Text>
            )}
          </Box>

          <ButtonGroup spacing={2}>
            {!isFirstStep && (
              <Button
                variant="outline"
                size="sm"
                leftIcon={<Icon as={ChevronLeftIcon} />}
                onClick={previousStep}
                aria-label={t('workspace.wizard.progress.backLabel')}
                data-testid="wizard-back-button"
              >
                {t('workspace.wizard.progress.backLabel')}
              </Button>
            )}

            <Button
              variant="primary"
              size="sm"
              rightIcon={!isLastStep ? <Icon as={ChevronRightIcon} /> : undefined}
              onClick={nextStep}
              isDisabled={!canProceed}
              aria-label={
                isLastStep ? t('workspace.wizard.progress.completeLabel') : t('workspace.wizard.progress.nextLabel')
              }
              data-testid={isLastStep ? 'wizard-complete-button' : 'wizard-next-button'}
            >
              {isLastStep ? t('workspace.wizard.progress.completeLabel') : t('workspace.wizard.progress.nextLabel')}
            </Button>

            <Button
              variant="ghost"
              size="sm"
              leftIcon={<Icon as={CloseIcon} boxSize={3} />}
              onClick={cancelWizard}
              aria-label={t('workspace.wizard.progress.cancelAriaLabel')}
              data-testid="wizard-cancel-button"
            >
              {t('workspace.wizard.progress.cancelLabel')}
            </Button>
          </ButtonGroup>
        </HStack>
      </Box>
    </Panel>
  )
}

export default WizardProgressBar
