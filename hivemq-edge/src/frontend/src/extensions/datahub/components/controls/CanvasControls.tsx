import type { FC } from 'react'
import type { ControlProps, ReactFlowState } from 'reactflow'
import { useReactFlow, useStore, useStoreApi } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { ButtonGroup } from '@chakra-ui/react'
import { FaLock, FaLockOpen, FaMinus, FaPlus } from 'react-icons/fa6'
import { LuBoxSelect } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import Panel from '@/components/react-flow/Panel.tsx'
import DesignerCheatSheet from '@datahub/components/controls/DesignerCheatSheet.tsx'

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
      <ButtonGroup variant="outline" isAttached size="sm" aria-label={t('workspace.controls.aria-label')}>
        <IconButton icon={<FaPlus />} onClick={() => zoomIn()} aria-label={t('workspace.controls.zoomIn')} />
        <IconButton icon={<FaMinus />} onClick={() => zoomOut()} aria-label={t('workspace.controls.zoomIOut')} />
        <IconButton icon={<LuBoxSelect />} onClick={() => fitView()} aria-label={t('workspace.controls.fitView')} />
        <IconButton
          icon={isInteractive ? <FaLock /> : <FaLockOpen />}
          onClick={onToggleInteractivity}
          aria-label={t('workspace.controls.toggleInteractivity')}
        />
        <DesignerCheatSheet />
      </ButtonGroup>
    </Panel>
  )
}

export default CanvasControls
