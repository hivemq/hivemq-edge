import { FC, useEffect, useState } from 'react'
import { FieldProps } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'

import ListMappings from '@/components/rjsf/MqttTransformation/components/ListMappings.tsx'
import MappingDrawer from '@/components/rjsf/MqttTransformation/components/MappingDrawer.tsx'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { OutwardMapping } from '@/modules/Mappings/types.ts'

export const MqttTransformationField: FC<FieldProps<OutwardMapping[], RJSFSchema, AdapterContext>> = (props) => {
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

  if (!subsData) return null

  return (
    <>
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
