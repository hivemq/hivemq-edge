import { FC, ReactNode, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { NodeProps } from 'reactflow'

import { BoxProps, Card, CardBody, CardBodyProps, HStack } from '@chakra-ui/react'
import { DataHubNodeData, PolicyDryRunStatus } from '@datahub/types.ts'

interface NodeWrapperProps extends NodeProps<DataHubNodeData> {
  children: ReactNode
  route: string
  wrapperProps?: CardBodyProps
}

export const NodeWrapper: FC<NodeWrapperProps> = ({ selected, children, route, wrapperProps, ...rest }) => {
  const [internalSelection, setInternalSelection] = useState(false)
  const navigate = useNavigate()
  const { pathname } = useLocation()

  useEffect(() => {
    if (!selected) setInternalSelection(false)
  }, [selected])

  const selectedStyle: Partial<BoxProps> = {
    boxShadow: 'var(--chakra-shadows-outline)',
  }

  const errorStyle: Partial<BoxProps> = {
    boxShadow: '0 0 10px 2px rgba(226, 85, 85, 0.75), 0 1px 1px rgb(0 0 0 / 15%)',
  }

  const successStyle: Partial<BoxProps> = {
    boxShadow: '0 0 10px 2px rgba(0,121,36, 0.75), 0 1px 1px rgb(0 0 0 / 15%)',
  }

  return (
    <Card
      variant="elevated"
      {...(rest.data.dryRunStatus === PolicyDryRunStatus.FAILURE ? { ...errorStyle } : {})}
      {...(rest.data.dryRunStatus === PolicyDryRunStatus.SUCCESS ? { ...successStyle } : {})}
      {...(selected ? { ...selectedStyle } : {})}
      size="sm"
      onClick={(event) => {
        if (internalSelection) {
          navigate(route, { state: { origin: pathname } })
          event.preventDefault()
          event.stopPropagation()
        }
        setInternalSelection(true)
      }}
    >
      <CardBody as={HStack} {...wrapperProps}>
        {children}
      </CardBody>
    </Card>
  )
}
