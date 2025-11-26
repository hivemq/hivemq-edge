import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { Node } from '@xyflow/react'
import { Box, Text, VStack, HStack, Icon } from '@chakra-ui/react'
import { LuPlus } from 'react-icons/lu'

interface AutoIncludedNodesListProps {
  /** Nodes that will be automatically included (DEVICE/HOST nodes) */
  autoIncludedNodes: Node[]
}

/**
 * Component that displays the list of nodes that will be auto-included in the group
 *
 * When users select adapters/bridges, their connected DEVICE/HOST nodes are automatically
 * included in the group. This component provides visual feedback about which nodes
 * will be auto-included, helping users understand the full scope of the group.
 *
 * Features:
 * - Only shown when there are auto-included nodes
 * - Blue background for visual distinction
 * - Plus icon to indicate "addition"
 * - Shows node type (Device/Host) for clarity
 * - Accessible with proper ARIA labels
 */
const AutoIncludedNodesList: FC<AutoIncludedNodesListProps> = ({ autoIncludedNodes }) => {
  const { t } = useTranslation()

  // Don't render anything if no auto-included nodes
  if (autoIncludedNodes.length === 0) {
    return null
  }

  /**
   * Get human-readable node type label using i18next context pattern
   */
  const getNodeTypeLabel = (nodeType: string | undefined): string => {
    // Use i18next context pattern: plain string key + context parameter
    // This will look for workspace.device.type_DEVICE_NODE, type_HOST_NODE, etc.
    return t('workspace.device.type', { context: nodeType, defaultValue: nodeType || 'node' })
  }

  return (
    <Box
      mt={4}
      p={3}
      bg="blue.50"
      borderRadius="md"
      borderWidth="1px"
      borderColor="blue.200"
      role="region"
      aria-label={t('workspace.wizard.group.autoIncluded')}
    >
      <Text fontSize="sm" fontWeight="medium" mb={2} color="blue.700">
        {t('workspace.wizard.group.autoIncluded')}
      </Text>
      <VStack align="stretch" spacing={1}>
        {autoIncludedNodes.map((node) => (
          <HStack key={node.id} fontSize="sm" color="blue.800">
            <Icon as={LuPlus} color="blue.500" boxSize={4} aria-hidden="true" />
            <Text fontWeight="medium">{String(node.data?.label || node.id)}</Text>
            <Text color="blue.600" fontSize="xs">
              ({getNodeTypeLabel(node.type)})
            </Text>
          </HStack>
        ))}
      </VStack>
    </Box>
  )
}

export default AutoIncludedNodesList
