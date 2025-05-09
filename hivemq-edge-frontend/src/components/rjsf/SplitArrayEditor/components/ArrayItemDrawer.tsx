import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { UseDisclosureProps } from '@chakra-ui/react'
import {
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Text,
  useDisclosure,
} from '@chakra-ui/react'

import type { DomainTagList } from '@/api/__generated__'
import DeviceTagForm from '@/modules/Device/components/DeviceTagForm.tsx'
import type { ManagerContextType } from '@/modules/Mappings/types.ts'

interface DeviceTagDrawerProps<T> {
  context: ManagerContextType<T>
  // TODO[NVL] Make the component generic and pass the type
  onSubmit?: (data: unknown) => void
  trigger: (disclosureProps: UseDisclosureProps) => JSX.Element
  header: string
  submitLabel?: string
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const ArrayItemDrawer: FC<DeviceTagDrawerProps<any>> = ({ header, context, onSubmit, trigger, submitLabel }) => {
  const { t } = useTranslation('components')
  const props = useDisclosure()
  const { isOpen, onClose } = props

  const onHandleSubmit = (data: DomainTagList | undefined) => {
    onSubmit?.(data)
    onClose()
  }

  return (
    <>
      {trigger(props)}
      <Drawer isOpen={isOpen} placement="right" size="md" onClose={onClose} closeOnOverlayClick={false}>
        <DrawerOverlay />
        <DrawerContent>
          <DrawerCloseButton />
          <DrawerHeader>
            <Text>{header}</Text>
          </DrawerHeader>

          <DrawerBody>
            <DeviceTagForm context={context} onSubmit={onHandleSubmit} />
          </DrawerBody>

          <DrawerFooter>
            <Button variant="primary" type="submit" form="domainTags-instance-form">
              {submitLabel || t('rjsf.actions.submit.label')}
            </Button>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </>
  )
}

export default ArrayItemDrawer
