import type { FC, ReactNode } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { NodeProps } from 'reactflow'
import type { BoxProps, CardBodyProps } from '@chakra-ui/react'
import { Avatar, Card, CardBody, CardHeader, HStack, Text, useColorModeValue } from '@chakra-ui/react'

import NodeToolbar from '@/components/react-flow/NodeToolbar.tsx'

import NodeDatahubToolbar from '@datahub/components/toolbar/NodeDatahubToolbar.tsx'
import type { DataHubNodeData } from '@datahub/types.ts'
import { DataHubNodeType, PolicyDryRunStatus } from '@datahub/types.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import { parseHotkey } from '@datahub/utils/hotkeys.utils.ts'
import { DATAHUB_HOTKEY } from '@datahub/utils/datahub.utils.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { NodeIcon } from '@datahub/components/helpers'
import { useTranslation } from 'react-i18next'

interface NodeWrapperProps extends NodeProps<DataHubNodeData> {
  children: ReactNode
  route: string
  wrapperProps?: CardBodyProps
  toolbar?: React.ReactNode
}

export const NodeWrapper: FC<NodeWrapperProps> = ({
  selected,
  children,
  toolbar,
  route,
  wrapperProps,
  data,
  type,
  ...props
}) => {
  const { t } = useTranslation('datahub')
  const { nodes } = useDataHubDraftStore()
  const [internalSelection, setInternalSelection] = useState(false)
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const headerBackgroundColor = useColorModeValue('blue.200', 'blue.700')
  const headerPolicyBackgroundColor = useColorModeValue('orange.200', 'orange.700')
  const headerResourceBackgroundColor = useColorModeValue('pink.200', 'pink.700')

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

  const backgroundColor =
    type === DataHubNodeType.DATA_POLICY || type === DataHubNodeType.BEHAVIOR_POLICY
      ? headerPolicyBackgroundColor
      : type === DataHubNodeType.SCHEMA || type === DataHubNodeType.FUNCTION
      ? headerResourceBackgroundColor
      : headerBackgroundColor

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
        <NodeDatahubToolbar
          selectedNode={props.id}
          onCopy={onHandleCopy}
          onDelete={onHandleDelete}
          onEdit={onHandleEdit}
        >
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
        w="250px"
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
        <CardHeader backgroundColor={backgroundColor} as={HStack} height={12}>
          <NodeIcon type={type} />
          <Text data-testid="node-title"> {t('workspace.nodes.type', { context: type })}</Text>
        </CardHeader>
        <CardBody {...wrapperProps}>{children}</CardBody>
      </Card>
    </>
  )
}
