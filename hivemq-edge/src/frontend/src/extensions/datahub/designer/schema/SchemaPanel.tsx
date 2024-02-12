import { FC, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'
import { CustomValidator } from '@rjsf/utils'

import { PanelProps, SchemaData } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_SCHEMA_SCHEMA } from '@datahub/designer/schema/SchemaData.ts'

export const SchemaPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes } = useDataHubDraftStore()
  // const [fields, setFields] = useState<string[] | null>(null)

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<SchemaData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  const customValidate: CustomValidator<SchemaData> = (formData, errors) => {
    if (!formData) return errors

    // TODO[NVL] Consider live validation
    // if (type === SchemaType.JAVASCRIPT && schemaSource) {
    //   try {
    //     const program = Parser.parse(schemaSource, { ecmaVersion: 'latest' })
    //   } catch (e) {
    //     errors.schemaSource?.addError((e as SyntaxError).message)
    //   }
    // }
    // if (type === SchemaType.JSON && schemaSource) {
    //   try {
    //     const validator = customizeValidator()
    //     const parsed: RJSFSchema = JSON.parse(schemaSource)
    //     const validated = validator.validateFormData(undefined, { ...jj, required: [] })
    //   } catch (e) {
    //     errors.schemaSource?.addError((e as SyntaxError).message)
    //     // setFields(null)
    //   }
    // }
    // if (type === SchemaType.PROTO && schemaSource) {
    //   try {
    //     const parsed = parse(schemaSource)
    //   } catch (e) {
    //     errors.schemaSource?.addError((e as SyntaxError).message)
    //     // setFields(null)
    //   }
    // }

    return errors
  }

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_SCHEMA_SCHEMA.schema}
          uiSchema={MOCK_SCHEMA_SCHEMA.uiSchema}
          formData={data}
          widgets={datahubRJSFWidgets}
          customValidate={customValidate}
          onSubmit={onFormSubmit}
        />
      </CardBody>
    </Card>
  )
}
