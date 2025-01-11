import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { ButtonGroup, HStack, Icon, Text } from '@chakra-ui/react'
import { LuTrash2 } from 'react-icons/lu'
import { PiPencilSimpleLineFill } from 'react-icons/pi'

import IconButton from '@/components/Chakra/IconButton.tsx'
import { NodeIcon } from '@datahub/components/helpers/index.ts'
import { DataHubNodeType, DesignerStatus, PolicyType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

const DraftStatus: FC = () => {
  const { t } = useTranslation('datahub')
  const { status, name, type, reset, setStatus } = useDataHubDraftStore()
  const navigate = useNavigate()

  const isEditable = useMemo(() => status !== DesignerStatus.LOADED, [status])

  function onHandleClear() {
    reset()
    navigate(`/datahub/${PolicyType.CREATE_POLICY}`)
  }

  function onHandleEdit() {
    setStatus(DesignerStatus.MODIFIED)
  }

  return (
    <HStack alignItems="center" sx={{ textWrap: 'nowrap' }} gap={4}>
      <HStack role="group" aria-label={t('workspace.toolbars.status.aria-label')}>
        <NodeIcon type={DataHubNodeType.DATA_POLICY} />
        <Text>{t('workspace.toolbox.draft.status', { context: status, name: name, type })}</Text>
      </HStack>
      <ButtonGroup role="group" aria-label={t('workspace.toolbars.edit.aria-label')}>
        <IconButton
          isDisabled={isEditable}
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
    </HStack>
  )
}

export default DraftStatus
