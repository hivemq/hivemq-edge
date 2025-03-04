import type { FC } from 'react'
import type { FieldProps, RJSFSchema } from '@rjsf/utils'
import { Box, HStack, Icon, Stack, VStack } from '@chakra-ui/react'
import { FaRightFromBracket } from 'react-icons/fa6'

import type { DataCombining, Instruction } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { SelectTopic } from '@/components/MQTT/EntityCreatableSelect'
import type { CombinerContext } from '@/modules/Mappings/types'
import CombinedEntitySelect from './CombinedEntitySelect'
import { CombinedSchemaLoader } from './CombinedSchemaLoader'
import { DestinationSchemaLoader } from './DestinationSchemaLoader'
import { PrimarySelect } from './PrimarySelect'

export const DataCombiningEditorField: FC<FieldProps<DataCombining, RJSFSchema, CombinerContext>> = (props) => {
  const { formData, formContext } = props

  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <VStack flex={1} alignItems="stretch" maxW="40vw">
          <Box>
            <CombinedEntitySelect
              tags={formData?.sources?.tags}
              topicFilters={formData?.sources?.topicFilters}
              formContext={formContext}
              onChange={(values) => {
                if (!formData) return
                const tag: string[] = []
                const filter: string[] = []
                values.forEach((entity) => {
                  if (entity.type === DataIdentifierReference.type.TAG) tag.push(entity.value)
                  if (entity.type === DataIdentifierReference.type.TOPIC_FILTER) filter.push(entity.value)
                })

                props.onChange({
                  ...formData,
                  // @ts-ignore TODO check for type clash on primary
                  sources: { ...formData.sources, tags: tag, topicFilters: filter },
                })
              }}
            />
          </Box>
          <VStack height={'60vh'} overflow={'auto'} alignItems={'flex-start'} justifyContent={'center'} tabIndex={0}>
            <CombinedSchemaLoader formData={props.formData} formContext={formContext} />
          </VStack>
          <Box>
            <PrimarySelect
              formData={formData}
              onChange={(values) => {
                if (!props.formData) return

                props.onChange({
                  ...props.formData,
                  sources: {
                    ...props.formData.sources,
                    // @ts-ignore TODO[30935] check for type clash on primary
                    primary: values
                      ? {
                          id: values.value,
                          type: values.type,
                        }
                      : undefined,
                  },
                })
              }}
            />
          </Box>
        </VStack>
        <VStack justifyContent="center">
          <HStack height={38}>
            <Icon as={FaRightFromBracket} />
          </HStack>
        </VStack>
        <VStack flex={1} alignItems="stretch" maxW="50vw">
          <Box maxW={'25vw'}>
            <SelectTopic
              isMulti={false}
              isCreatable={true}
              id={'destination'}
              value={formData?.destination?.topic || null}
              onChange={(topic) => {
                if (!props.formData) return

                if (topic && typeof topic === 'string') props.onChange({ ...props.formData, destination: { topic } })
                else if (!topic) props.onChange({ ...props.formData, destination: { topic: '' } })
              }}
            />
          </Box>
          <DestinationSchemaLoader
            formData={props.formData}
            onChange={(schema) => {
              if (!props.formData) return

              props.onChange({
                ...props.formData,
                destination: { topic: props.formData.destination.topic, schema },
              })
            }}
            onChangeInstructions={(v: Instruction[]) => {
              if (!props.formData) return
              if (!v.length) return

              props.onChange({
                ...props.formData,
                instructions: v,
              })
            }}
          />
        </VStack>
      </Stack>
    </VStack>
  )
}
