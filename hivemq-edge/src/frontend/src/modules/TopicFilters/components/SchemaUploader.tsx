import { FC, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDropzone } from 'react-dropzone'
import { AlertStatus, Button, Text, useToast, VStack } from '@chakra-ui/react'

import { DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils.ts'
import { getDropZoneBorder } from '@/modules/Theme/utils.ts'
import { ACCEPT_JSON_SCHEMA } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'

interface SchemaUploaderProps {
  onUpload: (s: string) => void
}

const SchemaUploader: FC<SchemaUploaderProps> = ({ onUpload }) => {
  const [loading, setLoading] = useState(false)
  const { t } = useTranslation()
  const toast = useToast()

  const { getRootProps, getInputProps, isDragActive, open } = useDropzone({
    noClick: true,
    noKeyboard: true,
    maxFiles: 1,
    accept: ACCEPT_JSON_SCHEMA,
    onDropRejected: (fileRejections) => {
      const status: AlertStatus = 'error'
      setLoading(false)
      fileRejections.forEach((fileRejection) => {
        toast({
          ...DEFAULT_TOAST_OPTION,
          status,
          title: t('rjsf.batchUpload.dropZone.status', {
            ns: 'components',
            context: status,
            fileName: fileRejection.file.name,
          }),
          description: fileRejection.errors[0].message,
        })
      })
    },
    onDropAccepted: async (files) => {
      const [file] = files
      const reader = new FileReader()
      reader.readAsDataURL(file)
      reader.onload = () => {
        if (typeof reader.result === 'string') onUpload(reader.result as string)
      }
    },
  })

  return (
    <VStack
      {...getRootProps()}
      {...getDropZoneBorder('blue.500')}
      minHeight="calc(250px - 2rem)"
      display="flex"
      justifyContent="center"
      alignItems="center"
      id="dropzone"
    >
      <input {...getInputProps()} data-testid="schema-dropzone" />
      {isDragActive && <Text>{t('rjsf.batchUpload.dropZone.dropping', { ns: 'components' })}</Text>}
      {loading && <Text>{t('rjsf.batchUpload.dropZone.loading', { ns: 'components' })}</Text>}
      {!isDragActive && !loading && (
        <>
          <Text>{t('topicFilter.schema.actions.upload')}</Text>
          <Button onClick={open}>{t('rjsf.batchUpload.dropZone.selectFile', { ns: 'components' })}</Button>
        </>
      )}
    </VStack>
  )
}

export default SchemaUploader
