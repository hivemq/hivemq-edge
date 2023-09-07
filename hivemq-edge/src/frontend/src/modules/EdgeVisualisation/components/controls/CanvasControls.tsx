import { FC } from 'react'
import { ControlButton, Controls } from 'reactflow'
import { useEdgeFlowContext } from '@/modules/EdgeVisualisation/hooks/useEdgeFlowContext.tsx'
import { IoMdOptions } from 'react-icons/io'

const CanvasControls: FC = () => {
  const { optionDrawer } = useEdgeFlowContext()

  return (
    <Controls>
      <ControlButton onClick={() => optionDrawer.onOpen()} title="configuration">
        <IoMdOptions />
      </ControlButton>
    </Controls>
  )
}

export default CanvasControls
