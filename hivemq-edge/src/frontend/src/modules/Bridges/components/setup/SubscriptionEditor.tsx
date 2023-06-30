import { FC, FormEvent, MutableRefObject, useRef } from 'react'
import {
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalHeader,
  ModalOverlay,
  Radio,
  RadioGroup,
  Stack,
  Switch,
} from '@chakra-ui/react'
import { Button, FormControl, FormLabel, ModalFooter } from '@chakra-ui/react'
import TopicAutoComplete from './TopicAutoComplete.tsx'
import { useTranslation } from 'react-i18next'
import { BridgeSubscription } from '@/api/__generated__'

interface SubscriptionEditorProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (sub: BridgeSubscription) => void
  finalFocusRef?: MutableRefObject<null>
}

const SubscriptionEditor: FC<SubscriptionEditorProps> = ({ isOpen, onClose, onSubmit, finalFocusRef }) => {
  const { t } = useTranslation()
  const initialRef = useRef(null)

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const formData = new FormData(event.currentTarget)
    const maxQoS = formData.get('maxQoS') as string
    const destination = formData.get('destination') as string
    const filters = formData.get('filters') as string
    const excludes = formData.get('excludes') as string
    const preserveRetain = formData.get('preserveRetain') as string

    onSubmit({
      maxQoS: Number(maxQoS),
      destination: destination,
      filters: filters.split(','),
      ...(excludes && { excludes: excludes.split(',') }),
      preserveRetain: preserveRetain !== null,
    })
  }
  return (
    <Modal
      closeOnOverlayClick={false}
      initialFocusRef={initialRef}
      finalFocusRef={finalFocusRef}
      isOpen={isOpen}
      onClose={onClose}
    >
      <ModalOverlay bg="blackAlpha.300" backdropFilter="blur(10px) hue-rotate(90deg)" />
      <ModalContent>
        <form onSubmit={handleSubmit}>
          <ModalHeader>{t('modals.SubscriptionEditor.header')}</ModalHeader>
          <ModalCloseButton />
          <ModalBody pb={6} display={'flex'} flexDirection={'column'} gap={4}>
            <TopicAutoComplete
              isRequired
              name={'destination'}
              initialRef={initialRef}
              maxCount={1}
              label={t('bridge.subscription.destination.label')}
            />

            <TopicAutoComplete isRequired name={'filters'} label={t('bridge.subscription.filters.label')} />

            <TopicAutoComplete name={'excludes'} label={t('bridge.subscription.excludes')} />

            <FormControl>
              <FormLabel>{t('bridge.subscription.maxQoS.helper')}</FormLabel>
              <RadioGroup name="maxQoS">
                <Stack spacing={4} direction="row">
                  <Radio value="0">At most once</Radio>
                  <Radio value="1">At least once</Radio>
                  <Radio value="2">Exactly once</Radio>
                </Stack>
              </RadioGroup>
            </FormControl>

            <FormControl display="flex" alignItems="center">
              <FormLabel htmlFor="preserveRetain" mb="0">
                {t('bridge.subscription.preserveRetain.label')}
              </FormLabel>
              <Switch id="preserveRetain" name={'preserveRetain'} value={1} />
            </FormControl>
          </ModalBody>

          <ModalFooter>
            <Button mr={3} onClick={onClose}>
              {t('action.cancel')}
            </Button>
            <Button colorScheme="green" type={'submit'}>
              {t('modals.SubscriptionEditor.submit')}
            </Button>
          </ModalFooter>
        </form>
      </ModalContent>
    </Modal>
  )
}

export default SubscriptionEditor
