import type { FC } from 'react'
import type { JSONSchema7 } from 'json-schema'
import type { RJSFSchema, ArrayFieldTemplateProps, ArrayFieldTemplateItemType } from '@rjsf/utils'
import { getTemplate, getUiOptions } from '@rjsf/utils'
import { Box, HStack, Table, Tbody, Th, Thead, Tr } from '@chakra-ui/react'

import AddButton from '@/components/rjsf/__internals/AddButton.tsx'
import type { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

export const CompactArrayFieldTemplate: FC<ArrayFieldTemplateProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const { canAdd, disabled, idSchema, uiSchema, items, onAddClick, readonly, registry, required, schema, title } = props
  const uiOptions = getUiOptions(uiSchema)
  const ArrayFieldDescriptionTemplate = getTemplate<'ArrayFieldDescriptionTemplate'>(
    'ArrayFieldDescriptionTemplate',
    registry,
    uiOptions
  )
  const ArrayFieldItemTemplate = getTemplate<'ArrayFieldItemTemplate'>('ArrayFieldItemTemplate', registry, uiOptions)
  const ArrayFieldTitleTemplate = getTemplate<'ArrayFieldTitleTemplate'>('ArrayFieldTitleTemplate', registry, uiOptions)

  const { items: SchemaItems } = schema as JSONSchema7
  const { properties } = SchemaItems as JSONSchema7

  // Better approach to unidentified headers?
  if (!properties) return null

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
        <Table size="xs">
          <Thead>
            <Tr>
              {Object.entries(properties).map(([key, values]) => {
                const { title } = values as JSONSchema7
                return <Th key={key}>{title}</Th>
              })}
              <Th>Actions</Th>
            </Tr>
          </Thead>
          <Tbody>
            {items?.length > 0 &&
              items?.map(({ key, ...itemProps }: ArrayFieldTemplateItemType) => {
                return <ArrayFieldItemTemplate key={key} {...itemProps} />
              })}
          </Tbody>
        </Table>
        {canAdd && (
          <HStack justifyContent="end" mt={2}>
            <AddButton
              data-testid="compact-add-item"
              className="array-item-add"
              onClick={onAddClick}
              disabled={disabled || readonly}
              uiSchema={uiSchema}
              registry={registry}
            />
          </HStack>
        )}
      </>
    </Box>
  )
}
