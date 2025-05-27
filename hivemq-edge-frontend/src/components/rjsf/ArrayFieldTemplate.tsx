import type { FC } from 'react'
import type { ArrayFieldTemplateProps, ArrayFieldTemplateItemType } from '@rjsf/utils'
import { getTemplate, getUiOptions } from '@rjsf/utils'
import type { RJSFSchema } from '@rjsf/utils'
import { Box, Grid, GridItem, HStack } from '@chakra-ui/react'

import AddButton from '@/components/rjsf/__internals/AddButton.tsx'
import BatchUploadButton from '@/components/rjsf/BatchModeMappings/BatchUploadButton.tsx'
import type { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

export const ArrayFieldTemplate: FC<ArrayFieldTemplateProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const {
    canAdd,
    disabled,
    idSchema,
    uiSchema,
    items,
    onAddClick,
    readonly,
    registry,
    required,
    schema,
    title,
    formContext,
  } = props
  const uiOptions = getUiOptions(uiSchema)
  const ArrayFieldDescriptionTemplate = getTemplate<'ArrayFieldDescriptionTemplate'>(
    'ArrayFieldDescriptionTemplate',
    registry,
    uiOptions
  )
  const ArrayFieldItemTemplate = getTemplate<'ArrayFieldItemTemplate'>('ArrayFieldItemTemplate', registry, uiOptions)
  const ArrayFieldTitleTemplate = getTemplate<'ArrayFieldTitleTemplate'>('ArrayFieldTitleTemplate', registry, uiOptions)

  const { onBatchUpload, isEditAdapter } = formContext || {}
  const addButtonTitle = uiSchema?.items?.['ui:addButton'] as string | undefined

  return (
    <Box>
      <ArrayFieldTitleTemplate
        idSchema={idSchema}
        title={uiOptions.title || title}
        schema={schema}
        uiSchema={uiSchema}
        required={required}
        registry={registry}
      />
      <ArrayFieldDescriptionTemplate
        idSchema={idSchema}
        description={uiOptions.description || schema.description}
        schema={schema}
        uiSchema={uiSchema}
        registry={registry}
      />
      <>
        <Grid
          key={`array-item-list-${idSchema.$id}`}
          // TODO[NVL] This is NOT a good approach to add a "role"; submit a PR!
          role="list"
        >
          <GridItem>
            {items.length > 0 &&
              items.map(({ key, ...itemProps }: ArrayFieldTemplateItemType) => (
                <ArrayFieldItemTemplate key={key} {...itemProps} />
              ))}
          </GridItem>
        </Grid>
        {canAdd && (
          <HStack justifyContent="space-between" mt={2}>
            <AddButton
              data-testid="array-item-add"
              className="array-item-add"
              onClick={onAddClick}
              disabled={disabled || readonly}
              uiSchema={uiSchema}
              registry={registry}
              aria-label={addButtonTitle}
            />
            {uiOptions.batchMode && onBatchUpload && isEditAdapter && (
              <BatchUploadButton idSchema={idSchema} schema={schema} onBatchUpload={onBatchUpload} />
            )}
          </HStack>
        )}
      </>
    </Box>
  )
}
