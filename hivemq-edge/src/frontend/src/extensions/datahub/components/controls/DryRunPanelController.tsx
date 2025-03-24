import { useCallback, useEffect } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { Node } from 'reactflow'
import { useReactFlow } from 'reactflow'
import {
  Button,
  Card,
  CardBody,
  CardHeader,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  useDisclosure,
} from '@chakra-ui/react'

import PolicySummaryReport from '@datahub/components/helpers/PolicySummaryReport.tsx'
import PolicyErrorReport from '@datahub/components/helpers/PolicyErrorReport.tsx'
import { ToolbarPublish } from '@datahub/components/toolbar/ToolbarPublish'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { PolicyDryRunStatus } from '@datahub/types.ts'

import { ANIMATION } from '@/modules/Theme/utils.ts'

const DryRunPanelController = () => {
  const { t } = useTranslation('datahub')
  const { policyType } = useParams()
  const { state } = useLocation()
  const navigate = useNavigate()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { fitView } = useReactFlow()
  const { nodes } = useDataHubDraftStore()
  const { status, getErrors } = usePolicyChecksStore()

  const errorNodeFrom = useCallback((id: string) => nodes.find((node) => node.id === id), [nodes])

  useEffect(() => {
    if (policyType) {
      onOpen()
    }
  }, [onOpen, policyType])

  const onDrawerClose = useCallback(() => {
    onClose()
    navigate(state?.origin || '/datahub')
  }, [onClose, navigate, state?.origin])

  const onShowNode = (node: Node) => {
    fitView({ nodes: [node], padding: 3, duration: ANIMATION.FIT_VIEW_DURATION_MS })
  }

  const onShowEditor = (node: Node) => {
    navigate(`../node/${node.type}/${node.id}`, { state: { origin: state?.origin } })
  }

  return (
    <Drawer size="sm" isOpen={isOpen} placement="right" onClose={onDrawerClose}>
      <DrawerContent data-testid="policy-validity-report" aria-label={t('workspace.dryRun.report.header')}>
        <DrawerCloseButton />
        <DrawerHeader>{t('workspace.dryRun.report.header')}</DrawerHeader>

        <DrawerBody>
          <Card size="sm">
            <CardHeader>
              <PolicySummaryReport status={status} />
            </CardHeader>
            <CardBody>
              <PolicyErrorReport
                errors={getErrors() || []}
                onFitView={(id) => {
                  const errorNode = errorNodeFrom(id)
                  if (errorNode) onShowNode(errorNode)
                }}
                onOpenConfig={(id) => {
                  const errorNode = errorNodeFrom(id)
                  if (errorNode) onShowEditor(errorNode)
                }}
              />
            </CardBody>
          </Card>
        </DrawerBody>
        <DrawerFooter justifyContent="space-between">
          {status === PolicyDryRunStatus.SUCCESS && <ToolbarPublish />}
          <Button onClick={onDrawerClose}>{t('workspace.dryRun.report.cta.close')}</Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default DryRunPanelController
