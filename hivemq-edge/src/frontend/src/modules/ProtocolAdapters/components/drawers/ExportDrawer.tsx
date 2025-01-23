import type { FC } from 'react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { SubmitHandler } from 'react-hook-form'
import { Controller, useForm } from 'react-hook-form'
import { Select } from 'chakra-react-select'

import type { AlertStatus } from '@chakra-ui/react'
import {
  chakra as Chakra,
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Flex,
  FormControl,
  FormHelperText,
  FormLabel,
  HStack,
  Image,
  Radio,
  RadioGroup,
  Text,
  useDisclosure,
  VStack,
  useToast,
} from '@chakra-ui/react'
import type { ExportFormatDisplay } from '@/modules/ProtocolAdapters/types.ts'
import { AdapterExportError, ExportFormat, ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'
import useGetAdapterInfo from '@/modules/ProtocolAdapters/hooks/useGetAdapterInfo.ts'
import { adapterExportFormats } from '@/modules/ProtocolAdapters/utils/export.utils.ts'
import { DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils.ts'

interface SelectedExportFormat {
  content: ExportFormat.Type
  format: string
}

interface MIMETypeOptions {
  value: string
  label: string
  description: string
}

const ExportDrawer: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const toast = useToast()
  const { adapterId } = useParams()
  const navigate = useNavigate()
  const { protocol, adapter } = useGetAdapterInfo(adapterId)
  const { name, logoUrl } = protocol || {}
  const [formatOptions, setFormatOptions] = useState<MIMETypeOptions[]>([])
  const form = useForm<SelectedExportFormat>({
    mode: 'all',
    criteriaMode: 'all',
    defaultValues: { content: ExportFormat.Type.CONFIGURATION },
  })
  const watchFormatChange = form.watch('content')

  useEffect(() => {
    if (adapterId) {
      onOpen()
    }
  }, [adapterId, onOpen])

  useEffect(() => {
    const format = adapterExportFormats.find((exportFormat) => exportFormat.value === watchFormatChange)
    if (format) {
      const mimeOptions =
        format.formats?.map<MIMETypeOptions>((format) => ({ label: format, value: format, description: '' })) || []
      setFormatOptions(mimeOptions)
      if (mimeOptions.length) form.setValue('format', mimeOptions[0].value)
    }
  }, [form, watchFormatChange])

  const handleInstanceClose = () => {
    onClose()
    navigate('/protocol-adapters', { state: { protocolAdapterTabIndex: ProtocolAdapterTabIndex.PROTOCOLS } })
  }

  const handleEditorOnSubmit: SubmitHandler<SelectedExportFormat> = (data) => {
    const downloader = adapterExportFormats.find((exportFormat) => exportFormat.value === data.content)
    if (downloader && adapter && protocol) {
      try {
        downloader.downloader?.(adapter.id, data.format, adapter, protocol)
        const status: AlertStatus = 'success'
        toast({
          ...DEFAULT_TOAST_OPTION,
          status,
          title: t('protocolAdapter.export.download.status', { context: status, name: adapter.id }),
        })
      } catch (error) {
        let message
        if (error instanceof AdapterExportError) message = t(error.message)
        else if (error instanceof Error) message = error.message
        else message = String(error)
        const status: AlertStatus = 'error'
        toast({
          ...DEFAULT_TOAST_OPTION,
          status,
          title: t('protocolAdapter.export.download.status', { context: status, name: adapter.id }),
          description: message,
        })
      }
    }
    // handleInstanceClose()
  }

  const listFormats = adapterExportFormats.map<ExportFormatDisplay>((exports) => {
    return {
      ...exports,
      label: t('protocolAdapter.export.formats.label', { context: exports.value }),
      description: t('protocolAdapter.export.formats.description', { context: exports.value }),
    }
  })

  return (
    <Drawer
      variant="hivemq"
      closeOnOverlayClick={false}
      size="md"
      isOpen={isOpen}
      placement="right"
      onClose={handleInstanceClose}
    >
      <DrawerOverlay />
      <DrawerContent aria-label={t('protocolAdapter.export.header')}>
        <DrawerCloseButton />
        <DrawerHeader borderBottomWidth="1px">
          <Text data-testid="adapter-export-title">{t('protocolAdapter.export.header')}</Text>
          <HStack>
            <Image boxSize="30px" objectFit="scale-down" src={logoUrl} aria-label={name} />
            <Text data-testid="adapter-export-type" fontSize="md" fontWeight="500">
              {name}
            </Text>
          </HStack>
        </DrawerHeader>
        <DrawerBody>
          <Chakra.form
            id="adapter-export-form"
            onSubmit={form.handleSubmit(handleEditorOnSubmit)}
            style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}
          >
            <FormControl variant="hivemq">
              <FormLabel as="legend" htmlFor="field-content" data-testid="field-content-label">
                {t('protocolAdapter.export.form.content.label')}
              </FormLabel>
              <Controller
                name="content"
                control={form.control}
                render={({ field: { value, ...rest } }) => (
                  <RadioGroup {...rest} value={value.toString()} id="field-content" data-testid="field-content-options">
                    <VStack alignItems="flex-start" gap={6}>
                      {listFormats.map((format) => {
                        const isDisabled = format.isDisabled?.(protocol)
                        return (
                          <FormControl as="div" key={format.value} w="-webkit-fill-available" isDisabled={isDisabled}>
                            <Radio value={format.value}>
                              <Text>{format.label} </Text>
                              <FormHelperText as="p">{format.description}</FormHelperText>
                              {isDisabled && (
                                <FormHelperText as="p">
                                  {t('protocolAdapter.export.formats.notAvailable')}
                                </FormHelperText>
                              )}
                            </Radio>
                          </FormControl>
                        )
                      })}
                    </VStack>
                  </RadioGroup>
                )}
              />
            </FormControl>

            <FormControl variant="hivemq">
              <FormLabel as="legend" htmlFor="field-format" data-testid="field-format-label">
                {t('protocolAdapter.export.form.format.label')}
              </FormLabel>
              <Controller
                name="format"
                control={form.control}
                rules={{ required: t('protocolAdapter.export.form.format.aria-label') }}
                render={({ field: { value, onChange, ...rest } }) => {
                  return (
                    <Select<MIMETypeOptions>
                      {...rest}
                      id="field-format"
                      isDisabled={formatOptions.length === 1}
                      instanceId="field-format.toto"
                      aria-label={t('protocolAdapter.export.form.format.aria-label')}
                      onChange={(e) => onChange(e?.value)}
                      value={{ label: value, value, description: '' }}
                      options={formatOptions}
                    />
                  )
                }}
              ></Controller>
            </FormControl>
          </Chakra.form>
        </DrawerBody>
        <DrawerFooter borderTopWidth="1px">
          <Flex flexGrow={1} justifyContent="flex-end">
            <Button variant="primary" isDisabled={!form.formState.isValid} type="submit" form="adapter-export-form">
              {t('protocolAdapter.export.action.export')}
            </Button>
          </Flex>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default ExportDrawer
