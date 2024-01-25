import { FC } from 'react'
import { HStack, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { NodeProps, Position } from 'reactflow'

import { DataHubNodeType, ValidatorData } from '../../types.ts'
import NodeIcon from '../helpers/NodeIcon.tsx'
import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'

export const ValidatorNode: FC<NodeProps<ValidatorData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props
  return (
    <>
      <NodeWrapper route={`node/${DataHubNodeType.VALIDATOR}/${id}`} {...props}>
        <HStack>
          <NodeIcon type={DataHubNodeType.VALIDATOR} />
          <Text data-testid={'node-title'} w={'50%'}>
            {t('workspace.nodes.type', { context: type })}
          </Text>
          <VStack data-testid={'node-model'}>
            <Text>{data.type}</Text>
            <Text>{data.strategy}</Text>
          </VStack>
        </HStack>
      </NodeWrapper>
      <CustomHandle type="target" position={Position.Top} id="target" />
      <CustomHandle type="source" position={Position.Bottom} id="source" />
    </>
  )
}
