import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack, ButtonGroup } from '@chakra-ui/react'

import { DataHubNodeType, DesignerStatus } from '@datahub/types.ts'
import Tool from '@datahub/components/controls/Tool.tsx'
import { DesignerToolBoxProps } from '@datahub/components/controls/DesignerToolbox.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

export const ToolboxNodes: FC<DesignerToolBoxProps> = () => {
  const { t } = useTranslation('datahub')
  const { nodes, status } = useDataHubDraftStore()

  const isEditEnabled =
    import.meta.env.VITE_FLAG_DATAHUB_EDIT_POLICY_ENABLED === 'true' || status === DesignerStatus.DRAFT
  const isDraftEmpty = nodes.length === 0

  return (
    <HStack
      pb={2}
      gap={5}
      role="group"
      aria-label={t('workspace.toolbox.aria-label') as string}
      backgroundColor="var(--chakra-colors-chakra-body-bg)"
    >
      <ButtonGroup variant="outline" size="sm" aria-labelledby="group-pipeline">
        <VStack alignItems="flex-start">
          <Text id="group-pipeline">{t('workspace.toolbox.group.pipeline')}</Text>
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
