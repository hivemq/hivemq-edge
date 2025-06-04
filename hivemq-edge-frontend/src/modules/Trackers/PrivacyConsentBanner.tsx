import type { FC } from 'react'
import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button,
  ButtonGroup,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  Link,
  Text,
  useDisclosure,
} from '@chakra-ui/react'
import { ExternalLinkIcon } from '@chakra-ui/icons'
import { useLocalStorage } from '@uidotdev/usehooks'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.ts'

export interface PrivacySourceGranted {
  heapAnalytics: boolean
  sentry: boolean
}

const PrivacyConsentBanner: FC = () => {
  const { t } = useTranslation('components')
  const { data } = useGetConfiguration()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const cancelRef = useRef<HTMLButtonElement>(null)
  const [privacy, setPrivacy] = useLocalStorage<PrivacySourceGranted | undefined>('edge.privacy', undefined)

  useEffect(() => {
    if (!data?.trackingAllowed) return
    if (!privacy) onOpen()
  }, [data?.trackingAllowed, onOpen, privacy])

  useEffect(() => {
    if (!window.heap) return
    if (privacy?.sentry && window.heap.load) {
      window.heap.load(import.meta.env.VITE_MONITORING_HEAP)
      window.heap.addUserProperties({ hivemqId: data?.hivemqId, version: data?.environment?.properties?.version })
    }
  }, [data?.environment?.properties?.version, data?.hivemqId, privacy?.sentry])

  const handleOptIn = () => {
    setPrivacy({ heapAnalytics: true, sentry: true })
    onClose()
    window.location.reload()
  }

  const handleOptOut = () => {
    setPrivacy({ heapAnalytics: false, sentry: false })
    onClose()
    window.location.reload()
  }

  const handleIgnore = () => {
    setPrivacy(undefined)
    onClose()
  }

  return (
    <Drawer isOpen={isOpen} placement="bottom" onClose={() => undefined} initialFocusRef={cancelRef}>
      <DrawerOverlay />
      <DrawerContent aria-labelledby="privacy-header">
        <DrawerCloseButton onClick={handleIgnore} />
        <DrawerHeader>{t('PrivacyConsentBanner.header')}</DrawerHeader>

        <DrawerBody>
          <Text>{t('PrivacyConsentBanner.body')}</Text>
        </DrawerBody>

        <DrawerFooter justifyContent="space-between">
          <ButtonGroup>
            <Link href="https://github.com/hivemq/hivemq-edge" isExternal data-testid="privacy-info">
              {t('PrivacyConsentBanner.action.moreInfo')} <ExternalLinkIcon ml={2} />
            </Link>
          </ButtonGroup>
          <ButtonGroup>
            <Button data-testid="privacy-optOut" onClick={handleOptOut}>
              {t('PrivacyConsentBanner.action.optOut')}
            </Button>
            <Button data-testid="privacy-optIn" variant="primary" onClick={handleOptIn} ref={cancelRef}>
              {t('PrivacyConsentBanner.action.optIn')}
            </Button>
          </ButtonGroup>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default PrivacyConsentBanner
