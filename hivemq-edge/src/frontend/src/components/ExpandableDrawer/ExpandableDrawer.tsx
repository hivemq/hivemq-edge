import { FC } from 'react'
import {
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  Text,
  DrawerProps,
  useBoolean,
} from '@chakra-ui/react'
import DrawerExpandButton from '@/components/Chakra/DrawerExpandButton.tsx'

interface ExpandableDrawerProps extends DrawerProps {
  header: string
}

const ExpandableDrawer: FC<ExpandableDrawerProps> = ({ header, ...props }) => {
  // TODO[NVL] use  local storage
  const [isExpanded, setExpanded] = useBoolean(true)

  return (
    <Drawer placement="right" size={isExpanded ? 'full' : 'lg'} {...props} variant="hivemq">
      {!props.closeOnOverlayClick && <DrawerOverlay />}
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerExpandButton isExpanded={isExpanded} toggle={setExpanded.toggle} />
        <DrawerHeader>
          <Text>{header}</Text>
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          {props.children}
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default ExpandableDrawer
