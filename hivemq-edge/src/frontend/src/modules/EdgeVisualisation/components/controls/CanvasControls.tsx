import { useEdgeFlowContext } from '@/modules/EdgeVisualisation/hooks/useEdgeFlowContext.tsx'
import { FC } from 'react'
import { IoMdOptions } from 'react-icons/io'
import { ControlButton, Controls } from 'reactflow'

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
