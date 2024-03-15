import { FC } from 'react'
import { ButtonGroup, HStack, Text } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { LuTrash2 } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'

import { NodeIcon } from '@datahub/components/helpers/index.ts'
import { DataHubNodeType, PolicyType } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { useNavigate } from 'react-router-dom'

const DraftStatus: FC = () => {
  const { t } = useTranslation('datahub')
  const { status, name, type, reset } = useDataHubDraftStore()
  const navigate = useNavigate()

  function onHandleClear() {
    // TODO[NVL] Add confirmation modal
    reset()
    navigate(`/datahub/${PolicyType.CREATE_POLICY}`)
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
    >
      <NodeIcon type={DataHubNodeType.DATA_POLICY} />
      <Text>{t('workspace.toolbox.draft.status', { context: status, name: name, type })}</Text>
      <ButtonGroup size="xs" isAttached>
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
