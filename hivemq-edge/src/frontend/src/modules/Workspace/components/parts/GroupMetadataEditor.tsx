import { FC } from 'react'
import { Node } from 'reactflow'
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Button,
  Card,
  FormControl,
  FormLabel,
  Input,
  VStack,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { Controller, useForm } from 'react-hook-form'
import { Group } from '@/modules/Workspace/types.ts'
import { ColorPicker } from '@/components/Chakra/ColorPicker.tsx'

interface GroupMetadataEditorProps {
  id?: string
  group: Node<Group>
  onSubmit: (data: Group) => void
}

const GroupMetadataEditor: FC<GroupMetadataEditorProps> = ({ group, onSubmit }) => {
  const { t } = useTranslation()
  const { register, control, reset, formState, handleSubmit } = useForm<Group>({
    mode: 'all',
    criteriaMode: 'all',
    defaultValues: group.data,
  })

  const handleFormSubmit = (data: Group) => {
    onSubmit(data)
    reset(data)
  }

  return (
    <Card size={'sm'}>
      <Accordion allowToggle>
        <AccordionItem>
          <AccordionButton data-testid="metrics-toggle">
            <Box as="span" flex="1" textAlign="left">
              {t('workspace.grouping.editor.title')}
            </Box>
            <AccordionIcon />
          </AccordionButton>

          <AccordionPanel pb={4}>
            <VStack gap={2} alignItems={'stretch'}>
              <form
                id="group-form"
                onSubmit={handleSubmit(handleFormSubmit)}
                style={{ display: 'flex', flexDirection: 'row', gap: '18px' }}
              >
                <FormControl>
                  <FormLabel htmlFor={'group-title'}>{t('workspace.grouping.editor.input-title')}</FormLabel>
                  <Input id={'group-title'} {...register('title')} />
                </FormControl>

                <FormControl as="fieldset">
                  <FormLabel as="legend">{t('workspace.grouping.editor.input-color')}</FormLabel>
                  <Controller
                    name={`colorScheme`}
                    render={({ field }) => {
                      const { value, onChange, ...rest } = field
                      return <ColorPicker colorScheme={value} onChange={(value) => onChange(value)} {...rest} />
                    }}
                    control={control}
                  />
                </FormControl>
              </form>
              <VStack alignItems={'end'}>
                <Button
                  isDisabled={!formState.isDirty}
                  type={'submit'}
                  form="group-form"
                  data-testid={'form-submit'}
                  mt={8}
                >
                  {t('workspace.grouping.editor.save')}
                </Button>
              </VStack>
            </VStack>
          </AccordionPanel>
        </AccordionItem>
      </Accordion>
    </Card>
  )
}

export default GroupMetadataEditor
