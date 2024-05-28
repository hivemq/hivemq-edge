import { FC } from 'react'
import {
  Box,
  Stepper,
  Step,
  StepDescription,
  StepIcon,
  StepIndicator,
  StepNumber,
  StepSeparator,
  StepStatus,
  StepTitle,
  VStack,
} from '@chakra-ui/react'
import { BatchModeSteps } from '@/components/rjsf/BatchSubscription/hooks/useBatchModeSteps.tsx'

interface UploadStepper {
  activeStep: number
  steps: BatchModeSteps[]
}

export const UploadStepper: FC<UploadStepper> = ({ steps, activeStep }) => {
  return (
    <VStack>
      <Stepper index={activeStep} w="100%">
        {steps
          .filter((step) => !step.isFinal)
          .map((step, index) => (
            <Step key={index}>
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
      <Box data-testid="stepper-container" minHeight={450} w="100%" p={4}>
        {steps[activeStep]?.renderer}
      </Box>
    </VStack>
  )
}
