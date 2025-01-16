import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HStack, VStack } from '@chakra-ui/react'

import { DataHubNodeType } from '@datahub/types.ts'
import ToolItem from '@datahub/components/controls/ToolItem.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import ToolGroup from '@datahub/components/controls/ToolGroup.tsx'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

interface ToolboxNodesProps {
  direction?: 'horizontal' | 'vertical'
}

export const ToolboxNodes: FC<ToolboxNodesProps> = ({ direction = 'horizontal' }) => {
  const { t } = useTranslation('datahub')
  const { nodes, isPolicyInDraft } = useDataHubDraftStore()
  const { isPolicyEditable } = usePolicyGuards()

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
        <ToolItem nodeType={DataHubNodeType.TOPIC_FILTER} isDisabled={isDraftEmpty || !isPolicyEditable} />
        <ToolItem nodeType={DataHubNodeType.CLIENT_FILTER} isDisabled={isDraftEmpty || !isPolicyEditable} />
      </ToolGroup>
      <ToolGroup title={t('workspace.toolbox.group.dataPolicy')} id="group-dataPolicy">
        <ToolItem nodeType={DataHubNodeType.DATA_POLICY} isDisabled={!isPolicyEditable || isPolicyInDraft()} />
        <ToolItem nodeType={DataHubNodeType.VALIDATOR} isDisabled={isDraftEmpty || !isPolicyEditable} />
        <ToolItem nodeType={DataHubNodeType.SCHEMA} isDisabled={isDraftEmpty || !isPolicyEditable} />
      </ToolGroup>
      <ToolGroup title={t('workspace.toolbox.group.behaviorPolicy')} id="group-behaviorPolicy">
        <ToolItem nodeType={DataHubNodeType.BEHAVIOR_POLICY} isDisabled={!isPolicyEditable || isPolicyInDraft()} />
        <ToolItem nodeType={DataHubNodeType.TRANSITION} isDisabled={isDraftEmpty || !isPolicyEditable} />
      </ToolGroup>
      <ToolGroup title={t('workspace.toolbox.group.operation')} id="group-operation">
        <ToolItem nodeType={DataHubNodeType.OPERATION} isDisabled={isDraftEmpty || !isPolicyEditable} />
        <ToolItem nodeType={DataHubNodeType.FUNCTION} isDisabled={isDraftEmpty || !isPolicyEditable} />
      </ToolGroup>
    </Wrapper>
  )
}
