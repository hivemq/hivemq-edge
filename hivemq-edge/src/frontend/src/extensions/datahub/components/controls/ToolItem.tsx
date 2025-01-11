import { FC, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { ButtonProps } from '@chakra-ui/react'

import IconButton from '@/components/Chakra/IconButton.tsx'

import { DataHubNodeType } from '@datahub/types.ts'
import { NodeIcon } from '@datahub/components/helpers'

interface ToolProps extends ButtonProps {
  nodeType: DataHubNodeType
}

const ToolItem: FC<ToolProps> = ({ nodeType, isDisabled }) => {
  const { t } = useTranslation('datahub')

  const onButtonDragStart = useCallback(
    (event: React.DragEvent<HTMLButtonElement>) => {
      if (isDisabled) {
        event.preventDefault()
      } else if (event && !isDisabled) {
        event.dataTransfer.setData('application/reactflow', nodeType.toString())
        event.dataTransfer.effectAllowed = 'move'
      }
    },
    [nodeType, isDisabled]
  )

  return (
    <IconButton
      size="lg"
      onDragStart={onButtonDragStart}
      draggable
      isDisabled={isDisabled}
      icon={<NodeIcon type={nodeType} />}
      aria-label={t('workspace.nodes.type', { context: nodeType })}
    />
  )
}

export default ToolItem
