import type { FC } from 'react'
import { useEffect } from 'react'
import {
  Button,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  useDisclosure,
} from '@chakra-ui/react'
import type { SubmitHandler } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import type { ApiError, ISA95ApiBean } from '@/api/__generated__'
import { useGetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useGetUnifiedNamespace.ts'
import { useSetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useSetUnifiedNamespace.ts'

import NamespaceForm from '@/modules/UnifiedNamespace/components/NamespaceForm.tsx'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

interface UnifiedNamespaceEditorProps {
  id?: string
}

const UnifiedNamespaceEditor: FC<UnifiedNamespaceEditorProps> = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { successToast, errorToast } = useEdgeToast()

  const { data } = useGetUnifiedNamespace()
  const { isPending: isUploading, mutateAsync } = useSetUnifiedNamespace()

  useEffect(() => {
    onOpen()
  }, [onOpen])

  const handleEditorOnClose = () => {
    onClose()
    navigate('/namespace')
  }

  const handleOnSubmit: SubmitHandler<ISA95ApiBean> = (data) => {
    mutateAsync({ requestBody: data })
      .then(() => {
        successToast({
          title: t('unifiedNamespace.toast.update.title'),
          description: t('unifiedNamespace.toast.update.description'),
        })
      })
      .catch((err: ApiError) =>
        errorToast(
          {
            title: t('unifiedNamespace.toast.update.title'),
            description: t('unifiedNamespace.toast.update.error'),
          },
          err
        )
      )

    onClose()
    navigate('/namespace')
  }
  if (!data) return null

  return (
    <Drawer closeOnOverlayClick={false} size="lg" isOpen={isOpen} placement="right" onClose={handleEditorOnClose}>
      <DrawerOverlay />
      <DrawerContent aria-label={t('bridge.drawer.label')}>
        <DrawerCloseButton />
        <DrawerHeader id="bridge-form-header" borderBottomWidth="1px">
          {t('unifiedNamespace.title')}
        </DrawerHeader>

        <DrawerBody>
          <NamespaceForm defaultValues={data} onSubmit={handleOnSubmit} />
        </DrawerBody>
        <DrawerFooter>
          <Button variant="primary" isLoading={isUploading} type="submit" form="namespace-form">
            {t('unifiedNamespace.submit.label')}
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default UnifiedNamespaceEditor
