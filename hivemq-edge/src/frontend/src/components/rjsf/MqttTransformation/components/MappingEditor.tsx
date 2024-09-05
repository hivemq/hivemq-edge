import { FC, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { dropTargetForElements } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
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
  const ref = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const element = ref.current
    if (!element) return

    return dropTargetForElements({
      element,
      canDrop: () => false,
    })
  }, [])

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
      <CardBody as={VStack} maxH="60vh" overflowY="scroll">
        {isLoading && <LoaderSpinner />}

        {properties.map((property) => {
          const instruction = mapping?.findIndex((e) => e.destination === property.title) || -1
          console.log('XXXXX prop', property, mapping, instruction)
          return (
            <MappingInstruction
              showTransformation={showTransformation}
              property={property}
              key={property.title}
              mapping={instruction ? mapping?.[instruction] : undefined}
              onChange={(s, d) => {
                console.log('XXXXX', s, d)
                const gg = [...(mapping || [])]
                const hhhh = { source: [s], destination: d }
                if (instruction !== -1) {
                  gg[instruction] = hhhh
                } else gg.push(hhhh)
                onChange?.(gg)
              }}
            />
          )
        })}
      </CardBody>
    </Card>
  )
}

export default MappingEditor
