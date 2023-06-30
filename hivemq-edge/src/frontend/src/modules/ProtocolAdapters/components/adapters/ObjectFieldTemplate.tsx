import {
  FormContextType,
  getTemplate,
  getUiOptions,
  ObjectFieldTemplateProps,
  RJSFSchema,
  StrictRJSFSchema,
  titleId,
} from '@rjsf/utils'
import { Card, CardBody, CardHeader, Tab, TabList, TabPanel, TabPanels, Tabs } from '@chakra-ui/react'
import { UIGroup } from '@/modules/ProtocolAdapters/types.ts'

export const ObjectFieldTemplate = <
  T = never,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = never
>(
  props: ObjectFieldTemplateProps<T, S, F>
) => {
  const { registry, properties, title, description, uiSchema, required, schema, idSchema } = props
  const options = getUiOptions<T, S, F>(uiSchema)
  const TitleFieldTemplate = getTemplate<'TitleFieldTemplate', T, S, F>('TitleFieldTemplate', registry, options)

  // @ts-ignore Type will need to be corrected
  const { groups }: { groups: UIGroup[] } = options
  if (!groups) {
    return (
      <Card m={2}>
        <CardHeader  p={2}>
          {title && (
            <TitleFieldTemplate
              id={titleId<T>(idSchema)}
              title={title}
              required={required}
              schema={schema}
              uiSchema={uiSchema}
              registry={registry}
            />
          )}
          {description}
        </CardHeader>
        <CardBody  p={2}>
          {properties.map((prop) => (
            <div className="x0" key={prop.content.key}>
              {prop.content}
            </div>
          ))}
        </CardBody>
      </Card>
    )
  }

  // TODO[NVL] Not efficient. Build a cluster
  const allGrouped = groups.map((e) => e.children).flat()

  return (
    <>
      <Tabs variant="enclosed">
        <TabList>
          {groups.map((e) => {
            const filteredProps = properties.filter((p) => e.children.includes(p.name))
            if (!filteredProps.length) return null
            return <Tab key={e.id}>{e.title}</Tab>
          })}
        </TabList>

        <TabPanels>
          {groups.map((e) => {
            const filteredProps = properties.filter((p) => e.children.includes(p.name))
            if (!filteredProps.length) return null
            return (
              <TabPanel key={e.id} p={0} pt={'1px'} mb={6}>
                <Card variant={'outline'} borderTopStyle={'none'} p={2}>
                  <CardBody p={2}>
                    {filteredProps.map((prop) => (
                      <div key={prop.content.key}>{prop.content}</div>
                    ))}
                  </CardBody>
                </Card>
              </TabPanel>
            )
          })}
        </TabPanels>
        {properties
          .filter((e) => !allGrouped.includes(e.name))
          .map((prop) => (
            <div key={prop.content.key}>{prop.content}</div>
          ))}
      </Tabs>
    </>
  )
}
