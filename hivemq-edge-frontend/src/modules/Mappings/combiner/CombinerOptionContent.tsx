import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataIdentifierReference } from '@/api/__generated__'
import { PLCTagIcon, TopicFilterIcon } from '@/components/Icons/TopicIcon'
import { formatOwnershipString } from '@/components/MQTT/topic-utils'

interface CombinerOptionContentProps {
  label: string
  type: DataIdentifierReference.type
  adapterId?: string
  description?: string
}

export const CombinerOptionContent: FC<CombinerOptionContentProps> = ({ label, adapterId, type, description }) => {
  const { t } = useTranslation()
  // const displayLabel = adapterId ? `${adapterId} :: ${label}` : label
  const displayLabel = formatOwnershipString({ id: label, type, scope: adapterId })

  return (
    <VStack gap={0} alignItems="stretch" w="100%">
      <HStack>
        <HStack flex={1} gap={2} overflow="hidden" minW={0}>
          {type === DataIdentifierReference.type.TAG && <PLCTagIcon boxSize="12px" flexShrink={0} />}
          {type === DataIdentifierReference.type.TOPIC_FILTER && <TopicFilterIcon boxSize="12px" flexShrink={0} />}
          <Text isTruncated>{displayLabel}</Text>
        </HStack>
        <Text fontSize="sm" fontWeight="bold">
          {t('combiner.schema.mapping.combinedSelector.type', { context: type })}
        </Text>
      </HStack>
      {description && (
        <Text fontSize="sm" noOfLines={3} ml={5} lineHeight="normal" textAlign="justify">
          {description}
        </Text>
      )}
    </VStack>
  )
}
