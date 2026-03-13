import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataIdentifierReference } from '@/api/__generated__'
import { PLCTagIcon } from '@/components/Icons/TopicIcon'

interface CombinerOptionContentProps {
  label: string
  type: DataIdentifierReference.type
  adapterId?: string
  description?: string
}

export const CombinerOptionContent: FC<CombinerOptionContentProps> = ({ label, adapterId, type, description }) => {
  const { t } = useTranslation()
  const displayLabel = adapterId ? `${adapterId} :: ${label}` : label

  return (
    <VStack gap={0} alignItems="stretch" w="100%">
      <HStack>
        <HStack flex={1} gap={1} overflow="hidden">
          {type === DataIdentifierReference.type.TAG && (
            <PLCTagIcon boxSize="12px" flexShrink={0} color="blue.500" />
          )}
          <Text isTruncated>{displayLabel}</Text>
        </HStack>
        <Text fontSize="sm" fontWeight="bold">
          {t('combiner.schema.mapping.combinedSelector.type', { context: type })}
        </Text>
      </HStack>
      {description && (
        <Text fontSize="sm" noOfLines={3} ml={4} lineHeight="normal" textAlign="justify">
          {description}
        </Text>
      )}
    </VStack>
  )
}
