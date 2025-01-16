import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Breadcrumb, BreadcrumbItem, ButtonGroup, HStack, Icon, Text, useDisclosure } from '@chakra-ui/react'
import { LuTrash2 } from 'react-icons/lu'
import { PiPencilSimpleLineFill } from 'react-icons/pi'

import IconButton from '@/components/Chakra/IconButton.tsx'
import { NodeIcon } from '@datahub/components/helpers/index.ts'
import { DataHubNodeType, DesignerStatus, PolicyType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.tsx'

const DraftStatus: FC = () => {
  const { t } = useTranslation('datahub')
  const { status, name, type, reset, setStatus } = useDataHubDraftStore()
  const { isPolicyEditable } = usePolicyGuards()
  const navigate = useNavigate()
  const { isOpen: isConfirmDeleteOpen, onOpen: onConfirmDeleteOpen, onClose: onConfirmDeleteClose } = useDisclosure()

  function onHandleClear() {
    onConfirmDeleteOpen()
  }

  function onHandleEdit() {
    setStatus(DesignerStatus.MODIFIED)
  }

  function handleConfirmOnClose() {
    onConfirmDeleteClose()
  }

  function handleConfirmOnSubmit() {
    reset()
    navigate(`/datahub/${PolicyType.CREATE_POLICY}`)
  }

  return (
    <HStack alignItems="center" sx={{ textWrap: 'nowrap' }} gap={4}>
      <HStack role="group" aria-label={t('workspace.toolbars.status.aria-label')}>
        <NodeIcon type={DataHubNodeType.DATA_POLICY} />
        <Breadcrumb separator="|">
          <BreadcrumbItem>
            <Text data-testid="status-type">{t('policy.type', { context: type || PolicyType.CREATE_POLICY })}</Text>
          </BreadcrumbItem>

          <BreadcrumbItem>
            <Text>{name || t('policy.unnamed')}</Text>
          </BreadcrumbItem>

          <BreadcrumbItem isCurrentPage>
            <Text>{t('workspace.toolbox.draft.status', { context: status })}</Text>
          </BreadcrumbItem>
        </Breadcrumb>
      </HStack>
      <ButtonGroup role="group" aria-label={t('workspace.toolbars.edit.aria-label')}>
        <IconButton
          isDisabled={isPolicyEditable}
          data-testid="designer-edit-"
          onClick={onHandleEdit}
          aria-label={t('workspace.controls.edit')}
          icon={<Icon as={PiPencilSimpleLineFill} boxSize="18px" />}
        />
        <IconButton
          data-testid="designer-clear-draft"
          onClick={onHandleClear}
          aria-label={t('workspace.controls.clear')}
          icon={<LuTrash2 />}
        />
      </ButtonGroup>
      <ConfirmationDialog
        isOpen={isConfirmDeleteOpen}
        onClose={handleConfirmOnClose}
        onSubmit={handleConfirmOnSubmit}
        message={t('workspace.toolbars.modal.clear.confirmation')}
        header={t('workspace.toolbars.modal.clear.header')}
        action={t('workspace.controls.clear')}
      />
    </HStack>
  )
}

export default DraftStatus
