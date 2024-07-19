import { FC, useCallback, useEffect, useMemo, useState } from 'react'
import { FieldProps, getTemplate, getUiOptions } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { JSONSchema7 } from 'json-schema'

import { DataGridProps, CustomPropertyForm, FormDataItem, CustomPropertyValue } from '@/components/rjsf/Fields/types.ts'
import DataGridWidget from '@/components/rjsf/Fields/DataGridWidget.tsx'
import DataTableWidget from '@/components/rjsf/Fields/DataTableWidget.tsx'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

const CompactArrayField: FC<FieldProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const { idSchema, registry, formData, schema, disabled, readonly } = props
  const uiOptions = getUiOptions(props.uiSchema)
  const [rawData, setRawData] = useState<CustomPropertyForm>([])

  useEffect(() => {
    const newData = formData as CustomPropertyForm | undefined
    if (!newData || !newData.length) return

    setRawData(formData as CustomPropertyForm)
  }, [formData])

  useEffect(() => {
    console.log('XXXXXX raw', rawData)
    // props.onChange(rawData)
  }, [rawData])

  const { items } = schema
  const { properties, maxItems, required } = items as JSONSchema7

  const columnTypes = useMemo(() => {
    if (!properties) return []
    return Object.entries(properties) as [string, JSONSchema7][]
  }, [properties])

  const makeNewItem = useCallback(() => {
    return Object.fromEntries(columnTypes.map(([columnId]) => [columnId, ''])) as FormDataItem
  }, [columnTypes])

  const onHandleAdd = () => {
    const newData = makeNewItem()
    setRawData((old) => [...old, newData])
  }

  const onHandleDelete = (index: number) => {
    setRawData((old) => {
      const newData = [...old]
      newData.splice(index, 1)
      return newData
    })
  }

  const onUpdateData = (rowIndex: number, columnId: string, value: CustomPropertyValue) => {
    setRawData((old) => {
      if (old.length === 0) return old

      const newData = [...old]
      newData[rowIndex] = { ...newData[rowIndex], [columnId]: value }
      return newData
    })
  }

  const ArrayFieldTitleTemplate = getTemplate<'ArrayFieldTitleTemplate'>('ArrayFieldTitleTemplate', registry, uiOptions)
  const ArrayFieldDescriptionTemplate = getTemplate<'ArrayFieldDescriptionTemplate'>(
    'ArrayFieldDescriptionTemplate',
    props.registry,
    uiOptions
  )

  // TODO Check for other conditions on the Schema and UISchema
  if (schema.type !== 'array') return <props.registry.fields.ArrayField {...props} />

  const commonProps: DataGridProps = {
    onHandleAddItem: onHandleAdd,
    onHandleDeleteItem: onHandleDelete,
    onUpdateData: onUpdateData,
    data: rawData,
    columnTypes: columnTypes,
    maxItems: maxItems,
    required: required,
  }

  return (
    <>
      <ArrayFieldTitleTemplate
        idSchema={idSchema}
        title={uiOptions.title || props.schema.title}
        schema={props.schema}
        uiSchema={props.uiSchema}
        required={props.required}
        registry={registry}
      />
      <ArrayFieldDescriptionTemplate
        idSchema={idSchema}
        description={uiOptions.description || props.schema.description}
        schema={props.schema}
        uiSchema={props.uiSchema}
        registry={registry}
      />
      <DataTableWidget {...commonProps} isDisabled={disabled || readonly} />
      <DataGridWidget {...commonProps} onSetData={setRawData} />
    </>
  )
}

export default CompactArrayField
