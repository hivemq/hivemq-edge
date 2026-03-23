import type { FC } from 'react'
import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'
import {
  Button,
  ButtonGroup,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  FormControl,
  FormErrorMessage,
  FormLabel,
  Input,
  NumberDecrementStepper,
  NumberIncrementStepper,
  NumberInput,
  NumberInputField,
  NumberInputStepper,
} from '@chakra-ui/react'

import type { TopicBufferSubscription } from '@/api/__generated__'
import { useCreateTopicBufferSubscription } from '@/api/hooks/useTopicBuffers/index.ts'
import { useUpdateTopicBufferSubscription } from '@/api/hooks/useTopicBuffers/index.ts'

interface TopicBufferEditorDrawerProps {
  isOpen: boolean
  onClose: () => void
  item?: TopicBufferSubscription
}

const TopicBufferEditorDrawer: FC<TopicBufferEditorDrawerProps> = ({ isOpen, onClose, item }) => {
  const { t } = useTranslation()
  const isNew = !item
  const { mutate: create, isPending: isCreating } = useCreateTopicBufferSubscription()
  const { mutate: update, isPending: isUpdating } = useUpdateTopicBufferSubscription()

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isDirty },
  } = useForm<TopicBufferSubscription>({
    mode: 'all',
    defaultValues: item ?? { topicFilter: '', maxMessages: 100 },
  })

  useEffect(() => {
    reset(item ?? { topicFilter: '', maxMessages: 100 })
  }, [item, reset])

  const onSubmit = (data: TopicBufferSubscription) => {
    if (isNew) {
      create({ requestBody: data }, { onSuccess: onClose })
    } else {
      update({ topicFilter: item.topicFilter, requestBody: data }, { onSuccess: onClose })
    }
  }

  return (
    <Drawer variant="hivemq" size="md" isOpen={isOpen} placement="right" onClose={onClose}>
      <DrawerContent aria-label={isNew ? t('topicBuffer.editor.titleAdd') : t('topicBuffer.editor.titleEdit')}>
        <DrawerCloseButton />
        <DrawerHeader>
          {isNew ? t('topicBuffer.editor.titleAdd') : t('topicBuffer.editor.titleEdit')}
        </DrawerHeader>
        <DrawerBody>
          <form id="topic-buffer-form" onSubmit={handleSubmit(onSubmit)}>
            <FormControl isInvalid={!!errors.topicFilter} mb={4}>
              <FormLabel htmlFor="topicFilter">{t('topicBuffer.editor.field.topicFilter')}</FormLabel>
              <Input
                id="topicFilter"
                isReadOnly={!isNew}
                data-testid="topic-buffer-topicFilter"
                {...register('topicFilter', { required: t('topicBuffer.editor.validation.topicFilterRequired') })}
              />
              {errors.topicFilter && <FormErrorMessage>{errors.topicFilter.message}</FormErrorMessage>}
            </FormControl>

            <FormControl isInvalid={!!errors.maxMessages} mb={4}>
              <FormLabel htmlFor="maxMessages">{t('topicBuffer.editor.field.maxMessages')}</FormLabel>
              <NumberInput
                id="maxMessages"
                min={1}
                data-testid="topic-buffer-maxMessages"
                onChange={(_, valueAsNumber) => setValue('maxMessages', valueAsNumber, { shouldDirty: true })}
                defaultValue={item?.maxMessages ?? 100}
              >
                <NumberInputField
                  {...register('maxMessages', {
                    required: t('topicBuffer.editor.validation.maxMessagesRequired'),
                    min: { value: 1, message: t('topicBuffer.editor.validation.maxMessagesMin') },
                  })}
                />
                <NumberInputStepper>
                  <NumberIncrementStepper />
                  <NumberDecrementStepper />
                </NumberInputStepper>
              </NumberInput>
              {errors.maxMessages && <FormErrorMessage>{errors.maxMessages.message}</FormErrorMessage>}
            </FormControl>
          </form>
        </DrawerBody>
        <DrawerFooter>
          <ButtonGroup>
            <Button variant="outline" onClick={onClose}>
              {t('action.cancel')}
            </Button>
            <Button
              type="submit"
              form="topic-buffer-form"
              isDisabled={!isDirty}
              isLoading={isCreating || isUpdating}
              data-testid="topic-buffer-submit"
            >
              {t('action.save')}
            </Button>
          </ButtonGroup>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default TopicBufferEditorDrawer
