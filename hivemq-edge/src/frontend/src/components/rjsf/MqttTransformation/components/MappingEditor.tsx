import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { CardProps } from '@chakra-ui/react'
import { Button, Card, CardBody, CardHeader, Heading, HStack, List, ListItem } from '@chakra-ui/react'
import { LuWand } from 'react-icons/lu'

import type { Instruction } from '@/api/__generated__'
import { useGetWritingSchema } from '@/api/hooks/useProtocolAdapters/useGetWritingSchema.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { filterSupportedProperties } from '@/components/rjsf/MqttTransformation/utils/data-type.utils.ts'
import MappingInstruction from '@/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

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
        {isSuccess && (
          <List>
            {properties.map((property) => {
              const instruction = instructions
                ? instructions.findIndex((instruction) => instruction.destination === property.key)
                : -1
              return (
                <ListItem key={property.key}>
                  <MappingInstruction
                    showTransformation={showTransformation}
                    property={property}
                    instruction={instruction !== -1 ? instructions?.[instruction] : undefined}
                    onChange={(source, destination) => {
                      let newMappings = [...(instructions || [])]
                      if (source) {
                        const newItem: Instruction = {
                          source: source,
                          destination: destination,
                        }
                        if (instruction !== -1) {
                          newMappings[instruction] = newItem
                        } else newMappings.push(newItem)
                      } else {
                        newMappings = newMappings.filter((mapped) => mapped.destination !== destination)
                      }

                      onChange?.(newMappings)
                    }}
                  />
                </ListItem>
              )
            })}
          </List>
        )}
      </CardBody>
    </Card>
  )
}

export default MappingEditor
