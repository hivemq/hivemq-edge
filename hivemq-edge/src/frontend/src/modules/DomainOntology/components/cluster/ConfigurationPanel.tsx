import { FC, useMemo } from 'react'
import {
  Button,
  Popover,
  PopoverArrow,
  PopoverBody,
  PopoverCloseButton,
  PopoverContent,
  PopoverHeader,
  PopoverTrigger,
} from '@chakra-ui/react'
import Form from '@rjsf/chakra-ui'
import validator from '@rjsf/validator-ajv8'
import { schema, uiSchema } from '@/modules/DomainOntology/components/cluster/form-cluster.utils.ts'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'

interface ConfigurationPanelProps {
  groupKeys: string[]
  onSubmit: (data: string[]) => void
}

const ConfigurationPanel: FC<ConfigurationPanelProps> = ({ groupKeys, onSubmit }) => {
  const data = useMemo(() => {
    const data = { groups: [...groupKeys] }
    console.log('groupKeys', data)
    return data
  }, [groupKeys])

  return (
    <Popover placement="bottom-start" closeOnBlur={false}>
      {({ onClose }) => {
        return (
          <>
            <PopoverTrigger>
              <Button>Configuration</Button>
            </PopoverTrigger>
            <PopoverContent minWidth="sm">
              <PopoverArrow />
              <PopoverCloseButton />
              <PopoverHeader>Configuration</PopoverHeader>
              <PopoverBody>
                <Form
                  schema={schema}
                  uiSchema={uiSchema}
                  formData={data}
                  validator={validator}
                  onChange={(e) => console.log('onChange', e)}
                  onSubmit={(e) => {
                    console.log('onSubmit', e.formData.groups)
                    onSubmit(e.formData.groups)
                    onClose()
                  }}
                  templates={{
                    ArrayFieldTemplate,
                  }}
                />
              </PopoverBody>
            </PopoverContent>
          </>
        )
      }}
    </Popover>
  )
}

export default ConfigurationPanel
