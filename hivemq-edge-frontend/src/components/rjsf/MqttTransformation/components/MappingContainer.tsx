import type { FC } from 'react'
import { useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FaRightFromBracket } from 'react-icons/fa6'
import type { JSONSchema7, JSONSchema7Definition } from 'json-schema'
import {
  Card,
  CardBody,
  FormControl,
  FormHelperText,
  FormLabel,
  HStack,
  Heading,
  Icon,
  Stack,
  Switch,
  VStack,
} from '@chakra-ui/react'

import type { SouthboundMapping, JsonNode } from '@/api/__generated__'
import { southboundMappingListSchema } from '@/api/schemas/southbound.json-schema.ts'
import DataModelSources from '@/components/rjsf/MqttTransformation/components/DataModelSources.tsx'
import MappingEditor from '@/components/rjsf/MqttTransformation/components/MappingEditor.tsx'
import {
  SelectDestinationTag,
  SelectSourceTopics,
} from '@/components/rjsf/MqttTransformation/components/EntitySelector.tsx'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { MappingStrategy } from '@/components/rjsf/MqttTransformation/types.ts'

interface SubscriptionContainerProps {
  item: SouthboundMapping
  adapterType: string
  adapterId: string
  onClose: () => void
  onSubmit: (newItem: SouthboundMapping) => void
  onChange: (id: keyof SouthboundMapping, v: JsonNode | string | string[] | boolean | null) => void
}

/** The settings this container renders itself, because the custom field replaces RJSF's generic rendering. */
type MethodCallSetting = 'methodCalls' | 'correlationData' | 'responseTopic'

/**
 * The label and helper text come from the JSON schema rather than the translation bundle: the schema is where
 * these three settings are declared, and a copy in the bundle would be a second place for the wording to drift.
 */
const methodCallSettingSchema = (id: MethodCallSetting): JSONSchema7 => {
  const mapping = southboundMappingListSchema.definitions?.SouthboundMapping as JSONSchema7 | undefined
  const property: JSONSchema7Definition | undefined = mapping?.properties?.[id]
  return typeof property === 'object' ? property : {}
}

const MappingContainer: FC<SubscriptionContainerProps> = ({ adapterId, adapterType, item, onChange }) => {
  const { t } = useTranslation('components')
  const [strategy] = useState<MappingStrategy>(MappingStrategy.TYPED)

  const renderMethodCallSetting = (id: MethodCallSetting, isDisabled: boolean) => {
    const { title, description } = methodCallSettingSchema(id)
    const controlId = `mapping-${id}`

    return (
      <FormControl isDisabled={isDisabled}>
        <HStack alignItems="center" gap={3}>
          <Switch
            id={controlId}
            isChecked={Boolean(item[id])}
            isDisabled={isDisabled}
            onChange={(event) => onChange(id, event.target.checked)}
          />
          <FormLabel htmlFor={controlId} mb={0}>
            {title}
          </FormLabel>
        </HStack>
        <FormHelperText>{description}</FormHelperText>
      </FormControl>
    )
  }

  // The two refinements only describe fields of a method request, so they are meaningless -- and must not be
  // independently settable -- while this mapping does not carry method calls at all.
  const areRefinementsDisabled = !item.methodCalls

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
                // The list emits a complete replacement (already pruned of read-only destinations) — appending
                // it to the old list would duplicate every kept instruction and resurrect the pruned ones.
                onChange('fieldMapping', { ...item.fieldMapping, instructions: mappings })
              }}
              onSchemaReady={onSchemaReadyHandler}
            />
          )}
        </VStack>
      </Stack>

      <Card size="sm" data-testid="mapping-method-calls">
        <CardBody>
          <VStack alignItems="stretch" gap={4}>
            <Heading as="h5" size="sm">
              {t('rjsf.MqttTransformationField.methodCalls.header')}
            </Heading>
            {renderMethodCallSetting('methodCalls', false)}
            {/* Indented so the two refinements read as subordinate to the setting that enables them. */}
            <VStack alignItems="stretch" gap={4} pl={8} borderLeftWidth={2} borderLeftStyle="solid">
              {renderMethodCallSetting('correlationData', areRefinementsDisabled)}
              {renderMethodCallSetting('responseTopic', areRefinementsDisabled)}
            </VStack>
          </VStack>
        </CardBody>
      </Card>
    </VStack>
  )
}

export default MappingContainer
