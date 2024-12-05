import { FC, useEffect, useState } from 'react'
import { FieldProps, getTemplate, getUiOptions } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'

import { MOCK_MAX_QOS } from '@/__test-utils__/adapters/mqtt.ts'
import { SouthboundMapping } from '@/api/__generated__'
import ListMappings from '@/components/rjsf/MqttTransformation/components/ListMappings.tsx'
import MappingDrawer from '@/components/rjsf/MqttTransformation/components/MappingDrawer.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

export const MqttTransformationField: FC<FieldProps<SouthboundMapping[], RJSFSchema, AdapterContext>> = (props) => {
  const { t } = useTranslation('components')
  const [selectedItem, setSelectedItem] = useState<number | undefined>(undefined)
  const [subsData, setSubsData] = useState<SouthboundMapping[] | undefined>(props.formData)

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
        topicFilter: undefined,
        tagName: undefined,
        maxQoS: MOCK_MAX_QOS,
        fieldMapping: {
          instructions: [],
          metadata: {
            source: {},
            destination: {},
          },
        },
      },
    ])
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleChange = (id: keyof SouthboundMapping, v: any) => {
    if (selectedItem === undefined) return
    setSubsData((old) => {
      const currentItem = old?.[selectedItem]
      if (currentItem) {
        // @ts-ignore
        currentItem[id] = v
      }
      return [...(old || [])]
    })
  }

  const ArrayFieldDescriptionTemplate = getTemplate<'ArrayFieldDescriptionTemplate', SouthboundMapping[]>(
    'ArrayFieldDescriptionTemplate',
    props.registry,
    props.uiOptions
  )

  const ArrayFieldTitleTemplate = getTemplate<'ArrayFieldTitleTemplate', SouthboundMapping[]>(
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
