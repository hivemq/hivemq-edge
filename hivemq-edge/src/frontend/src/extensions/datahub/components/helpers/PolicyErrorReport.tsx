import { FC } from 'react'
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Button,
  ButtonGroup,
  Text,
  VStack,
} from '@chakra-ui/react'
import { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'
import { useTranslation } from 'react-i18next'

interface PolicyErrorReportProps {
  errors: ProblemDetailsExtended[]
  onFitView?: (id: string) => void
  onOpenConfig?: (id: string) => void
}

const PolicyErrorReport: FC<PolicyErrorReportProps> = ({ errors, onFitView, onOpenConfig }) => {
  const { t } = useTranslation('datahub')

  return (
    <Accordion allowMultiple>
      {errors.map((problem, i) => {
        const { id, title, detail } = problem
        // ProblemDetailsExtended doesn't type the extensions!
        const nodeId = problem.id as string
        return (
          <AccordionItem key={`${id}-item${i}`} borderColor="blackAlpha.500">
            <h2>
              <AccordionButton>
                <Box as="span" flex="1" textAlign="left">
                  {t('workspace.nodes.type', { context: title })}
                </Box>
                <AccordionIcon />
              </AccordionButton>
            </h2>
            <AccordionPanel pb={4}>
              <VStack alignItems="flex-start">
                <Text whiteSpace="normal">{detail}</Text>
                <ButtonGroup alignItems="flex-start" size="sm">
                  <Button onClick={() => onFitView?.(nodeId)} data-testid="report-error-fitView">
                    {t('workspace.dryRun.report.cta.highlight')}
                  </Button>
                  <Button onClick={() => onOpenConfig?.(nodeId)} data-testid="report-error-config">
                    {t('workspace.dryRun.report.cta.config')}
                  </Button>
                </ButtonGroup>
              </VStack>
            </AccordionPanel>
          </AccordionItem>
        )
      })}
    </Accordion>
  )
}

export default PolicyErrorReport
