import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, CardBody, CardHeader, CardProps, Heading, HStack, VStack } from '@chakra-ui/react'
import { LuWand } from 'react-icons/lu'

import { useGetSubscriptionSchemas } from '@/api/hooks/useTopicOntology/useGetSubscriptionSchemas.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import MappingInstruction from '@/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { Mapping } from '@/modules/Subscriptions/types.ts'

interface MappingEditorProps extends Omit<CardProps, 'onChange'> {
  topic: string
  showTransformation?: boolean
  mapping?: Mapping[]
  onChange?: (v: Mapping[] | undefined) => void
}

const MappingEditor: FC<MappingEditorProps> = ({ topic, showTransformation = false, mapping, onChange, ...props }) => {
  const { t } = useTranslation('components')
  const { data, isLoading } = useGetSubscriptionSchemas(topic as string, topic ? 'destination' : undefined)
  const properties = data ? getPropertyListFrom(data) : []

  return (
    <Card {...props} size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading as="h3" size="sm">
          {t('rjsf.MqttTransformationField.mapping.header')}
        </Heading>
        <Button size="xs" leftIcon={<LuWand />}>
          {t('rjsf.MqttTransformationField.mapping.auto.aria-label')}
        </Button>
      </CardHeader>
      <CardBody as={VStack} maxH="60vh" overflowY="scroll">
        {isLoading && <LoaderSpinner />}

        {properties.map((property) => {
          const instruction = mapping?.findIndex((e) => e.destination === property.title) || -1
          return (
            <MappingInstruction
              showTransformation={showTransformation}
              property={property}
              key={property.title}
              mapping={instruction ? mapping?.[instruction] : undefined}
              onChange={(source, destination) => {
                const newMappings = [...(mapping || [])]
                const newItem = { source: [source], destination: destination }
                if (instruction !== -1) {
                  newMappings[instruction] = newItem
                } else newMappings.push(newItem)
                onChange?.(newMappings)
              }}
            />
          )
        })}
      </CardBody>
    </Card>
  )
}

export default MappingEditor
