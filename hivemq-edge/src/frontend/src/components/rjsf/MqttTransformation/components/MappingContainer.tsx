import { FC, useCallback, useState } from 'react'
import { FaRightFromBracket } from 'react-icons/fa6'
import { Card, CardBody, HStack, Icon, Stack, VStack } from '@chakra-ui/react'

import { SouthboundMapping, JsonNode } from '@/api/__generated__'
import DataModelSources from '@/components/rjsf/MqttTransformation/components/DataModelSources.tsx'
import MappingEditor from '@/components/rjsf/MqttTransformation/components/MappingEditor.tsx'
import {
  SelectDestinationTag,
  SelectSourceTopics,
} from '@/components/rjsf/MqttTransformation/components/EntitySelector.tsx'
import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { useTranslation } from 'react-i18next'

export enum MappingStrategy {
  EXACT = 'EXACT',
  TYPED = 'TYPED',
  TRANSFORMED = 'TRANSFORMED',
}

interface SubscriptionContainerProps {
  item: SouthboundMapping
  adapterType: string
  adapterId: string
  onClose: () => void
  onSubmit: (newItem: SouthboundMapping) => void
  onChange: (id: keyof SouthboundMapping, v: JsonNode | string | string[] | null) => void
}

const MappingContainer: FC<SubscriptionContainerProps> = ({ adapterId, adapterType, item, onChange }) => {
  const { t } = useTranslation('components')
  const [strategy] = useState<MappingStrategy>(MappingStrategy.TYPED)

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const onSchemaReadyHandler = useCallback((_properties: FlatJSONSchema7[]) => {
    /// TODO[⚠ 28441 ⚠] This will not work anymore because of nested structure. DO NOT MERGE AND FIX
    // onChange('metadata', { ...item.metadata, destination: properties })
  }, [])

  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <VStack flex={1} alignItems="stretch" maxW="40vw">
          <SelectSourceTopics
            adapterId={adapterId}
            adapterType={adapterType}
            value={item.topicFilter}
            onChange={(v) => onChange('topicFilter', v)}
          />
          {!item.topicFilter && (
            <Card size="sm" h="25vh">
              <CardBody pt="50px">
                <ErrorMessage message={t('rjsf.MqttTransformationField.sources.prompt')} status="info" />
              </CardBody>
            </Card>
          )}
          {item.topicFilter && <DataModelSources flex={1} topic={item.topicFilter} minH={250} />}
        </VStack>
        <VStack justifyContent="center">
          <HStack height={38}>
            <Icon as={FaRightFromBracket} />
          </HStack>
        </VStack>
        <VStack flex={1} alignItems="stretch" maxW="50vw">
          <SelectDestinationTag
            adapterId={adapterId}
            adapterType={adapterType}
            value={item.tagName}
            onChange={(v) => onChange('tagName', v)}
          />
          {!item.tagName && (
            <Card size="sm" h="25vh">
              <CardBody pt="50px">
                <ErrorMessage message={t('rjsf.MqttTransformationField.destination.prompt')} status="info" />
              </CardBody>
            </Card>
          )}

          {item.tagName && (
            <MappingEditor
              flex={1}
              adapterId={adapterId}
              adapterType={adapterType}
              topic={item.tagName}
              instructions={item.fieldMapping?.instructions}
              showTransformation={strategy === MappingStrategy.TRANSFORMED}
              onChange={(mappings) => {
                if (!mappings) {
                  return
                }
                onChange('fieldMapping', { instructions: [...(item.fieldMapping?.instructions || []), ...mappings] })
              }}
              onSchemaReady={onSchemaReadyHandler}
            />
          )}
        </VStack>
      </Stack>
    </VStack>
  )
}

export default MappingContainer
