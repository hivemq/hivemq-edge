import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  ButtonGroup,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  FormControl,
  HStack,
  List,
  Textarea,
} from '@chakra-ui/react'
import { RiDeleteBin2Fill, RiFormula } from 'react-icons/ri'

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
  const { t } = useTranslation('components')
  return (
    <HStack>
      <Card size="sm" variant="outline" flex={1}>
        <CardHeader>
          <List>
            <PropertyItem property={property} />
          </List>
        </CardHeader>
        <CardBody {...getDropZoneBorder('blue')} m={2} p={4}>
          {t('rjsf.MqttTransformationField.instructions.dropzone.arial-label')}
        </CardBody>
        <CardFooter>
          <ButtonGroup isAttached size="xs" isDisabled>
            <IconButton
              aria-label={t('rjsf.MqttTransformationField.instructions.actions.edit.aria-label')}
              icon={<RiFormula />}
            />
          </ButtonGroup>
          <FormControl isDisabled>
            <Textarea size="xs" aria-label="ssss" />
          </FormControl>
        </CardFooter>
      </Card>
      <ButtonGroup isAttached size="xs">
        <IconButton
          aria-label={t('rjsf.MqttTransformationField.instructions.actions.clear.aria-label')}
          icon={<RiDeleteBin2Fill />}
        />
      </ButtonGroup>
    </HStack>
  )
}

export default MappingInstruction
