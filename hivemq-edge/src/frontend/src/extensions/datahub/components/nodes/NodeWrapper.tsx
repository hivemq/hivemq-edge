import { FC, ReactNode, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { NodeProps } from 'reactflow'
import { BoxProps, Card, CardBody, CardBodyProps, HStack } from '@chakra-ui/react'

interface NodeWrapperProps extends NodeProps {
  children: ReactNode
  route: string
  wrapperProps?: CardBodyProps
}

export const NodeWrapper: FC<NodeWrapperProps> = ({ selected, children, route, wrapperProps }) => {
  const [internalSelection, setInternalSelection] = useState(false)
  const navigate = useNavigate()
  const { pathname } = useLocation()

  useEffect(() => {
    if (!selected) setInternalSelection(false)
  }, [selected])

  const selectedStyle: Partial<BoxProps> = {
    boxShadow: 'var(--chakra-shadows-outline)',
  }

  return (
    <Card
      variant="elevated"
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
