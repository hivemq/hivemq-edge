import { FC } from 'react'
import { ButtonGroup, Card, CardBody, CardHeader, HStack, List } from '@chakra-ui/react'
import { RxReset } from 'react-icons/rx'

import IconButton from '@/components/Chakra/IconButton.tsx'
import PropertyItem from '@/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx'
import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

const getDropZoneBorder = (color: string) => {
  return {
    bgGradient: `repeating-linear(0deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(90deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(180deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(270deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px)`,
    backgroundSize: '2px 100%, 100% 2px, 2px 100% , 100% 2px',
    backgroundPosition: '0 0, 0 0, 100% 0, 0 100%',
    backgroundRepeat: 'no-repeat',
    borderRadius: '4px',
  }
}

interface MappingInstructionProps {
  property: FlatJSONSchema7
}

const MappingInstruction: FC<MappingInstructionProps> = ({ property }) => {
  console.log('xxx', property)
  return (
    <HStack>
      <Card size="sm" variant="outline" flex={1}>
        <CardHeader>
          <List>
            <PropertyItem property={property} />
          </List>
        </CardHeader>
        <CardBody {...getDropZoneBorder('blue')} m={2} p={4}>
          Drag a source property here
        </CardBody>
      </Card>
      <ButtonGroup isAttached size="xs">
        <IconButton aria-label="Clear mapping" icon={<RxReset />} />
      </ButtonGroup>
    </HStack>
  )
}

export default MappingInstruction
