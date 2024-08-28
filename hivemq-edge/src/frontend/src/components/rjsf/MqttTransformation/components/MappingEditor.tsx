import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { RJSFSchema } from '@rjsf/utils'
import { Button, Card, CardBody, CardHeader, CardProps, Heading, HStack, VStack } from '@chakra-ui/react'
import { LuWand } from 'react-icons/lu'

import MOCK_SCHEMA from '@datahub/api/__generated__/schemas/TransitionData.json'
import MappingInstruction from '@/components/rjsf/MqttTransformation/components/MappingInstruction.tsx'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

interface MappingEditorProps extends CardProps {
  id?: string
}

const MappingEditor: FC<MappingEditorProps> = ({ id, ...props }) => {
  const { t } = useTranslation('components')
  const properties = getPropertyListFrom(MOCK_SCHEMA as RJSFSchema)
  return (
    <Card {...props}>
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading as="h3" size="sm">
          {t('rjsf.MqttTransformationField.mapping.header')}
        </Heading>
        <Button size="xs" leftIcon={<LuWand />}>
          {t('rjsf.MqttTransformationField.mapping.auto.aria-label')}
        </Button>
      </CardHeader>
      <CardBody as={VStack}>
        {properties.map((e) => (
          <MappingInstruction property={e} key={e.title} />
        ))}
      </CardBody>
    </Card>
  )
}

export default MappingEditor
