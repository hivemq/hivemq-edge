import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Handle, NodeProps, Position } from 'reactflow'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, OperationData } from '../../types.ts'
import { styleSourceHandle } from '../../utils/node.utils.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'

export const OperationNode: FC<NodeProps<OperationData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { data, id, type } = props
  const { action } = data

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.OPERATION}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.OPERATION} />
          <Text> {t('workspace.nodes.type', { context: type })}</Text>
          <VStack>
            <Text>{action?.functionId || '< none >'}</Text>
          </VStack>
        </HStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Left} id={'target'} />
      {!action?.isTerminal && (
        <Handle type="source" position={Position.Right} id={'source'} style={styleSourceHandle} />
      )}
      {action?.hasArguments && <Handle type="target" position={Position.Top} id={'schema'} />}
    </>
  )
}
