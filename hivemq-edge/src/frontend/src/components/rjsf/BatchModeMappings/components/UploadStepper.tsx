import { FC } from 'react'
import { IdSchema } from '@rjsf/utils'
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
import { BatchModeSteps, BatchModeStore } from '@/components/rjsf/BatchModeMappings/types.ts'

interface UploadStepperProps {
  activeStep: number
  steps: BatchModeSteps[]
  onContinue: (partialStore: Partial<BatchModeStore>) => void
  onBatchUpload?: (idSchema: IdSchema<unknown>, batch: Record<string, unknown>[]) => void
  onClose?: () => void
  store: BatchModeStore
}

export const UploadStepper: FC<UploadStepperProps> = ({
  store,
  steps,
  activeStep,
  onContinue,
  onBatchUpload,
  onClose,
}) => {
  const Component = steps[activeStep]?.renderer

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
        <Component onContinue={onContinue} store={store} onBatchUpload={onBatchUpload} onClose={onClose} />
      </Box>
    </VStack>
  )
}
