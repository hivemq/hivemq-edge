import type { FC } from 'react'
import type { TagProps } from '@chakra-ui/react'
import { Tag, TagLabel } from '@chakra-ui/react'

interface NodeParamsProps extends TagProps {
  value: string
}

const NodeParams: FC<NodeParamsProps> = ({ value, ...tagProps }) => {
  return (
    <Tag as="p" letterSpacing="-0.05rem" fontFamily="monospace" {...tagProps}>
      <TagLabel>{value}</TagLabel>
    </Tag>
  )
}

export default NodeParams
