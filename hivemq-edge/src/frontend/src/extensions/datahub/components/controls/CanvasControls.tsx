import { FC } from 'react'
import { ControlProps, Panel, ReactFlowState, useReactFlow, useStore, useStoreApi } from 'reactflow'

import { ButtonGroup } from '@chakra-ui/react'
import { FaLock, FaLockOpen, FaMinus, FaPlus } from 'react-icons/fa6'
import { LuBoxSelect } from 'react-icons/lu'
import { useTranslation } from 'react-i18next'

import IconButton from '@/components/Chakra/IconButton.tsx'

import 'reactflow/dist/style.css'

const CanvasControls: FC<ControlProps> = ({ onInteractiveChange }) => {
  const { t } = useTranslation('datahub')
  const store = useStoreApi()
  const { zoomIn, zoomOut, fitView } = useReactFlow()
  const { isInteractive } = useStore((s: ReactFlowState) => ({
    isInteractive: s.nodesDraggable || s.nodesConnectable || s.elementsSelectable,
  }))

  const onToggleInteractivity = () => {
    store.setState({
      nodesDraggable: !isInteractive,
      nodesConnectable: !isInteractive,
      elementsSelectable: !isInteractive,
    })

    onInteractiveChange?.(!isInteractive)
  }

  return (
    <Panel position="bottom-left">
      <ButtonGroup variant="outline" isAttached size="sm" aria-label={t('workspace.controls.aria-label') as string}>
        <IconButton icon={<FaPlus />} onClick={() => zoomIn()} aria-label={t('workspace.controls.zoomIn') as string} />
        <IconButton
          icon={<FaMinus />}
          onClick={() => zoomOut()}
          aria-label={t('workspace.controls.zoomIOut') as string}
        />
        <IconButton
          icon={<LuBoxSelect />}
          onClick={() => fitView()}
          aria-label={t('workspace.controls.fitView') as string}
        />
        <IconButton
          icon={isInteractive ? <FaLock /> : <FaLockOpen />}
          onClick={onToggleInteractivity}
          aria-label={t('workspace.controls.toggleInteractivity') as string}
        />
      </ButtonGroup>
    </Panel>
  )
}

export default CanvasControls
