import { FC } from 'react'
import { Panel } from 'reactflow'
import { Tag, TagLabel, TagLeftIcon } from '@chakra-ui/react'
import { LuCopy, LuCopyCheck } from 'react-icons/lu'

interface CopyPasteStatusProps {
  nbCopied: number | undefined
}

const CopyPasteStatus: FC<CopyPasteStatusProps> = ({ nbCopied }) => {
  return (
    <Panel position="bottom-center">
      <Tag size="lg" variant="subtle" userSelect="none" data-testid="copy-paste-status">
        <TagLeftIcon boxSize="12px" as={nbCopied ? LuCopyCheck : LuCopy} />
        <TagLabel>{nbCopied}</TagLabel>
      </Tag>
    </Panel>
  )
}

export default CopyPasteStatus
