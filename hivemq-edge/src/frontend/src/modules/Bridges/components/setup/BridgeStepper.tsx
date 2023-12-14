import {
  Box,
  Step,
  StepDescription,
  StepIcon,
  StepIndicator,
  StepNumber,
  StepSeparator,
  StepStatus,
  StepTitle,
  Stepper,
  useDisclosure,
  useSteps,
} from '@chakra-ui/react'
import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import PageContainer from '@/components/PageContainer.tsx'

import { BridgeProvider } from '../../hooks/useBridgeConfig.tsx'
import ConnectionStep from './ConnectionStep.tsx'
import FinalCreateStep from './FinalCreateStep.tsx'
import OptionsStep from './OptionsStep.tsx'
import StepperDebugger from './StepperDebugger.tsx'
import StepperNavigation from './StepperNavigation.tsx'
import SubscriptionStep from './SubscriptionStep.tsx'

interface BridgeStepperProps {
  name?: string
}

const BridgeStepper: FC<BridgeStepperProps> = () => {
  const { t } = useTranslation()
  const navigate = useNavigate()

  const { isOpen, onOpen, onClose } = useDisclosure()
  const steps = [
    {
      title: t('bridge.stepper.source.title'),
      description: t('bridge.stepper.source.description'),
    },
    { title: t('bridge.stepper.local.title'), description: t('bridge.stepper.local.description') },
    {
      title: t('bridge.stepper.remote.title'),
      description: t('bridge.stepper.remote.description'),
    },
    { title: t('bridge.stepper.options.title') },
    { title: t('bridge.stepper.publish.title') },
  ]
  const { activeStep, goToNext, goToPrevious } = useSteps({
    index: 0,
    count: steps.length,
  })

  return (
    <BridgeProvider>
      <PageContainer title={t('bridge.title') as string} subtitle={t('bridge.stepper.description') as string}>
        <Stepper index={activeStep} colorScheme="yellow">
          {steps.map((step, index) => (
            <Step key={`step_${index}`}>
              <StepIndicator>
                <StepStatus complete={<StepIcon />} incomplete={<StepNumber />} active={<StepNumber />} />
              </StepIndicator>

              <Box flexShrink="0">
                <StepTitle>{step.title}</StepTitle>
                <StepDescription>{step.description}</StepDescription>
              </Box>

              <StepSeparator />
            </Step>
          ))}
        </Stepper>
        <Box flexGrow={1}>
          {activeStep === 0 && <ConnectionStep />}
          {activeStep === 1 && <SubscriptionStep type={'local'} />}
          {activeStep === 2 && <SubscriptionStep type={'remote'} />}
          {activeStep === 3 && <OptionsStep />}
          {activeStep === 4 && <FinalCreateStep />}
        </Box>
        <StepperDebugger />
        <StepperNavigation activeStep={activeStep} goToPrevious={goToPrevious} goToNext={goToNext} onCancel={onOpen} />
      </PageContainer>
      <ConfirmationDialog
        isOpen={isOpen}
        onClose={onClose}
        message={t('modals.generics.confirmation')}
        header={t('modals.StepperCancelDialog.header')}
        onSubmit={() => navigate('/mqtt-bridges')}
      />
    </BridgeProvider>
  )
}

export default BridgeStepper
