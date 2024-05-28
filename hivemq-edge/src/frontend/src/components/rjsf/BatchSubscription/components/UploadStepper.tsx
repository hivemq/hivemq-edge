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
import { BatchModeStep, BatchModeSteps } from '@/components/rjsf/BatchSubscription/hooks/useBatchModeSteps.tsx'
import DataSourceStep from '@/components/rjsf/BatchSubscription/components/DataSourceStep.tsx'
import ColumnMatcherStep from '@/components/rjsf/BatchSubscription/components/ColumnMatcherStep.tsx'
import SubscriptionsValidationStep from '@/components/rjsf/BatchSubscription/components/SubscriptionsValidationStep.tsx'

interface UploadStepper {
  activeStep: number
  steps: BatchModeSteps[]
}

export const UploadStepper: FC<UploadStepper> = ({ steps, activeStep }) => {
  return (
    <VStack>
      <Stepper index={activeStep} w="100%">
        {steps.map((step, index) => (
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
      <Box minHeight={450} w="100%" p={4}>
        {activeStep === BatchModeStep.UPLOAD && <DataSourceStep />}
        {activeStep === BatchModeStep.MATCH && <ColumnMatcherStep />}
        {activeStep === BatchModeStep.VALIDATE && <SubscriptionsValidationStep />}
      </Box>
    </VStack>
  )
}
