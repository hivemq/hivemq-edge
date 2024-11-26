import { FC, useEffect, useState } from 'react'
import { FieldProps, getTemplate, getUiOptions } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'

import ListMappings from '@/components/rjsf/MqttTransformation/components/ListMappings.tsx'
import MappingDrawer from '@/components/rjsf/MqttTransformation/components/MappingDrawer.tsx'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { OutwardMapping } from '@/modules/Mappings/types.ts'
import { useTranslation } from 'react-i18next'
import ErrorMessage from '@/components/ErrorMessage.tsx'

export const MqttTransformationField: FC<FieldProps<OutwardMapping[], RJSFSchema, AdapterContext>> = (props) => {
  const { t } = useTranslation('components')
  const [selectedItem, setSelectedItem] = useState<number | undefined>(undefined)
  const [subsData, setSubsData] = useState<OutwardMapping[] | undefined>(props.formData)

  const { adapterId, adapterType } = props.formContext || {}

  useEffect(() => {
    props.onChange(subsData)
  }, [props, subsData])

  const handleEdit = (index: number) => {
    setSelectedItem(index)
  }

  const handleDelete = (index: number) => {
    setSubsData((old) => {
      const gg = [...(old || [])]
      gg.splice(index, 1)
      return [...gg]
    })
  }

  const handleClose = () => {
    setSelectedItem(undefined)
  }

  const handleSubmit = () => {
    setSelectedItem(undefined)
    props.onChange(subsData)
  }

  const handleAdd = () => {
    setSubsData((old) => [
      ...(old || []),
      {
        mqttTopicFilter: undefined,
        tag: undefined,
        fieldMapping: [],
      },
    ])
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleChange = (id: keyof OutwardMapping, v: any) => {
    if (selectedItem === undefined) return
    setSubsData((old) => {
      const currentItem = old?.[selectedItem]
      if (currentItem) currentItem[id] = v
      return [...(old || [])]
    })
  }

  const ArrayFieldDescriptionTemplate = getTemplate<'ArrayFieldDescriptionTemplate', OutwardMapping[]>(
    'ArrayFieldDescriptionTemplate',
    props.registry,
    props.uiOptions
  )

  const ArrayFieldTitleTemplate = getTemplate<'ArrayFieldTitleTemplate', OutwardMapping[]>(
    'ArrayFieldTitleTemplate',
    props.registry,
    props.uiOptions
  )
  const uiOptions = getUiOptions(props.uiSchema)

  if (!subsData || !adapterId || !adapterType)
    return <ErrorMessage message={t('rjsf.MqttTransformationField.error.internalError')} status="error" />

  return (
    <>
      <ArrayFieldTitleTemplate
        idSchema={props.idSchema}
        title={uiOptions.title || props.schema.title}
        schema={props.schema}
        uiSchema={props.uiSchema}
        required={props.required}
        registry={props.registry}
      />
      <ArrayFieldDescriptionTemplate
        idSchema={props.idSchema}
        description={uiOptions.description || props.schema.description}
        schema={props.schema}
        uiSchema={props.uiSchema}
        registry={props.registry}
      />

      <ListMappings items={subsData} onEdit={handleEdit} onAdd={handleAdd} onDelete={handleDelete} />
      {selectedItem != undefined && (
        <MappingDrawer
          adapterId={adapterId}
          adapterType={adapterType}
          item={subsData[selectedItem]}
          onClose={handleClose}
          onSubmit={handleSubmit}
          onChange={handleChange}
        />
      )}
    </>
  )
}
