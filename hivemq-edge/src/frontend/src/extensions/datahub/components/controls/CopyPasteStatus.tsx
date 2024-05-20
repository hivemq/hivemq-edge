import { FC } from 'react'
import { Panel } from 'reactflow'
import { Tag, TagLabel, TagLeftIcon } from '@chakra-ui/react'
import { LuCopy, LuCopyCheck } from 'react-icons/lu'
import { PiPencilSimpleLineFill, PiPencilSimpleSlashFill } from 'react-icons/pi'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DesignerStatus } from '@datahub/types.ts'

interface CopyPasteStatusProps {
  nbCopied: number | undefined
}

const CopyPasteStatus: FC<CopyPasteStatusProps> = ({ nbCopied }) => {
  const { status } = useDataHubDraftStore()
  const isEditable = status !== DesignerStatus.LOADED

  return (
    <Panel position="bottom-center">
      <Tag size="lg" variant="subtle" userSelect="none" data-testid="edit-status">
        <TagLeftIcon boxSize="18px" as={isEditable ? PiPencilSimpleLineFill : PiPencilSimpleSlashFill} />
      </Tag>
      <Tag size="lg" variant="subtle" userSelect="none" data-testid="copy-paste-status">
        <TagLeftIcon boxSize="12px" as={nbCopied ? LuCopyCheck : LuCopy} />
        <TagLabel>{nbCopied}</TagLabel>
      </Tag>
    </Panel>
  )
}

export default CopyPasteStatus
