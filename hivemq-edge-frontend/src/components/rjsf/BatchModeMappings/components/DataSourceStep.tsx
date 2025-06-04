import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDropzone } from 'react-dropzone'
import * as XLSX from 'xlsx'
import type { AlertStatus } from '@chakra-ui/react'
import { Button, Text, useToast, VStack } from '@chakra-ui/react'

import { acceptMimeTypes } from '@/components/rjsf/BatchModeMappings/utils/config.utils.ts'
import { readFileAsync } from '@/components/rjsf/BatchModeMappings/utils/dropzone.utils.ts'
import type { StepRendererProps, WorksheetData } from '@/components/rjsf/BatchModeMappings/types.ts'
import { BASE_TOAST_OPTION, DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils.ts'
import { getDropZoneBorder } from '@/modules/Theme/utils.ts'

const DataSourceStep: FC<StepRendererProps> = ({ onContinue, store }) => {
  const { t } = useTranslation('components')
  const toast = useToast(BASE_TOAST_OPTION)
  const [loading, setLoading] = useState(false)
  const { fileName } = store
  const { getRootProps, getInputProps, isDragActive, open } = useDropzone({
    noClick: true,
    noKeyboard: true,
    maxFiles: 1,
    accept: acceptMimeTypes,
    onDropRejected: (fileRejections) => {
      const status: AlertStatus = 'error'
      setLoading(false)
      fileRejections.forEach((fileRejection) => {
        toast({
          ...DEFAULT_TOAST_OPTION,
          status,
          title: t('rjsf.batchUpload.dropZone.status', { context: status, fileName: fileRejection.file.name }),
          description: fileRejection.errors[0].message,
        })
      })
    },
    onDropAccepted: async (files) => {
      const [file] = files
      setLoading(true)
      try {
        const workbook = XLSX.read(await readFileAsync(file), {
          dense: true,
        })
        const firstWorksheetAsRawData = workbook.Sheets[workbook.SheetNames[0]]
        const firstWorksheetData = XLSX.utils.sheet_to_json<WorksheetData>(firstWorksheetAsRawData)

        const status: AlertStatus = 'success'
        toast({
          ...DEFAULT_TOAST_OPTION,
          status,
          title: t('rjsf.batchUpload.dropZone.status', { context: status, fileName: file.name }),
        })
        onContinue({ worksheet: firstWorksheetData, fileName: file.name })
      } catch (error) {
        let message
        if (error instanceof Error) message = error.message
        else message = String(error)
        const status: AlertStatus = 'error'
        toast({
          ...DEFAULT_TOAST_OPTION,
          status,
          title: t('rjsf.batchUpload.dropZone.status', { context: status, fileName: file.name }),
          description: message,
        })
      }

      setLoading(false)
    },
  })

  return (
    <VStack
      {...getRootProps()}
      {...getDropZoneBorder('blue.500')}
      minHeight="calc(450px - 2rem)"
      display="flex"
      justifyContent="center"
      alignItems="center"
      id="dropzone"
    >
      <input {...getInputProps()} data-testid="batch-load-dropzone" />
      {isDragActive && <Text>{t('rjsf.batchUpload.dropZone.dropping')}</Text>}
      {loading && <Text>{t('rjsf.batchUpload.dropZone.loading')}</Text>}
      {!isDragActive && !loading && (
        <>
          {fileName && <Text mb={4}>{t('rjsf.batchUpload.dropZone.currentlyLoaded', { fileName: fileName })}</Text>}
          <Text>{t('rjsf.batchUpload.dropZone.placeholder')}</Text>
          <Button onClick={open}>{t('rjsf.batchUpload.dropZone.selectFile')}</Button>
        </>
      )}
    </VStack>
  )
}

export default DataSourceStep
