import { FC, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { dropTargetForElements } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
import { Button, Card, CardBody, CardHeader, CardProps, Heading, HStack, VStack } from '@chakra-ui/react'
import { LuWand } from 'react-icons/lu'

import { useGetSubscriptionSchemas } from '@/api/hooks/useTopicOntology/useGetSubscriptionSchemas.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import MappingInstruction from '@/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

interface MappingEditorProps extends CardProps {
  topic: string
  showTransformation?: boolean
}

const MappingEditor: FC<MappingEditorProps> = ({ topic, showTransformation = false, ...props }) => {
  const { t } = useTranslation('components')
  const { data, isLoading } = useGetSubscriptionSchemas(topic as string, topic ? 'activated_short' : undefined)
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

        {properties.map((e) => (
          <MappingInstruction showTransformation={showTransformation} property={e} key={e.title} />
        ))}
      </CardBody>
    </Card>
  )
}

export default MappingEditor