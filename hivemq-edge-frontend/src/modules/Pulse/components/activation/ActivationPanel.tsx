import type { FC } from 'react'
import { useState, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Alert,
  AlertTitle,
  AlertDescription,
  Button,
  Drawer,
  DrawerBody,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  useDisclosure,
  VStack,
  useToast,
} from '@chakra-ui/react'

import { Capability } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { useCreateActivationToken } from '@/api/hooks/usePulse/useCreateActivationToken.ts'
import { useDeleteActivationToken } from '@/api/hooks/usePulse/useDeleteActivationToken.ts'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import { BASE_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils.ts'

export const ActivationPanel: FC = () => {
  const { t } = useTranslation()
  const btnRef = useRef<HTMLButtonElement>(null)
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { isOpen: isConfirmDeleteOpen, onOpen: onConfirmDeleteOpen, onClose: onConfirmDeleteClose } = useDisclosure()
  const { data: hasPulseCapability, error, isLoading } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)
  const createToken = useCreateActivationToken()
  const deleteToken = useDeleteActivationToken()
  const [token, setToken] = useState<string | undefined>()
  const [hasErrors, setHasErrors] = useState(false)
  const toast = useToast(BASE_TOAST_OPTION)

  // const extraErrors = useMemo(() => {
  //   return {
  //     // __errors: ['An error occurred while fetching capabilities'],
  //   }
  // }, [])

  const onSubmitToken = (token: string) => {
    const promise = createToken.mutateAsync({ token })
    toast.closeAll()
    toast.promise(
      promise.then(() => onClose()),
      {
        success: {
          ...BASE_TOAST_OPTION,
          title: t('pulse.activation.activate.title'),
          description: t('pulse.activation.activate.success'),
        },
        error: (error) => ({
          ...BASE_TOAST_OPTION,
          title: t('pulse.activation.activate.title'),
          description: t('pulse.activation.activate.error', { error: error.message }),
        }),
        loading: { title: t('pulse.activation.activate.title'), description: t('pulse.activation.activate.loading') },
      }
    )
  }

  const onRevokeToken = () => {
    const promise = deleteToken.mutateAsync()
    toast.closeAll()
    toast.promise(
      promise.then(() => onClose()),
      {
        success: {
          ...BASE_TOAST_OPTION,
          title: t('pulse.activation.revoke.title'),
          description: t('pulse.activation.revoke.success'),
        },
        error: (error) => ({
          ...BASE_TOAST_OPTION,
          title: t('pulse.activation.revoke.title'),
          description: t('pulse.activation.revoke.error', { error: error.message }),
        }),
        loading: {
          title: t('pulse.activation.revoke.title'),
          description: t('pulse.activation.revoke.loading'),
        },
      }
    )
  }

  function handleConfirmOnClose() {
    onConfirmDeleteClose()
  }

  function handleConfirmOnSubmit() {
    onConfirmDeleteClose()
    onRevokeToken()
  }

  if (isLoading) return <LoaderSpinner />
  if (error) return <ErrorMessage type={error?.message} message={t('pulse.activation.error.noCapabilityLoaded')} />

  const title = hasPulseCapability
    ? t('pulse.activation.form.activated.title')
    : t('pulse.activation.form.notActivated.title')
  const description = hasPulseCapability
    ? t('pulse.activation.form.activated.description')
    : t('pulse.activation.form.notActivated.description')

  return (
    <>
      <Button ref={btnRef} onClick={onOpen} data-testid="pulse-activation-trigger">
        {t('pulse.activation.action.activate')}
      </Button>
      <Drawer isOpen={isOpen} size="md" placement="right" onClose={onClose} finalFocusRef={btnRef}>
        <DrawerOverlay />
        <DrawerContent aria-label={t('pulse.activation.heading.title')}>
          <DrawerCloseButton />
          <DrawerHeader>{t('pulse.activation.heading.title')}</DrawerHeader>

          <DrawerBody display="flex" gap={4} flexDirection="column">
            <Alert status="info">
              <VStack align="flex-start">
                <AlertTitle>{title}</AlertTitle>
                <AlertDescription>{description}</AlertDescription>
              </VStack>
            </Alert>

            <ChakraRJSForm
              id="pulse-activation-form"
              schema={{ type: 'string', format: 'jwt' }}
              // formData={token}
              uiSchema={{
                'ui:showErrorList': false,
                'ui:title': t('pulse.activation.form.token.title'),
                'ui:description': t('pulse.activation.form.token.description'),
                'ui:placeholder': t('pulse.activation.form.token.placeholder'),
                'ui:submitButtonOptions': {
                  norender: true,
                },
                'ui:widget': 'textarea',
                'ui:options': {
                  rows: 10,
                },
              }}
              // extraErrors={extraErrors}
              onSubmit={(event) => {
                onSubmitToken(event.formData)
              }}
              onChange={(e) => {
                setToken(e.formData)
                setHasErrors(e.errors.length > 0)
              }}
              onError={(errors) => setHasErrors(errors.length > 0)}
            />
          </DrawerBody>

          <DrawerFooter justifyContent="space-between">
            <Button
              data-testid="pulse-activation-revoke"
              variant="danger"
              onClick={onConfirmDeleteOpen}
              isLoading={deleteToken.isPending}
              isDisabled={!hasPulseCapability}
            >
              {t('pulse.activation.action.revoke')}
            </Button>
            <Button
              data-testid="pulse-activation-submit"
              type="submit"
              form="pulse-activation-form"
              variant="primary"
              isLoading={createToken.isPending}
              isDisabled={!token || hasErrors}
            >
              {t('pulse.activation.action.submit')}
            </Button>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
      <ConfirmationDialog
        isOpen={isConfirmDeleteOpen}
        onClose={handleConfirmOnClose}
        onSubmit={handleConfirmOnSubmit}
        message={t('pulse.activation.revoke.confirmation')}
        header={t('pulse.activation.revoke.title')}
        action={t('pulse.activation.action.revoke')}
      />
    </>
  )
}
