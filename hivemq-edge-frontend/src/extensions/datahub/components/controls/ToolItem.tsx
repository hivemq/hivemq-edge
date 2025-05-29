import type { FC } from 'react'
import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { ButtonProps } from '@chakra-ui/react'

import IconButton from '@/components/Chakra/IconButton.tsx'

import type { DataHubNodeType } from '@datahub/types.ts'
import { NodeIcon } from '@datahub/components/helpers'
import { DND_DESIGNER_NODE_TYPE } from '@datahub/utils/datahub.utils.ts'

interface ToolProps extends ButtonProps {
  nodeType: DataHubNodeType
  callback?: () => void
}

const ToolItem: FC<ToolProps> = ({ nodeType, isDisabled, callback }) => {
  const { t } = useTranslation('datahub')

  const onButtonDragEnd = useCallback((event: React.DragEvent<HTMLButtonElement>) => {
    event.dataTransfer.clearData(DND_DESIGNER_NODE_TYPE)
  }, [])

  const onButtonDragStart = useCallback(
    (event: React.DragEvent<HTMLButtonElement>) => {
      if (isDisabled) {
        event.preventDefault()
      } else if (event && !isDisabled) {
        event.dataTransfer.setData(DND_DESIGNER_NODE_TYPE, nodeType.toString())
        event.dataTransfer.effectAllowed = 'move'
        callback?.()
      }
    },
    [isDisabled, nodeType, callback]
  )

  return (
    <IconButton
      size="lg"
      onDragStart={onButtonDragStart}
      onDragEnd={onButtonDragEnd}
      draggable
      isDisabled={isDisabled}
      icon={<NodeIcon type={nodeType} />}
      aria-label={t('workspace.nodes.type', { context: nodeType })}
    />
  )
}

export default ToolItem
