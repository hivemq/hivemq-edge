import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { CardProps } from '@chakra-ui/react'
import { Button, Card, CardBody, CardHeader, Heading, HStack } from '@chakra-ui/react'
import { LuWand } from 'react-icons/lu'

import type { Instruction } from '@/api/__generated__'
import { useGetWritingSchema } from '@/api/hooks/useProtocolAdapters/useGetWritingSchema.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { filterSupportedProperties } from '@/components/rjsf/MqttTransformation/utils/data-type.utils.ts'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { MappingInstructionList } from './MappingInstructionList'

interface MappingEditorProps extends Omit<CardProps, 'onChange'> {
  topic: string
  adapterType: string
  adapterId: string
  showTransformation?: boolean
  instructions?: Instruction[]
  onChange?: (v: Instruction[] | undefined) => void
  onSchemaReady?: (v: FlatJSONSchema7[]) => void
}

const MappingEditor: FC<MappingEditorProps> = ({
  topic,
  showTransformation = false,
  instructions,
  onChange,
  onSchemaReady,
  adapterId,
  adapterType,
  ...props
}) => {
  const { t } = useTranslation('components')
  const { data, isLoading, isError, error, isSuccess } = useGetWritingSchema(adapterId, topic)

  const properties = useMemo(() => {
    const allProperties = data ? getPropertyListFrom(data) : []
    return allProperties.filter(filterSupportedProperties)
  }, [data])

  useEffect(() => {
    if (properties.length) onSchemaReady?.(properties)
    // TODO[NVL] Infinite re-rendering prevented by removing callback from dependencies; check if side effects
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [properties, properties.length])

  return (
    <Card {...props} size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading as="h3" size="sm">
          {t('rjsf.MqttTransformationField.mapping.header')}
        </Heading>
        <Button data-testid="auto-mapping" size="sm" leftIcon={<LuWand />} isDisabled>
          {t('rjsf.MqttTransformationField.mapping.auto.aria-label')}
        </Button>
      </CardHeader>
      <CardBody maxH="58vh" overflowY="scroll">
        {isLoading && <LoaderSpinner />}
        {isError && error && <ErrorMessage message={error.message} />}
        {!isSuccess && !isError && !isLoading && (
          <ErrorMessage message={t('rjsf.MqttTransformationField.destination.prompt')} status="info" />
        )}
        {isSuccess && instructions && (
          <MappingInstructionList
            instructions={instructions}
            schema={data}
            onChange={onChange}
            showTransformation={showTransformation}
          />
        )}
      </CardBody>
    </Card>
  )
}

export default MappingEditor
