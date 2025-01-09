import { useCallback, useEffect } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useReactFlow, Node } from 'reactflow'
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

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import PolicySummaryReport from '@datahub/components/helpers/PolicySummaryReport.tsx'
import PolicyErrorReport from '@datahub/components/helpers/PolicyErrorReport.tsx'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { PolicyDryRunStatus } from '@datahub/types.ts'

import { ANIMATION } from '@datahub/utils/datahub.utils.ts'
import { ToolbarPublish } from '@datahub/components/toolbar/ToolbarPublish.tsx'
import { useTranslation } from 'react-i18next'

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
      <DrawerContent data-testid="node-editor-content">
        <DrawerCloseButton />
        <DrawerHeader>Policy Dry Run</DrawerHeader>

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
        <DrawerFooter borderTopWidth="1px"> {status === PolicyDryRunStatus.SUCCESS && <ToolbarPublish />}</DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default DryRunPanelController
