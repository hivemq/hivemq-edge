import { FC } from 'react'
import { ButtonGroup, Tag, TagLabel, TagLeftIcon } from '@chakra-ui/react'
import { LuCopy, LuCopyCheck } from 'react-icons/lu'

import Panel from '@/components/react-flow/Panel.tsx'
import { useTranslation } from 'react-i18next'

interface CopyPasteStatusProps {
  nbCopied: number | undefined
}

const CopyPasteStatus: FC<CopyPasteStatusProps> = ({ nbCopied }) => {
  const { t } = useTranslation('datahub')
  return (
    <Panel position="bottom-center">
      <ButtonGroup variant="outline" isAttached size="sm" aria-label={t('workspace.toolbars.clipboard.aria-label')}>
        <Tag
          size="lg"
          variant="subtle"
          userSelect="none"
          data-testid="copy-paste-status"
          tabIndex={0}
          aria-label={t(`workspace.toolbars.clipboard.content`, { count: nbCopied })}
        >
          <TagLeftIcon boxSize="12px" as={nbCopied ? LuCopyCheck : LuCopy} />
          <TagLabel>{nbCopied}</TagLabel>
        </Tag>
      </ButtonGroup>
    </Panel>
  )
}

export default CopyPasteStatus
