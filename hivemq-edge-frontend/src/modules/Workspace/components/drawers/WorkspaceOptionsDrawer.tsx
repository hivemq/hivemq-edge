import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  FormControl,
  FormLabel,
  Text,
  useDisclosure,
} from '@chakra-ui/react'

import { useEdgeFlowContext } from '../../hooks/useEdgeFlowContext.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import { useNavigate } from 'react-router-dom'

const WorkspaceOptionsDrawer: FC = () => {
  const { t } = useTranslation()
  const { optionDrawer } = useEdgeFlowContext()
  const { reset } = useWorkspaceStore()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()

  const onReset = () => {
    reset()
    navigate('/')
  }

  return (
    <Drawer isOpen={optionDrawer.isOpen || false} placement="right" size="md" onClose={optionDrawer.onClose}>
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerHeader>{t('workspace.configuration.header')}</DrawerHeader>

        <DrawerBody>
          <FormControl as="fieldset">
            <FormLabel as="legend">{t('workspace.configuration.reset.legend')}</FormLabel>
            <Text>{t('workspace.configuration.reset.prompt')}</Text>

            <FormControl m={4}>
              <Button variant="danger" onClick={onOpen}>
                {t('workspace.configuration.reset.action')}
              </Button>
            </FormControl>
            <Text>{t('workspace.configuration.reset.warning')}</Text>
          </FormControl>
        </DrawerBody>
      </DrawerContent>
      <ConfirmationDialog
        isOpen={isOpen}
        onClose={onClose}
        onSubmit={onReset}
        message={t('workspace.configuration.reset.modal.description')}
        header={t('workspace.configuration.reset.legend')}
        action={t('workspace.configuration.reset.action')}
      />
    </Drawer>
  )
}

export default WorkspaceOptionsDrawer
