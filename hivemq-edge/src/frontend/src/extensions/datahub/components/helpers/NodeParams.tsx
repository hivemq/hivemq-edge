import { FC } from 'react'
import { Tag, TagLabel, TagProps } from '@chakra-ui/react'

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
