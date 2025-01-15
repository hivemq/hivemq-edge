import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HStack, VStack } from '@chakra-ui/react'

import config from '@/config'

import { DataHubNodeType, DesignerStatus } from '@datahub/types.ts'
import ToolItem from '@datahub/components/controls/ToolItem.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import ToolGroup from '@datahub/components/controls/ToolGroup.tsx'

interface ToolboxNodesProps {
  direction?: 'horizontal' | 'vertical'
}

export const ToolboxNodes: FC<ToolboxNodesProps> = ({ direction = 'horizontal' }) => {
  const { t } = useTranslation('datahub')
  const { nodes, status, isPolicyInDraft } = useDataHubDraftStore()

  const isEditEnabled = config.features.DATAHUB_EDIT_POLICY_ENABLED || status === DesignerStatus.DRAFT
  const isDraftEmpty = nodes.length === 0

  const Wrapper = direction === 'horizontal' ? HStack : VStack

  return (
    <Wrapper
      pb={2}
      gap={5}
      role="group"
      aria-label={t('workspace.toolbox.aria-label')}
      backgroundColor="var(--chakra-colors-chakra-body-bg)"
      alignItems={direction === 'horizontal' ? 'center' : 'flex-start'}
    >
      <ToolGroup title={t('workspace.toolbox.group.integration.edge')} id="group-integrations">
        <ToolItem nodeType={DataHubNodeType.TOPIC_FILTER} isDisabled={isDraftEmpty || !isEditEnabled} />
        <ToolItem nodeType={DataHubNodeType.CLIENT_FILTER} isDisabled={isDraftEmpty || !isEditEnabled} />
      </ToolGroup>
      <ToolGroup title={t('workspace.toolbox.group.dataPolicy')} id="group-dataPolicy">
        <ToolItem nodeType={DataHubNodeType.DATA_POLICY} isDisabled={!isEditEnabled || isPolicyInDraft()} />
        <ToolItem nodeType={DataHubNodeType.VALIDATOR} isDisabled={isDraftEmpty || !isEditEnabled} />
        <ToolItem nodeType={DataHubNodeType.SCHEMA} isDisabled={isDraftEmpty || !isEditEnabled} />
      </ToolGroup>
      <ToolGroup title={t('workspace.toolbox.group.behaviorPolicy')} id="group-behaviorPolicy">
        <ToolItem nodeType={DataHubNodeType.BEHAVIOR_POLICY} isDisabled={!isEditEnabled || isPolicyInDraft()} />
        <ToolItem nodeType={DataHubNodeType.TRANSITION} isDisabled={isDraftEmpty || !isEditEnabled} />
      </ToolGroup>
      <ToolGroup title={t('workspace.toolbox.group.operation')} id="group-operation">
        <ToolItem nodeType={DataHubNodeType.OPERATION} isDisabled={isDraftEmpty || !isEditEnabled} />
        <ToolItem nodeType={DataHubNodeType.FUNCTION} isDisabled={isDraftEmpty || !isEditEnabled} />
      </ToolGroup>
    </Wrapper>
  )
}
