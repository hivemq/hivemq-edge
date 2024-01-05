import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { ControlProps, Panel, ReactFlowState, useReactFlow, useStore, useStoreApi } from 'reactflow'
import { ButtonGroup } from '@chakra-ui/react'
import { shallow } from 'zustand/shallow'
import { IoMdOptions } from 'react-icons/io'
import { LuBoxSelect } from 'react-icons/lu'
import { FaLock, FaLockOpen, FaMinus, FaPlus } from 'react-icons/fa6'

import { useEdgeFlowContext } from '../../hooks/useEdgeFlowContext.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'

const selector = (s: ReactFlowState) => ({
  isInteractive: s.nodesDraggable || s.nodesConnectable || s.elementsSelectable,
})

const CanvasControls: FC<ControlProps> = ({ onInteractiveChange }) => {
  const { t } = useTranslation()
  const { optionDrawer } = useEdgeFlowContext()
  const store = useStoreApi()
  const { zoomIn, zoomOut, fitView } = useReactFlow()
  const { isInteractive } = useStore(selector, shallow)

  const onToggleInteractivity = () => {
    store.setState({
      nodesDraggable: !isInteractive,
      nodesConnectable: !isInteractive,
      elementsSelectable: !isInteractive,
    })

    onInteractiveChange?.(!isInteractive)
  }

  // + - f l
  return (
    <Panel position={'bottom-left'}>
      <ButtonGroup variant="outline" isAttached size={'sm'}>
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
        <IconButton
          icon={<IoMdOptions />}
          onClick={() => optionDrawer.onOpen()}
          aria-label={t('workspace.configuration.header') as string}
        />
      </ButtonGroup>
    </Panel>
  )
}

export default CanvasControls
