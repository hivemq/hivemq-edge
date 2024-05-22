import { FC } from 'react'
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

  const isEditable = status !== DesignerStatus.LOADED

  function onHandleClear() {
    reset()
    navigate(`/datahub/${PolicyType.CREATE_POLICY}`)
  }

  function onHandleEdit() {
    setStatus(DesignerStatus.MODIFIED)
  }

  return (
    <HStack
      alignItems="flex-start"
      p={2}
      borderWidth={1}
      bg="var(--chakra-colors-chakra-body-bg)"
      borderRadius="var(--chakra-radii-base)"
      boxShadow="var(--chakra-shadows-lg)"
      sx={{ textWrap: 'nowrap' }}
      gap={4}
    >
      <HStack>
        <NodeIcon type={DataHubNodeType.DATA_POLICY} />
        <Text>{t('workspace.toolbox.draft.status', { context: status, name: name, type })}</Text>
      </HStack>
      <ButtonGroup size="xs">
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
