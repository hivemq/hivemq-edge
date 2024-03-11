import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack, ButtonGroup } from '@chakra-ui/react'
import { DataHubNodeType } from '@datahub/types.ts'
import Tool from '@datahub/components/controls/Tool.tsx'

export const ToolboxNodes = () => {
  const { t } = useTranslation('datahub')
  const isBehaviorPolicyEnabled = import.meta.env.VITE_FLAG_DATAHUB_BEHAVIOR_ENABLED === 'true'

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
            <Tool nodeType={DataHubNodeType.TOPIC_FILTER} />
            <Tool nodeType={DataHubNodeType.CLIENT_FILTER} />
          </HStack>
        </VStack>
      </ButtonGroup>
      <ButtonGroup variant="outline" size="sm" aria-labelledby="group-dataPolicy">
        <VStack alignItems="flex-start">
          <Text id="group-dataPolicy">{t('workspace.toolbox.group.dataPolicy')}</Text>
          <HStack>
            <Tool nodeType={DataHubNodeType.DATA_POLICY} />
            <Tool nodeType={DataHubNodeType.VALIDATOR} />
            <Tool nodeType={DataHubNodeType.SCHEMA} />{' '}
          </HStack>
        </VStack>
      </ButtonGroup>
      {isBehaviorPolicyEnabled && (
        <ButtonGroup variant="outline" size="sm" aria-labelledby="group-behaviorPolicy">
          <VStack alignItems="flex-start">
            <Text id="group-behaviorPolicy">{t('workspace.toolbox.group.behaviorPolicy')}</Text>
            <HStack>
              <Tool nodeType={DataHubNodeType.BEHAVIOR_POLICY} />
              <Tool nodeType={DataHubNodeType.TRANSITION} />
            </HStack>
          </VStack>
        </ButtonGroup>
      )}
      <ButtonGroup variant="outline" size="sm" aria-labelledby="group-operation">
        <VStack alignItems="flex-start" pr={2}>
          <Text id="group-operation">{t('workspace.toolbox.group.operation')}</Text>
          <HStack>
            <Tool nodeType={DataHubNodeType.OPERATION} />
            <Tool nodeType={DataHubNodeType.FUNCTION} />
          </HStack>
        </VStack>
      </ButtonGroup>
    </HStack>
  )
}
