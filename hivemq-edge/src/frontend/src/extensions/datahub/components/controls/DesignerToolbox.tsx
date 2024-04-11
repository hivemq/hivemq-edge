import { FC, useState } from 'react'
import { Panel } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { motion } from 'framer-motion'
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
import { FaTools } from 'react-icons/fa'
import { LuPanelLeftOpen, LuPanelRightOpen, LuSkipBack, LuSkipForward } from 'react-icons/lu'

import { ToolboxNodes } from '@datahub/components/controls/ToolboxNodes.tsx'
import { ToolboxDryRun } from '@datahub/components/controls/ToolboxDryRun.tsx'
import { ToolboxPublish } from '@datahub/components/controls/ToolboxPublish.tsx'
import DraftStatus from '@datahub/components/helpers/DraftStatus.tsx'

const stepKeys = ['build', 'check', 'publish']

const DesignerToolbox: FC = () => {
  const { t } = useTranslation('datahub')
  const { getButtonProps, getDisclosureProps, isOpen } = useDisclosure()
  const [hidden, setHidden] = useState(!isOpen)
  const { activeStep, setActiveStep } = useSteps({
    index: 0,
    count: stepKeys.length,
  })

  const steps = stepKeys.map((key) => ({
    key: key,
    title: t(`workspace.toolbox.panel.${key}.title`),
    description: t(`workspace.toolbox.panel.${key}.description`),
  }))

  return (
    <Panel position="top-left" style={{ margin: '4px' }}>
      <HStack alignItems="flex-start" userSelect="none">
        <Box>
          <IconButton
            data-testid="toolbox-trigger"
            aria-label={t('workspace.toolbox.trigger', { context: !isOpen ? 'open' : 'close' })}
            icon={
              <>
                <Icon as={FaTools} />
                <Icon as={isOpen ? LuPanelRightOpen : LuPanelLeftOpen} ml={2} boxSize="24px" />
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
                  data-testid={`toolbox-step-${step.key}`}
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
                            data-testid="toolbox-navigation-prev"
                            aria-label={t('workspace.toolbox.navigation.previous')}
                            icon={<LuSkipBack />}
                            isDisabled={activeStep === 0}
                            onClick={() => setActiveStep((s) => s - 1)}
                          />
                          <IconButton
                            data-testid="toolbox-navigation-next"
                            aria-label={t('workspace.toolbox.navigation.next')}
                            icon={<LuSkipForward />}
                            isDisabled={activeStep === 2}
                            onClick={() => setActiveStep((s) => s + 1)}
                          />
                        </ButtonGroup>
                      </HStack>
                    )}
                  </Step>
                  {activeStep !== index && <StepTitle as="h2">{step.title}</StepTitle>}

                  {activeStep === index && (
                    <>
                      <Box pt={5} h="100%">
                        {activeStep === 0 && <ToolboxNodes />}
                        {activeStep === 1 && <ToolboxDryRun />}
                        {activeStep === 2 && <ToolboxPublish />}
                      </Box>
                    </>
                  )}
                </VStack>
              ))}
            </Stepper>
          </Stack>
        </motion.div>
        <DraftStatus />
      </HStack>
    </Panel>
  )
}

export default DesignerToolbox
