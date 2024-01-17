import { FC } from 'react'
import { Handle, NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text } from '@chakra-ui/react'

import { DataHubNodeType, DataPolicyData } from '../../types.ts'
import { styleSourceHandle } from '../../utils/node.utils.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'

export const DataPolicyNode: FC<NodeProps<DataPolicyData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, type } = props

  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.DATA_POLICY}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.DATA_POLICY} />
          <Text> {t('workspace.nodes.type', { context: type })}</Text>
        </HStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Left} id={DataPolicyData.Handle.TOPIC_FILTER} />
      <Handle type="target" position={Position.Top} id={DataPolicyData.Handle.VALIDATION} />
      <Handle
        type="source"
        position={Position.Right}
        id={DataPolicyData.Handle.ON_SUCCESS}
        style={{
          top: 10,
          background: 'green',
          ...styleSourceHandle,
        }}
      />
      <Handle
        type="source"
        position={Position.Right}
        id={DataPolicyData.Handle.ON_ERROR}
        style={{
          top: 'calc(100% - 10px)',
          background: 'red',
          ...styleSourceHandle,
        }}
      />
      {/*<Handle type="source" position={Position.Right} id="finally" />*/}
    </>
  )
}
