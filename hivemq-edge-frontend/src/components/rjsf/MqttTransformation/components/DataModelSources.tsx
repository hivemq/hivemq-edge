import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { JSONSchema7 } from 'json-schema'
import type { CardProps } from '@chakra-ui/react'
import { Card, CardBody, CardHeader, Heading, HStack } from '@chakra-ui/react'
import { RxReload } from 'react-icons/rx'

import { useSamplingForTopic } from '@/api/hooks/useDomainModel/useSamplingForTopic.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import type { JsonNode } from '@/api/__generated__'

interface DataModelSourcesProps extends Omit<CardProps, 'onChange'> {
  topic: string
  onChange?: (v: JsonNode | undefined) => void
}

const DataModelSources: FC<DataModelSourcesProps> = ({ topic, onChange, ...props }) => {
  const { t } = useTranslation()
  const { schema, isLoading, isError, error } = useSamplingForTopic(topic)

  const structuredSchema = useMemo(() => {
    if (!schema) return [] as JSONSchema7[]
    return [{ ...schema, title: topic }] as JSONSchema7[]
  }, [schema, topic])

  useEffect(() => {
    onChange?.(schema)
  }, [onChange, schema])

  return (
    <Card {...props} size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading as="h3" size="sm">
          {t('components:rjsf.MqttTransformationField.sources.header')}
        </Heading>
        <IconButton
          size="sm"
          icon={<RxReload />}
          aria-label={t('components:rjsf.MqttTransformationField.sources.samples.aria-label')}
          isDisabled
        />
      </CardHeader>

      <CardBody maxH="55vh" overflowY="scroll">
        {isLoading && <LoaderSpinner />}
        {isError && error && <ErrorMessage message={error.message} />}
        {!isLoading &&
          schema &&
          structuredSchema.map((schema) => (
            <JsonSchemaBrowser schema={schema} isDraggable hasExamples key={schema.title} />
          ))}
      </CardBody>
    </Card>
  )
}

export default DataModelSources
