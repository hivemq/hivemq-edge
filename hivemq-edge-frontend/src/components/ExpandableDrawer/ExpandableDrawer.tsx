import type { FC } from 'react'
import type { DrawerProps } from '@chakra-ui/react'
import {
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  Text,
  useBoolean,
} from '@chakra-ui/react'
import DrawerExpandButton from '@/components/Chakra/DrawerExpandButton.tsx'

interface ExpandableDrawerProps extends DrawerProps {
  header: string
  subHeader?: JSX.Element
  initiallyExpanded?: boolean
}

const ExpandableDrawer: FC<ExpandableDrawerProps> = ({ header, subHeader, initiallyExpanded = false, ...props }) => {
  // TODO[NVL] use  local storage
  const [isExpanded, setExpanded] = useBoolean(initiallyExpanded)

  return (
    <Drawer placement="right" size={isExpanded ? 'full' : 'lg'} {...props} variant="hivemq">
      {!props.closeOnOverlayClick && <DrawerOverlay />}
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerExpandButton isExpanded={isExpanded} toggle={setExpanded.toggle} />
        <DrawerHeader>
          <Text>{header}</Text>
          {subHeader}
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          {props.children}
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default ExpandableDrawer
