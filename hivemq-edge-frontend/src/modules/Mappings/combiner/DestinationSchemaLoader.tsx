import { type FC, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { JSONSchema7 } from 'json-schema'
import {
  Button,
  ButtonGroup,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalFooter,
  ModalBody,
  ModalCloseButton,
  VStack,
  useDisclosure,
  FormLabel,
  FormHelperText,
  FormControl,
  HStack,
  Icon,
  Box,
} from '@chakra-ui/react'
import { LuDownload, LuUpload } from 'react-icons/lu'
import { RiAiGenerate } from 'react-icons/ri'

import type { DataCombining, Instruction } from '@/api/__generated__'
import type { DataIdentifierReference } from '@/api/__generated__'
import IconButton from '@/components/Chakra/IconButton'
import ErrorMessage from '@/components/ErrorMessage'
import { SelectEntityType } from '@/components/MQTT/types'
import { MappingInstructionList } from '@/components/rjsf/MqttTransformation/components/MappingInstructionList'
import { toJsonPath } from '@/components/rjsf/MqttTransformation/utils/data-type.utils'
import {
  type FlatJSONSchema7,
  getSchemaFromPropertyList,
} from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'
import { AccessibleDraggableLock } from '@/hooks/useAccessibleDraggable'
import SchemaUploader from '@/modules/TopicFilters/components/SchemaUploader'
import { encodeDataUriJsonSchema, validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema'
import { downloadJSON } from '@/utils/download.utils'

import type { CombinerContext } from '../types'
import SchemaMerger from './SchemaMerger'

interface DestinationSchemaLoaderProps {
  title: string | undefined
  description: string | undefined
  isInvalid: boolean
  formData?: DataCombining
  formContext?: CombinerContext
  onChange: (schema: string, v?: Instruction[]) => void
  onChangeInstructions: (v: Instruction[]) => void
}

enum EDITOR_MODE {
  UPLOADER = 'UPLOADER',
  INFERRER = 'INFERRER',
}

export const DestinationSchemaLoader: FC<DestinationSchemaLoaderProps> = ({
  formData,
  formContext,
  onChange,
  onChangeInstructions,
  title,
  description,
  isInvalid,
}) => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const [schemaEditor, setSchemaEditor] = useState<EDITOR_MODE | undefined>(undefined)

  const isTopicDefined = Boolean(formData?.destination?.topic && formData?.destination?.topic !== '')
  const isDestSchemaDefined = Boolean(formData?.destination?.schema && formData?.destination?.schema !== '')

  const handleSchemaEditor = (mode: EDITOR_MODE | undefined) => {
    setSchemaEditor(mode)
    onOpen()
  }

  const handleSchemaUpload = (schema: string) => {
    onChange(schema)
  }

  const handleSchemaMerge = (properties: FlatJSONSchema7[]) => {
    const schema = getSchemaFromPropertyList(properties)
    const destinationSchema = encodeDataUriJsonSchema(schema)
    const autoInstructions = properties.map<Instruction>((property) => {
      const { id, type } = property.metadata || {}
      const instruction: DataIdentifierReference = { id: id as string, type: type as DataIdentifierReference.type }
      const [, ...source] = property.key.split('_')
      return {
        sourceRef: instruction,
        destination: toJsonPath([...property.path, property.key].join('.')),
        source: toJsonPath([...property.path, source.join('_')].join('.')),
      }
    })

    onChange(destinationSchema, autoInstructions)
    onClose()
  }

  const handleSchemaDownload = () => {
    if (!formData?.destination?.schema) return

    const handler = validateSchemaFromDataURI(formData?.destination?.schema, SelectEntityType.TOPIC)
    if (handler.schema) downloadJSON<JSONSchema7>(handler.schema.title || 'topic-untitled', handler.schema)
  }

  const handleInstructionChange = (v: Instruction[] | undefined) => {
    if (v) onChangeInstructions(v)
  }

  const schema = useMemo(() => {
    if (!formData?.destination?.schema) return undefined
    return validateSchemaFromDataURI(formData?.destination?.schema, SelectEntityType.TOPIC)
  }, [formData?.destination?.schema])

  return (
    <FormControl isInvalid={isInvalid}>
      <HStack>
        <FormLabel flex={3} marginEnd={0}>
          {title}
        </FormLabel>
        <ButtonGroup
          isAttached
          size="sm"
          variant="outline"
          justifyContent={'flex-end'}
          flexWrap={'wrap'}
          rowGap={2}
          mb={1}
        >
          <IconButton
            icon={<Icon as={RiAiGenerate} />}
            data-testid={'combiner-destination-infer'}
            isDisabled={!isTopicDefined}
            onClick={() => handleSchemaEditor(EDITOR_MODE.INFERRER)}
            aria-label={t('combiner.schema.schemaManager.action.infer')}
          />
          <IconButton
            icon={<Icon as={LuUpload} />}
            data-testid={'combiner-destination-upload'}
            isDisabled={!isTopicDefined}
            onClick={() => handleSchemaEditor(EDITOR_MODE.UPLOADER)}
            aria-label={t('combiner.schema.schemaManager.action.upload')}
          />

          <IconButton
            icon={<Icon as={LuDownload} />}
            data-testid={'combiner-destination-download'}
            onClick={handleSchemaDownload}
            isDisabled={!isDestSchemaDefined}
            aria-label={t('combiner.schema.schemaManager.action.download')}
          ></IconButton>
        </ButtonGroup>
      </HStack>

      {!formData?.destination?.schema && (
        <Box borderWidth={1} p={3}>
          <ErrorMessage message={t('combiner.error.noSchemaLoadedYet')} status={'info'} />
        </Box>
      )}

      {schema?.status === 'error' && (
        <Box borderWidth={1} p={3}>
          <ErrorMessage type={schema?.message} message={schema?.error} />
        </Box>
      )}

      {schema?.schema && (
        <AccessibleDraggableLock>
          <VStack w="100%" justifyContent={'center'} alignItems={'stretch'} gap={3}>
            <MappingInstructionList
              id={'destination-mapping-editor'}
              schema={schema.schema}
              instructions={formData?.instructions || []}
              onChange={handleInstructionChange}
              display={'flex'}
              flexDirection={'column'}
              gap={4}
            />
          </VStack>
        </AccessibleDraggableLock>
      )}

      <Modal isOpen={isOpen && Boolean(schemaEditor)} onClose={onClose} id={'destination-schema'}>
        <ModalOverlay />
        <ModalContent>
          <ModalCloseButton />
          <ModalHeader>
            {schemaEditor === EDITOR_MODE.INFERRER && t('combiner.schema.schemaManager.action.infer')}
            {schemaEditor === EDITOR_MODE.UPLOADER && t('combiner.schema.schemaManager.action.upload')}
          </ModalHeader>
          {schemaEditor === EDITOR_MODE.INFERRER && (
            <SchemaMerger
              formData={formData}
              formContext={formContext}
              onUpload={handleSchemaMerge}
              onClose={onClose}
            />
          )}
          {schemaEditor === EDITOR_MODE.UPLOADER && (
            <>
              <ModalBody>
                <SchemaUploader onUpload={handleSchemaUpload} />
              </ModalBody>
              <ModalFooter>
                <Button onClick={onClose}> {t('action.cancel')}</Button>
              </ModalFooter>
            </>
          )}
        </ModalContent>
      </Modal>
      <FormHelperText>{description}</FormHelperText>
    </FormControl>
  )
}
