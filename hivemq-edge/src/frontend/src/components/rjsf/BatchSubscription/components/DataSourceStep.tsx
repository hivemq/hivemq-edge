import { FC, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDropzone } from 'react-dropzone'
import * as XLSX from 'xlsx'
import { Button, Text, useToast, VStack } from '@chakra-ui/react'

import { acceptMimeTypes, readFileAsync } from '@/components/rjsf/BatchSubscription/utils/dropzone.utils.ts'
import { StepProps, ToastStatus, WorksheetData } from '@/components/rjsf/BatchSubscription/types.ts'
import { DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils.ts'

const getDropZoneBorder = (color: string) => {
  return {
    bgGradient: `repeating-linear(0deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(90deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(180deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px), repeating-linear-gradient(270deg, ${color}, ${color} 10px, transparent 10px, transparent 20px, ${color} 20px)`,
    backgroundSize: '2px 100%, 100% 2px, 2px 100% , 100% 2px',
    backgroundPosition: '0 0, 0 0, 100% 0, 0 100%',
    backgroundRepeat: 'no-repeat',
    borderRadius: '4px',
  }
}

const DataSourceStep: FC<StepProps> = ({ onContinue }) => {
  const { t } = useTranslation('components')
  const toast = useToast()
  const [loading, setLoading] = useState(false)
  const { getRootProps, getInputProps, isDragActive, open } = useDropzone({
    noClick: true,
    noKeyboard: true,
    maxFiles: 1,
    accept: acceptMimeTypes,
    onDropRejected: (fileRejections) => {
      const status: ToastStatus = 'error'
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
        const worksheet = workbook.Sheets[workbook.SheetNames[0]] // get the first worksheet
        const jsonData = XLSX.utils.sheet_to_json<WorksheetData>(worksheet) // generate objects

        console.log('xxxxxx', jsonData)
        const status: ToastStatus = 'success'
        toast({
          ...DEFAULT_TOAST_OPTION,
          status,
          title: t('rjsf.batchUpload.dropZone.status', { context: status, fileName: file.name }),
        })
        onContinue({ worksheet: jsonData })
      } catch (error) {
        let message
        if (error instanceof Error) message = error.message
        else message = String(error)
        const status: ToastStatus = 'error'
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
      {isDragActive ? (
        <Text>{t('rjsf.batchUpload.dropZone.dropping')}</Text>
      ) : loading ? (
        <Text>{t('rjsf.batchUpload.dropZone.loading')}</Text>
      ) : (
        <>
          <Text>{t('rjsf.batchUpload.dropZone.placeholder')}</Text>
          <Button onClick={open}>{t('rjsf.batchUpload.dropZone.selectFile')}</Button>
        </>
      )}
    </VStack>
  )
}

export default DataSourceStep
