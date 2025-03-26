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
  Text,
  VStack,
  useDisclosure,
} from '@chakra-ui/react'

import type { DataCombining, Instruction } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage'
import { MappingInstructionList } from '@/components/rjsf/MqttTransformation/components/MappingInstructionList'
import SchemaUploader from '@/modules/TopicFilters/components/SchemaUploader'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema'
import { downloadJSON } from '@/utils/download.utils'

interface DestinationSchemaLoaderProps {
  formData?: DataCombining
  onChange: (schema: string) => void
  onChangeInstructions: (v: Instruction[]) => void
}

enum EDITOR_MODE {
  UPLOADER = 'UPLOADER',
  INFERRER = 'INFERRER',
}

export const DestinationSchemaLoader: FC<DestinationSchemaLoaderProps> = ({
  formData,
  onChange,
  onChangeInstructions,
}) => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const [isSchemaEditor, setSchemaEditor] = useState<EDITOR_MODE | undefined>(undefined)

  const isTopicDefined = Boolean(formData?.destination?.topic && formData?.destination?.topic !== '')
  const isDestSchemaDefined = Boolean(formData?.destination?.schema && formData?.destination?.schema !== '')
  const isSourceSchemaDefined = useMemo(() => {
    return true
  }, [])

  const handleSchemaEditor = (mode: EDITOR_MODE | undefined) => {
    setSchemaEditor(mode)
    onOpen()
  }

  const handleSchemaUpload = (schema: string) => {
    onChange(schema)
  }

  const handleSchemaDownload = () => {
    if (!formData?.destination?.schema) return

    const handler = validateSchemaFromDataURI(formData?.destination?.schema)
    if (handler.schema) downloadJSON<JSONSchema7>(handler.schema.title || 'topic-untitled', handler.schema)
  }

  const handleInstructionChange = (v: Instruction[] | undefined) => {
    if (v) onChangeInstructions(v)
  }

  const schema = useMemo(() => {
    if (!formData?.destination?.schema) return undefined
    return validateSchemaFromDataURI(formData?.destination?.schema)
  }, [formData?.destination?.schema])

  return (
    <>
      <ButtonGroup size="sm" variant="outline" flexWrap={'wrap'} rowGap={2} mb={2}>
        <Button
          data-testid={'combiner-destination-infer'}
          isDisabled={!isSourceSchemaDefined}
          onClick={() => handleSchemaEditor(EDITOR_MODE.INFERRER)}
        >
          {t('combiner.schema.schemaManager.action.infer')}
        </Button>
        <Button
          data-testid={'combiner-destination-upload'}
          isDisabled={!isTopicDefined}
          onClick={() => handleSchemaEditor(EDITOR_MODE.UPLOADER)}
        >
          {t('combiner.schema.schemaManager.action.upload')}
        </Button>

        <Button
          data-testid={'combiner-destination-download'}
          onClick={handleSchemaDownload}
          isDisabled={!isDestSchemaDefined}
        >
          {t('combiner.schema.schemaManager.action.download')}
        </Button>
      </ButtonGroup>

      {!formData?.destination?.schema && (
        <VStack flex={1}>
          <ErrorMessage message={t('combiner.error.noSchemaLoadedYet')} status={'info'} />
        </VStack>
      )}

      {schema?.schema && (
        <VStack w="100%" justifyContent={'center'} alignItems={'stretch'} gap={3}>
          <MappingInstructionList
            schema={schema.schema}
            instructions={formData?.instructions || []}
            onChange={handleInstructionChange}
            display={'flex'}
            flexDirection={'column'}
            gap={4}
          />
        </VStack>
      )}

      <Modal isOpen={isOpen && Boolean(isSchemaEditor)} onClose={onClose}>
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>
            {isSchemaEditor === EDITOR_MODE.INFERRER && t('combiner.schema.schemaManager.action.infer')}
            {isSchemaEditor === EDITOR_MODE.UPLOADER && t('combiner.schema.schemaManager.action.upload')}
          </ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            {isSchemaEditor === EDITOR_MODE.INFERRER && <Text>{t('combiner.schema.schemaManager.infer.message')}</Text>}
            {isSchemaEditor === EDITOR_MODE.UPLOADER && <SchemaUploader onUpload={handleSchemaUpload} />}
          </ModalBody>

          <ModalFooter>
            <Button colorScheme="blue" mr={3} onClick={onClose}>
              Close
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </>
  )
}
