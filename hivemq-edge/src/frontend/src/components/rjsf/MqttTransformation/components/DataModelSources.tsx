import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { JSONSchema7 } from 'json-schema'
import { Card, CardBody, CardHeader, CardProps, Heading, HStack } from '@chakra-ui/react'
import { RxReload } from 'react-icons/rx'

import IconButton from '@/components/Chakra/IconButton.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { useGetTopicSchema } from '@/api/hooks/_deprecated/useGetTagSchema.ts'

interface DataModelSourcesProps extends CardProps {
  topic: string | undefined
}

const DataModelSources: FC<DataModelSourcesProps> = ({ topic, ...props }) => {
  const { t } = useTranslation()

  const { data, isLoading, isError, error, isSuccess } = useGetTopicSchema(topic)

  const structuredSchema = useMemo(() => {
    if (!data) return [] as JSONSchema7[]
    return [{ ...data, title: topic }] as JSONSchema7[]
  }, [data, topic])

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
        {!isSuccess && !isError && !isLoading && (
          <ErrorMessage message={t('components:rjsf.MqttTransformationField.sources.prompt')} status="info" />
        )}
        {isSuccess &&
          structuredSchema.map((schema) => (
            <JsonSchemaBrowser schema={schema} isDraggable hasExamples key={schema.title} />
          ))}
      </CardBody>
    </Card>
  )
}

export default DataModelSources
