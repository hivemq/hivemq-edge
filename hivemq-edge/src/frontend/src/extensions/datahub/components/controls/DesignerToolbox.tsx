import { FC, useState } from 'react'
import { Panel, useReactFlow } from 'reactflow'
import { useLocation, useNavigate } from 'react-router-dom'
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
import { ANIMATION } from '@datahub/utils/datahub.utils.ts'

const stepKeys = ['build', 'check', 'publish']

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace DesignerToolBoxProps {
  export enum Steps {
    TOOLBOX_NODES = 0,
    TOOLBOX_CHECK = 1,
    TOOLBOX_PUBLISH = 2,
  }
}

export interface DesignerToolBoxProps {
  onActiveStep?: (step: DesignerToolBoxProps.Steps) => void
}

const DesignerToolbox: FC = () => {
  const { t } = useTranslation('datahub')
  const { fitView } = useReactFlow()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { getButtonProps, getDisclosureProps, isOpen } = useDisclosure()
  const [hidden, setHidden] = useState(!isOpen)
  const { activeStep, setActiveStep } = useSteps({
    index: DesignerToolBoxProps.Steps.TOOLBOX_NODES,
    count: stepKeys.length,
  })

  const onActiveStep = (step: number) => {
    setActiveStep(step)
  }

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
                            isDisabled={activeStep === DesignerToolBoxProps.Steps.TOOLBOX_NODES}
                            onClick={() => setActiveStep((s) => s - 1)}
                          />
                          <IconButton
                            data-testid="toolbox-navigation-next"
                            aria-label={t('workspace.toolbox.navigation.next')}
                            icon={<LuSkipForward />}
                            isDisabled={activeStep === DesignerToolBoxProps.Steps.TOOLBOX_PUBLISH}
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
                        {activeStep === DesignerToolBoxProps.Steps.TOOLBOX_NODES && <ToolboxNodes />}
                        {activeStep === DesignerToolBoxProps.Steps.TOOLBOX_CHECK && (
                          <ToolboxDryRun
                            onActiveStep={onActiveStep}
                            onShowNode={(node) =>
                              fitView({ nodes: [node], padding: 3, duration: ANIMATION.FIT_VIEW_DURATION_MS })
                            }
                            onShowEditor={(node) =>
                              navigate(`node/${node.type}/${node.id}`, { state: { origin: pathname } })
                            }
                          />
                        )}
                        {activeStep === DesignerToolBoxProps.Steps.TOOLBOX_PUBLISH && (
                          <ToolboxPublish onActiveStep={onActiveStep} />
                        )}
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
