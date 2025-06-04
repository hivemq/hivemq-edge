import type { FC } from 'react'
import type { Node } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { Controller, useForm } from 'react-hook-form'
import { Button, Card, CardBody, CardFooter, CardHeader, FormControl, FormLabel, Input } from '@chakra-ui/react'

import type { Group } from '@/modules/Workspace/types.ts'
import { ColorPicker } from '@/components/Chakra/ColorPicker.tsx'

interface GroupMetadataEditorProps {
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
    <Card size="sm">
      <CardHeader data-testid="group-metadata-header">{t('workspace.grouping.editor.title')}</CardHeader>
      <CardBody>
        <form
          id="group-form"
          onSubmit={handleSubmit(handleFormSubmit)}
          style={{ display: 'flex', flexDirection: 'row', gap: '18px' }}
        >
          <FormControl>
            <FormLabel htmlFor="group-title">{t('workspace.grouping.editor.input-title')}</FormLabel>
            <Input id="group-title" {...register('title')} />
          </FormControl>

          <FormControl as="fieldset">
            <FormLabel as="legend">{t('workspace.grouping.editor.input-color')}</FormLabel>
            <Controller
              name="colorScheme"
              render={({ field }) => {
                const { value, onChange, ...rest } = field
                return <ColorPicker colorScheme={value} onChange={(value) => onChange(value)} {...rest} />
              }}
              control={control}
            />
          </FormControl>
        </form>
      </CardBody>
      <CardFooter justifyContent="flex-end">
        <Button isDisabled={!formState.isDirty} type="submit" form="group-form" data-testid="form-submit">
          {t('workspace.grouping.editor.save')}
        </Button>
      </CardFooter>
    </Card>
  )
}

export default GroupMetadataEditor
