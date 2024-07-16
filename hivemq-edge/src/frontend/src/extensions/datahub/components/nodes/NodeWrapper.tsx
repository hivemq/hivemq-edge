import { FC, ReactNode, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { NodeProps } from 'reactflow'
import { Avatar, BoxProps, Card, CardBody, CardBodyProps, HStack } from '@chakra-ui/react'

import NodeDatahubToolbar from '@datahub/components/nodes/NodeDatahubToolbar.tsx'
import { DataHubNodeData, PolicyDryRunStatus } from '@datahub/types.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import { parseHotkey } from '@datahub/utils/hotkeys.utils.ts'
import { DATAHUB_HOTKEY } from '@datahub/utils/datahub.utils.ts'

interface NodeWrapperProps extends NodeProps<DataHubNodeData> {
  children: ReactNode
  route: string
  wrapperProps?: CardBodyProps
}

export const NodeWrapper: FC<NodeWrapperProps> = ({ selected, children, route, wrapperProps, data }) => {
  const [internalSelection, setInternalSelection] = useState(false)
  const navigate = useNavigate()
  const { pathname } = useLocation()

  const CheckIcon = useMemo(() => getDryRunStatusIcon(data.dryRunStatus), [data])

  useEffect(() => {
    if (!selected) setInternalSelection(false)
  }, [selected])

  const onHandleCopy = () => {
    document.dispatchEvent(new KeyboardEvent('keydown', parseHotkey(DATAHUB_HOTKEY.COPY)))
  }

  const onHandleDelete = () => {
    document.dispatchEvent(new KeyboardEvent('keydown', parseHotkey(DATAHUB_HOTKEY.DELETE)))
  }

  const onHandleEdit = (event: React.BaseSyntheticEvent) => {
    if (internalSelection) {
      navigate(route, { state: { origin: pathname } })
      event.preventDefault()
      event.stopPropagation()
    }
    setInternalSelection(true)
  }

  const selectedStyle: Partial<BoxProps> = {
    boxShadow: 'var(--chakra-shadows-outline)',
  }

  const errorStyle: Pick<BoxProps, 'boxShadow'> = {
    boxShadow: '0 0 10px 2px rgba(226, 85, 85, 0.75), 0 1px 1px rgb(0 0 0 / 15%)',
  }

  const successStyle: Pick<BoxProps, 'boxShadow'> = {
    boxShadow: '0 0 10px 2px rgba(0,121,36, 0.75), 0 1px 1px rgb(0 0 0 / 15%)',
  }

  const isDryRun = data.dryRunStatus !== undefined && data.dryRunStatus !== PolicyDryRunStatus.IDLE
  const isToolbarEnabled = import.meta.env.VITE_FLAG_DATAHUB_SHOW_TOOLBAR === 'true'

  return (
    <>
      {isToolbarEnabled && (
        <NodeDatahubToolbar
          isVisible={selected}
          onCopy={onHandleCopy}
          onDelete={onHandleDelete}
          onEdit={onHandleEdit}
        />
      )}
      <Card
        variant="elevated"
        {...(data.dryRunStatus === PolicyDryRunStatus.FAILURE ? { ...errorStyle } : {})}
        {...(data.dryRunStatus === PolicyDryRunStatus.SUCCESS ? { ...successStyle } : {})}
        {...(selected ? { ...selectedStyle } : {})}
        size="sm"
        onClick={onHandleEdit}
      >
        {isDryRun && (
          <Avatar
            position="absolute"
            left="-1rem"
            top="-1rem"
            size="sm"
            icon={<CheckIcon fontSize="1.2rem" />}
            {...(data.dryRunStatus === PolicyDryRunStatus.FAILURE ? { ...errorStyle, background: 'red.500' } : {})}
            {...(data.dryRunStatus === PolicyDryRunStatus.SUCCESS ? { ...successStyle, background: 'green.500' } : {})}
          />
        )}
        <CardBody as={HStack} {...wrapperProps}>
          {children}
        </CardBody>
      </Card>
    </>
  )
}
