import { FC, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Controller, SubmitHandler, useForm } from 'react-hook-form'

import {
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
  FormLabel,
  HStack,
  Image,
  Radio,
  RadioGroup,
  Text,
  useDisclosure,
  VStack,
} from '@chakra-ui/react'
import { ExportFormat, ExportFormatDisplay, ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'
import useGetAdapterInfo from '@/modules/ProtocolAdapters/hooks/useGetAdapterInfo.ts'
import { adapterExportFormats } from '@/modules/ProtocolAdapters/utils/export.utils.ts'

interface SelectedExportFormat {
  format: ExportFormat.Type
}

const ExportDrawer: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { adapterId } = useParams()
  const navigate = useNavigate()
  const { name, logo } = useGetAdapterInfo(adapterId)
  const form = useForm<SelectedExportFormat>({
    mode: 'all',
    criteriaMode: 'all',
    defaultValues: { format: ExportFormat.Type.SUBSCRIPTIONS },
  })

  useEffect(() => {
    if (adapterId) {
      onOpen()
    }
  }, [adapterId, onOpen])

  const handleInstanceClose = () => {
    onClose()
    navigate('/protocol-adapters', { state: { protocolAdapterTabIndex: ProtocolAdapterTabIndex.PROTOCOLS } })
  }

  const handleEditorOnSubmit: SubmitHandler<SelectedExportFormat> = (data) => {
    console.log('XXXXX', data)
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
        <DrawerHeader id="adapter-discovery-header" borderBottomWidth="1px">
          <Text>{t('protocolAdapter.export.header')}</Text>
          <HStack>
            <Image boxSize="30px" objectFit="scale-down" src={logo} aria-label={name} />
            <Text fontSize="md" fontWeight="500">
              {name}
            </Text>
          </HStack>
        </DrawerHeader>
        <DrawerBody>
          <form
            id="adapter-export-form"
            onSubmit={form.handleSubmit(handleEditorOnSubmit)}
            style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}
          >
            <FormControl>
              <FormLabel htmlFor="format" data-testid="format">
                {t('protocolAdapter.export.form.format.label')}
              </FormLabel>
              <Controller
                name="format"
                control={form.control}
                render={({ field: { value, ...rest } }) => (
                  <RadioGroup {...rest} value={value.toString()} id="format" data-testid="format.options">
                    <VStack alignItems="flex-start" gap={6}>
                      {listFormats.map((format) => (
                        <Radio key={format.value} value={format.value}>
                          {format.label}
                        </Radio>
                      ))}
                    </VStack>
                  </RadioGroup>
                )}
              />
            </FormControl>
          </form>
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
