import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { ControlButton, Controls } from 'reactflow'
import { IoMdOptions } from 'react-icons/io'

import { useEdgeFlowContext } from '../../hooks/useEdgeFlowContext.tsx'

const CanvasControls: FC = () => {
  const { t } = useTranslation()
  const { optionDrawer } = useEdgeFlowContext()

  return (
    <Controls>
      <ControlButton onClick={() => optionDrawer.onOpen()} title={t('workspace.configuration.header') as string}>
        <IoMdOptions />
      </ControlButton>
    </Controls>
  )
}

export default CanvasControls
