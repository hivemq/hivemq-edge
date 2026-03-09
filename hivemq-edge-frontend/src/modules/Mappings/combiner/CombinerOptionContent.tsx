import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataIdentifierReference } from '@/api/__generated__'

interface CombinerOptionContentProps {
  label: string
  type: DataIdentifierReference.type
  adapterId?: string
  description?: string
}

export const CombinerOptionContent: FC<CombinerOptionContentProps> = ({ label, adapterId, type, description }) => {
  const { t } = useTranslation()
  return (
    <VStack gap={0} alignItems="stretch" w="100%">
      <HStack>
        <Text flex={1}>{label}</Text>
        {adapterId && (
          <Text fontSize="sm" color="gray.500">
            {adapterId}
          </Text>
        )}
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
