import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { JSONSchema7 } from 'json-schema'
import { Card, CardBody, CardHeader, CardProps, Heading, HStack } from '@chakra-ui/react'
import { RxReload } from 'react-icons/rx'

import IconButton from '@/components/Chakra/IconButton.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import { useGetSubscriptionSchemas } from '@/api/hooks/useTopicOntology/useGetSubscriptionSchemas.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'

interface DataModelSourcesProps extends CardProps {
  topics: string[]
}

const DataModelSources: FC<DataModelSourcesProps> = ({ topics, ...props }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetSubscriptionSchemas(topics, topics ? 'source' : undefined)

  const structuredSchema = useMemo(() => {
    return Object.keys(data || {}).reduce<JSONSchema7[]>((acc, schemaId) => {
      if (data?.[schemaId]) {
        const newData: JSONSchema7 = { ...(data?.[schemaId] as JSONSchema7), title: schemaId }
        acc.push(newData)
      }
      return acc
    }, [])
  }, [data])

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
          structuredSchema.map((schema) => (
            <JsonSchemaBrowser schema={schema} isDraggable hasExamples key={schema.title} />
          ))}
      </CardBody>
    </Card>
  )
}

export default DataModelSources
