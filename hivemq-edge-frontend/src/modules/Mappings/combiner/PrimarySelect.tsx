import { useMemo, type FC } from 'react'
import { useTranslation } from 'react-i18next'
import { type SingleValue, Select, chakraComponents } from 'chakra-react-select'
import { HStack, Icon, Text, VStack } from '@chakra-ui/react'
import { FaKey } from 'react-icons/fa'

import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { PLCTag, TopicFilter as TopicFilterTag } from '@/components/MQTT/EntityTag.tsx'
import { formatOwnershipString } from '@/components/MQTT/topic-utils.ts'
import type { CombinerContext } from '@/modules/Mappings/types'
import { getAdapterIdForTag } from '@/modules/Mappings/utils/combining.utils'

interface PrimaryOption {
  label: string
  value: string
  type: DataIdentifierReference.type
  adapterId?: string
  description?: string
}

interface PrimarySelectProps {
  id?: string
  formData?: DataCombining
  formContext?: CombinerContext
  onChange: (newValue: SingleValue<PrimaryOption>) => void
}

export const PrimarySelect: FC<PrimarySelectProps> = ({ id, formData, formContext, onChange }) => {
  const { t } = useTranslation()

  const primaryOptions = useMemo(() => {
    // Prefer selectedSources from context — carries DataIdentifierReference[] with scope already resolved
    if (formContext?.selectedSources) {
      return [
        ...formContext.selectedSources.tags.map<PrimaryOption>((ref) => ({
          label: ref.id,
          value: ref.id,
          type: DataIdentifierReference.type.TAG,
          adapterId: ref.scope || undefined,
        })),
        ...formContext.selectedSources.topicFilters.map<PrimaryOption>((ref) => ({
          label: ref.id,
          value: ref.id,
          type: DataIdentifierReference.type.TOPIC_FILTER,
        })),
      ]
    }

    // Fallback: deprecated string arrays + context lookup
    const tags = formData?.sources?.tags || []
    const topicFilters = formData?.sources?.topicFilters || []

    return [
      ...tags.map<PrimaryOption>((entity) => ({
        label: entity,
        value: entity,
        type: DataIdentifierReference.type.TAG,
        adapterId: getAdapterIdForTag(entity, formContext),
      })),
      ...topicFilters.map<PrimaryOption>((entity) => ({
        label: entity,
        value: entity,
        type: DataIdentifierReference.type.TOPIC_FILTER,
      })),
    ]
  }, [formData, formContext])

  const primaryValue = useMemo<PrimaryOption | null>(() => {
    if (!formData?.sources.primary) return null
    const primary = formData.sources.primary
    return {
      label: formatOwnershipString(primary),
      value: primary.id,
      type: primary.type,
      adapterId: primary.scope || undefined,
    }
  }, [formData?.sources.primary])

  return (
    <Select<PrimaryOption>
      id={id}
      options={primaryOptions}
      data-testid="combiner-mapping-primaryOptions"
      value={primaryValue}
      onChange={onChange}
      isClearable
      components={{
        Control: ({ children, ...props }) => (
          <chakraComponents.Control {...props}>
            <Icon as={FaKey} ml={3} />
            {children}
          </chakraComponents.Control>
        ),
        Option: ({ children: _children, ...props }) => (
          <chakraComponents.Option {...props}>
            <VStack gap={0} alignItems="stretch" w="100%">
              <HStack>
                <Text flex={1}>{props.data.label}</Text>
                {props.data.adapterId && (
                  <Text fontSize="sm" color="gray.500">
                    {props.data.adapterId}
                  </Text>
                )}
                <Text fontSize="sm" fontWeight="bold">
                  {t('combiner.schema.mapping.combinedSelector.type', { context: props.data.type })}
                </Text>
              </HStack>
            </VStack>
          </chakraComponents.Option>
        ),
        SingleValue: (props) => (
          <chakraComponents.SingleValue {...props}>
            {props.data.type === DataIdentifierReference.type.TAG ? (
              <PLCTag tagTitle={props.data.label} />
            ) : (
              <TopicFilterTag tagTitle={props.data.label} />
            )}
          </chakraComponents.SingleValue>
        ),
      }}
      placeholder={t('combiner.schema.mapping.primary.placeholder')}
      aria-label={t('combiner.schema.mapping.primary.label')}
    />
  )
}
