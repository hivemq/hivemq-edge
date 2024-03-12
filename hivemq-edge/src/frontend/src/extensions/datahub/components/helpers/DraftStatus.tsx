import { FC } from 'react'
import { HStack, Text } from '@chakra-ui/react'
import { NodeIcon } from '@datahub/components/helpers/index.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { useTranslation } from 'react-i18next'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

const DraftStatus: FC = () => {
  const { t } = useTranslation('datahub')
  const { status, name, type } = useDataHubDraftStore()

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
    </HStack>
  )
}

export default DraftStatus
