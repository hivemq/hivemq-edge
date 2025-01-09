import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack, ButtonGroup } from '@chakra-ui/react'

import config from '@/config'

import { DataHubNodeType, DesignerStatus } from '@datahub/types.ts'
import Tool from '@datahub/components/controls/Tool.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

export const ToolboxNodes: FC = () => {
  const { t } = useTranslation('datahub')
  const { nodes, status } = useDataHubDraftStore()

  const isEditEnabled = config.features.DATAHUB_EDIT_POLICY_ENABLED || status === DesignerStatus.DRAFT
  const isDraftEmpty = nodes.length === 0

  return (
    <HStack
      pb={2}
      gap={5}
      role="group"
      aria-label={t('workspace.toolbox.aria-label')}
      backgroundColor="var(--chakra-colors-chakra-body-bg)"
    >
      <ButtonGroup variant="outline" size="sm" aria-labelledby="group-integrations">
        <VStack alignItems="flex-start">
          <Text id="group-integrations">{t('workspace.toolbox.group.integration.edge')}</Text>
          <HStack>
            <Tool nodeType={DataHubNodeType.TOPIC_FILTER} isDisabled={isDraftEmpty || !isEditEnabled} />
            <Tool nodeType={DataHubNodeType.CLIENT_FILTER} isDisabled={isDraftEmpty || !isEditEnabled} />
          </HStack>
        </VStack>
      </ButtonGroup>
      <ButtonGroup variant="outline" size="sm" aria-labelledby="group-dataPolicy">
        <VStack alignItems="flex-start">
          <Text id="group-dataPolicy">{t('workspace.toolbox.group.dataPolicy')}</Text>
          <HStack>
            <Tool nodeType={DataHubNodeType.DATA_POLICY} isDisabled={!isEditEnabled} />
            <Tool nodeType={DataHubNodeType.VALIDATOR} isDisabled={isDraftEmpty || !isEditEnabled} />
            <Tool nodeType={DataHubNodeType.SCHEMA} isDisabled={isDraftEmpty || !isEditEnabled} />
          </HStack>
        </VStack>
      </ButtonGroup>
      <ButtonGroup variant="outline" size="sm" aria-labelledby="group-behaviorPolicy">
        <VStack alignItems="flex-start">
          <Text id="group-behaviorPolicy">{t('workspace.toolbox.group.behaviorPolicy')}</Text>
          <HStack>
            <Tool nodeType={DataHubNodeType.BEHAVIOR_POLICY} isDisabled={!isEditEnabled} />
            <Tool nodeType={DataHubNodeType.TRANSITION} isDisabled={isDraftEmpty || !isEditEnabled} />
          </HStack>
        </VStack>
      </ButtonGroup>
      <ButtonGroup variant="outline" size="sm" aria-labelledby="group-operation">
        <VStack alignItems="flex-start" pr={2}>
          <Text id="group-operation">{t('workspace.toolbox.group.operation')}</Text>
          <HStack>
            <Tool nodeType={DataHubNodeType.OPERATION} isDisabled={isDraftEmpty || !isEditEnabled} />
            <Tool nodeType={DataHubNodeType.FUNCTION} isDisabled={isDraftEmpty || !isEditEnabled} />
          </HStack>
        </VStack>
      </ButtonGroup>
    </HStack>
  )
}
