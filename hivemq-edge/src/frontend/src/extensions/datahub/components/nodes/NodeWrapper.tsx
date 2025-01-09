import { FC, ReactNode, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { NodeProps } from 'reactflow'
import { Avatar, BoxProps, Card, CardBody, CardBodyProps, HStack } from '@chakra-ui/react'

import NodeToolbar from '@/components/react-flow/NodeToolbar.tsx'

import NodeDatahubToolbar from '@datahub/components/toolbar/NodeDatahubToolbar.tsx'
import { DataHubNodeData, PolicyDryRunStatus } from '@datahub/types.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import { parseHotkey } from '@datahub/utils/hotkeys.utils.ts'
import { DATAHUB_HOTKEY } from '@datahub/utils/datahub.utils.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

interface NodeWrapperProps extends NodeProps<DataHubNodeData> {
  children: ReactNode
  route: string
  wrapperProps?: CardBodyProps
  toolbar?: React.ReactNode
}

export const NodeWrapper: FC<NodeWrapperProps> = ({ selected, children, toolbar, route, wrapperProps, data }) => {
  const { nodes } = useDataHubDraftStore()
  const [internalSelection, setInternalSelection] = useState(false)
  const navigate = useNavigate()
  const { pathname } = useLocation()

  const CheckIcon = useMemo(() => getDryRunStatusIcon(data.dryRunStatus), [data])

  useEffect(() => {
    if (!selected) setInternalSelection(false)
  }, [selected])

  const isSingleSelect = useMemo(() => {
    const selectedNodes = nodes.filter((node) => node.selected)
    return selectedNodes.length === 1
  }, [nodes])

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

  return (
    <>
      <NodeToolbar isVisible={selected && isSingleSelect} offset={16}>
        <NodeDatahubToolbar onCopy={onHandleCopy} onDelete={onHandleDelete} onEdit={onHandleEdit}>
          {toolbar}
        </NodeDatahubToolbar>
      </NodeToolbar>

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
