import { FC, useState } from 'react'
import {
  Box,
  ButtonGroup,
  HStack,
  Icon,
  IconButton,
  Stack,
  Step,
  StepIndicator,
  StepNumber,
  Stepper,
  StepStatus,
  StepTitle,
  Text,
  useDisclosure,
  useSteps,
  VStack,
} from '@chakra-ui/react'
import { FaAngleLeft, FaAngleRight, FaTools } from 'react-icons/fa'
import { Panel } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { motion } from 'framer-motion'
import { ToolboxNodes } from '@datahub/components/controls/ToolboxNodes.tsx'
import ToolboxDryRun from '@datahub/components/controls/ToolboxDryRun.tsx'
import { MdSkipNext, MdSkipPrevious } from 'react-icons/md'

const steps = [
  { title: 'Build', description: 'Add elements on the canvas' },
  { title: 'Check', description: 'Verify your construction' },
  { title: 'Publish', description: 'Publish your construction' },
]

const DesignerToolbox: FC = () => {
  const { t } = useTranslation('datahub')
  const { getButtonProps, getDisclosureProps, isOpen } = useDisclosure()
  const [hidden, setHidden] = useState(!isOpen)
  const { activeStep, setActiveStep } = useSteps({
    index: 0,
    count: steps.length,
  })

  return (
    <Panel position="top-left" style={{ margin: '5px' }}>
      <HStack alignItems="flex-start">
        <Box>
          <IconButton
            data-testid="toolbox-trigger"
            aria-label={t('workspace.toolbox.trigger', { context: !isOpen ? 'open' : 'close' })}
            icon={
              <>
                <Icon as={FaTools} />
                <Icon as={isOpen ? FaAngleLeft : FaAngleRight} />
              </>
            }
            {...getButtonProps()}
            px={2}
          />
        </Box>
        <motion.div
          {...getDisclosureProps()}
          data-testid="toolbox-container"
          hidden={hidden}
          initial={false}
          onAnimationStart={() => setHidden(false)}
          onAnimationComplete={() => setHidden(!isOpen)}
          animate={{ width: isOpen ? '100%' : 0 }}
          style={{
            overflow: 'hidden',
            whiteSpace: 'nowrap',
          }}
        >
          <Stack>
            <Stepper
              size="md"
              index={activeStep}
              orientation="horizontal"
              gap={2}
              sx={{
                '&[data-orientation=horizontal]': { display: 'flex', alignItems: 'flex-start' },
              }}
            >
              {steps.map((step, index) => (
                <VStack
                  key={index}
                  alignItems="flex-start"
                  p={2}
                  borderWidth={1}
                  bg="var(--chakra-colors-chakra-body-bg)"
                  borderRadius="var(--chakra-radii-base)"
                  boxShadow="var(--chakra-shadows-lg)"
                >
                  <Step as={Box} w="100%">
                    <StepIndicator>
                      <StepStatus complete={<StepNumber />} incomplete={<StepNumber />} active={<StepNumber />} />
                    </StepIndicator>
                    {activeStep === index && (
                      <HStack width="100%">
                        <Box flex={1}>
                          <Text fontWeight="bold">{steps[activeStep]?.description}</Text>
                        </Box>
                        <ButtonGroup size="sm" isAttached>
                          <IconButton
                            aria-label="df"
                            icon={<MdSkipPrevious />}
                            isDisabled={activeStep === 0}
                            onClick={() => setActiveStep((s) => s - 1)}
                          />
                          <IconButton
                            aria-label="df"
                            icon={<MdSkipNext />}
                            isDisabled={activeStep === 2}
                            onClick={() => setActiveStep((s) => s + 1)}
                          />
                        </ButtonGroup>
                      </HStack>
                    )}
                  </Step>
                  <StepTitle as="h2">{step.title}</StepTitle>

                  {activeStep === index && (
                    <>
                      <Box pt={5} h="100%">
                        {activeStep === 0 && <ToolboxNodes />}
                        {activeStep === 1 && <ToolboxDryRun />}
                      </Box>
                    </>
                  )}
                </VStack>
              ))}
            </Stepper>
          </Stack>
        </motion.div>
      </HStack>
    </Panel>
  )
}

export default DesignerToolbox
