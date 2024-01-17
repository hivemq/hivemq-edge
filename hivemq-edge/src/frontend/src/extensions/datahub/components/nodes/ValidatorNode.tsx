import { FC } from 'react'
import { HStack, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { Handle, NodeProps, Position } from 'reactflow'

import { DataHubNodeType, ValidatorData } from '../../types.ts'
import { styleSourceHandle } from '../../utils/node.utils.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'

export const ValidatorNode: FC<NodeProps<ValidatorData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props
  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.VALIDATOR}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.VALIDATOR} />
          <Text w={'50%'}> {t('workspace.nodes.type', { context: type })}</Text>
          <VStack>
            <Text>{data.strategy}</Text>
            <Text>{data.type}</Text>
          </VStack>
        </HStack>
      </NodeWrapper>

      <Handle type="target" position={Position.Top} id="schema" />
      <Handle type="source" position={Position.Bottom} id="policy" style={styleSourceHandle} />
    </>
  )
}
