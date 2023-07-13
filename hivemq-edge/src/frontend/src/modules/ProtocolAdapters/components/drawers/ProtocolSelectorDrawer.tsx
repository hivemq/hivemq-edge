import { FC } from 'react'
import {
  Drawer,
  DrawerBody,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  Button,
  Flex,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'

import { AdapterType } from '@/modules/ProtocolAdapters/types.ts'
import AdapterTypeSelector from '@/modules/ProtocolAdapters/components/drawers/AdapterTypeSelector.tsx'

interface ProtocolSelectorDrawerProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (data: AdapterType) => void
}

/**
 * @deprecated
 */
const ProtocolSelectorDrawer: FC<ProtocolSelectorDrawerProps> = ({ isOpen, onClose, onSubmit }) => {
  const { t } = useTranslation()

  const form = useForm<AdapterType>({
    mode: 'all',
    criteriaMode: 'all',
    // defaultValues: { adapterType: undefined },
  })

  return (
    <>
      <Drawer closeOnOverlayClick={false} size={'lg'} isOpen={isOpen} placement="right" onClose={onClose}>
        <DrawerOverlay />
        <DrawerContent aria-label={t('protocolAdapter.store.label') as string}>
          <DrawerCloseButton />
          <DrawerHeader id={'adapter-selector-header'} borderBottomWidth="1px">
            {t('protocolAdapter.store.title.create')}
          </DrawerHeader>

          <DrawerBody>
            <form
              id="adapter-selector-form"
              onSubmit={form.handleSubmit(onSubmit)}
              style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}
            >
              <AdapterTypeSelector form={form} />
            </form>
          </DrawerBody>
          <DrawerFooter>
            <Flex flexGrow={1} justifyContent={'flex-end'}>
              <Button isDisabled={!form.formState.isValid} type="submit" form="adapter-selector-form">
                {t('protocolAdapter.action.instantiate')}
              </Button>
            </Flex>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </>
  )
}

export default ProtocolSelectorDrawer
