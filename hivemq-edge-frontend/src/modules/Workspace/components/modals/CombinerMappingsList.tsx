import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Box, HStack, Icon, List, ListIcon, ListItem, Text, VStack } from '@chakra-ui/react'
import { LuArrowRight, LuFileText } from 'react-icons/lu'

import type { DataCombining } from '@/api/__generated__'

interface CombinerMappingsListProps {
  mappings: DataCombining[]
}

/**
 * Lightweight component to display a list of combiner mappings
 * Used in the DuplicateCombinerModal to show existing mappings
 */
const CombinerMappingsList: FC<CombinerMappingsListProps> = ({ mappings }) => {
  const { t } = useTranslation()

  if (mappings.length === 0) {
    return (
      <Text color="chakra-subtle-text" fontSize="sm" fontStyle="italic" data-testid="mappings-list-empty">
        {t('workspace.modal.duplicateCombiner.noMappings')}
      </Text>
    )
  }

  return (
    <Box data-testid="mappings-list">
      <HStack mb={2}>
        <Badge colorScheme="blue" data-testid="mappings-count-badge">
          {t('workspace.modal.duplicateCombiner.mappingsCount', { count: mappings.length })}
        </Badge>
      </HStack>
      <List spacing={2} maxH="200px" overflowY="auto" pr={2} tabIndex={0}>
        {mappings.map((mapping) => (
          <ListItem
            key={mapping.id}
            fontSize="sm"
            p={2}
            borderRadius="md"
            bg="chakra-subtle-bg"
            data-testid={`mapping-item-${mapping.id}`}
          >
            <HStack spacing={2} align="flex-start">
              <ListIcon as={LuFileText} color="blue.500" mt={0.5} />
              <VStack align="flex-start" spacing={1} flex={1}>
                <HStack spacing={2} w="full">
                  <Text fontWeight="medium" data-testid={`mapping-source-${mapping.id}`}>
                    {mapping.sources.primary.id}
                  </Text>
                  <Icon as={LuArrowRight} color="chakra-subtle-text" />
                  <Text fontWeight="medium" data-testid={`mapping-destination-${mapping.id}`}>
                    {mapping.destination.topic ||
                      mapping.destination.assetId ||
                      t('workspace.modal.duplicateCombiner.noDestination')}
                  </Text>
                </HStack>
                {mapping.instructions.length > 0 && (
                  <Text fontSize="xs" color="chakra-subtle-text" data-testid={`mapping-instructions-${mapping.id}`}>
                    {t('workspace.modal.duplicateCombiner.instructionsCount', {
                      count: mapping.instructions.length,
                    })}
                  </Text>
                )}
              </VStack>
            </HStack>
          </ListItem>
        ))}
      </List>
    </Box>
  )
}

export default CombinerMappingsList
