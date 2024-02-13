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

interface CheckPolicyReportToastProps {
  errors: ProblemDetailsExtended[]
  onFitView?: (id: string) => void
  openConfig?: (id: string) => void
}

const CheckPolicyReportToast: FC<CheckPolicyReportToastProps> = ({ errors, onFitView, openConfig }) => {
  const { t } = useTranslation('datahub')
  const status = errors.length ? 'warning' : 'success'

  return (
    <>
      <Box as="p" mb={2}>
        <Text as="span" fontWeight="bold">
          {t('workspace.dryRun.report.title', { context: status })}
        </Text>{' '}
        <Text as="span">{t('workspace.dryRun.report.subtitle', { context: status })}</Text>
      </Box>

      <Accordion allowToggle overflow="auto" maxHeight="180px">
        {errors.map((problem) => {
          const { id, title, detail } = problem
          return (
            <AccordionItem key={id as string} borderColor="blackAlpha.500">
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
                  <Text>{detail}</Text>
                  <ButtonGroup alignItems="flex-start" size="sm">
                    <Button onClick={() => onFitView?.(id as string)}>
                      {t('workspace.dryRun.report.cta.highlight')}
                    </Button>
                    <Button onClick={() => openConfig?.(id as string)}>
                      {t('workspace.dryRun.report.cta.config')}
                    </Button>
                  </ButtonGroup>
                </VStack>
              </AccordionPanel>
            </AccordionItem>
          )
        })}
      </Accordion>
    </>
  )
}

export default CheckPolicyReportToast
