import type { FC, ReactNode } from 'react'
import { Box, Collapse } from '@chakra-ui/react'

interface NetworkGraphDetailsPanelContainerProps {
  isOpen: boolean
  children: ReactNode
}

/**
 * Container component that handles the positioning and animation of the details panel.
 * This separation allows easy swapping of layout strategies (bottom slide-in, side panel, etc.)
 * without changing the panel content.
 */
const NetworkGraphDetailsPanelContainer: FC<NetworkGraphDetailsPanelContainerProps> = ({ isOpen, children }) => {
  return (
    <Collapse in={isOpen} animateOpacity>
      <Box
        position="absolute"
        bottom={0}
        left={0}
        right={0}
        bg="white"
        borderTop="2px solid"
        borderColor="gray.200"
        p={4}
        maxH="300px"
        overflowY="auto"
        boxShadow="lg"
        zIndex={10}
        data-testid="network-graph-details-container"
      >
        {children}
      </Box>
    </Collapse>
  )
}

export default NetworkGraphDetailsPanelContainer
