import { FC, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FieldProps } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { Accordion, AccordionItem, AccordionButton, AccordionPanel, AccordionIcon, Box } from '@chakra-ui/react'

import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { OutwardSubscription } from '@/modules/Subscriptions/types.ts'
import ListSubscriptions from '@/components/rjsf/MqttTransformation/components/ListSubscriptions.tsx'
import SubscriptionContainer from '@/components/rjsf/MqttTransformation/components/SubscriptionContainer.tsx'

export const MqttTransformationField: FC<FieldProps<OutwardSubscription[], RJSFSchema, AdapterContext>> = (props) => {
  const { t } = useTranslation('components')
  const [selectedItem, setSelectedItem] = useState<number | undefined>(undefined)
  const [subsData, setSubsData] = useState<OutwardSubscription[] | undefined>(props.formData)

  const { adapterId, adapterType } = props.formContext || {}

  useEffect(() => {
    // TODO[NVL] Add validation and persistence
    return () => undefined
  }, [])

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
  }

  const handleAdd = () => {
    setSubsData((old) => [
      ...(old || []),
      {
        node: '',
        'mqtt-topic': [],
        mapping: [],
      },
    ])
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleChange = (id: keyof OutwardSubscription, v: any) => {
    if (selectedItem === undefined) return
    setSubsData((old) => {
      const currentItem = old?.[selectedItem]
      if (currentItem) currentItem[id] = v
      return [...(old || [])]
    })
  }

  if (!subsData) return null

  return (
    <Accordion defaultIndex={0} index={selectedItem === undefined ? 0 : 1}>
      <AccordionItem isDisabled={selectedItem !== undefined}>
        <AccordionButton>
          <Box as="span" flex="1" textAlign="left">
            {t('rjsf.MqttTransformationField.tabs.list')}
          </Box>
          <AccordionIcon />
        </AccordionButton>
        <AccordionPanel pb={4}>
          <ListSubscriptions
            items={subsData}
            onEdit={handleEdit}
            onAdd={handleAdd}
            onDelete={handleDelete}
            isDisabled={selectedItem !== undefined}
          />
        </AccordionPanel>
      </AccordionItem>

      <AccordionItem isDisabled={selectedItem === undefined}>
        <AccordionButton>
          <Box as="span" flex="1" textAlign="left">
            {t('rjsf.MqttTransformationField.tabs.editor')}
          </Box>
          <AccordionIcon />
        </AccordionButton>
        <AccordionPanel pb={4}>
          {selectedItem !== undefined && (
            <SubscriptionContainer
              adapterId={adapterId}
              adapterType={adapterType}
              item={subsData[selectedItem]}
              onClose={handleClose}
              onSubmit={handleSubmit}
              onChange={handleChange}
            />
          )}
        </AccordionPanel>
      </AccordionItem>
    </Accordion>
  )
}
