import { FC, useCallback, useState } from 'react'
import { FaRightFromBracket } from 'react-icons/fa6'
import { Card, CardBody, HStack, Icon, Stack, VStack } from '@chakra-ui/react'

import { FieldMappingsModel, JsonNode } from '@/api/__generated__'
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
  item: FieldMappingsModel
  adapterType: string
  adapterId: string
  onClose: () => void
  onSubmit: (newItem: FieldMappingsModel) => void
  onChange: (id: keyof FieldMappingsModel, v: JsonNode | string | string[] | null) => void
}

const MappingContainer: FC<SubscriptionContainerProps> = ({ adapterId, adapterType, item, onChange }) => {
  const { t } = useTranslation('components')
  const [strategy] = useState<MappingStrategy>(MappingStrategy.TYPED)

  const onSchemaReadyHandler = useCallback(
    (properties: FlatJSONSchema7[]) => {
      onChange('metadata', { ...item.metadata, destination: properties })
    },
    [item.metadata, onChange]
  )

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
            value={item.tag}
            onChange={(v) => onChange('tag', v)}
          />
          {!item.tag && (
            <Card size="sm" h="25vh">
              <CardBody pt="50px">
                <ErrorMessage message={t('rjsf.MqttTransformationField.destination.prompt')} status="info" />
              </CardBody>
            </Card>
          )}

          {item.tag && (
            <MappingEditor
              flex={1}
              adapterId={adapterId}
              adapterType={adapterType}
              topic={item.tag}
              mapping={item.fieldMapping}
              showTransformation={strategy === MappingStrategy.TRANSFORMED}
              onChange={(mappings) => {
                if (!mappings) {
                  return
                }
                onChange('fieldMapping', mappings)
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
