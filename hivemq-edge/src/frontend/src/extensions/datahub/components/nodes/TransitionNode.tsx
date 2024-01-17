import { FC } from 'react'
import { HStack, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { Handle, NodeProps, Position } from 'reactflow'

import { DataHubNodeType, TransitionData } from '../../types.ts'
import { styleSourceHandle } from '../../utils/node.utils.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'

export const TransitionNode: FC<NodeProps<TransitionData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { data, id, type } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.TRANSITION}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.TRANSITION} />
          <Text> {t('workspace.nodes.type', { context: type })}</Text>
          <VStack>
            <Text>{data.type || '< none >'}</Text>
          </VStack>
        </HStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Left} id={'target'} />
      <Handle type="source" position={Position.Right} id={'source'} style={styleSourceHandle} />
    </>
  )
}
