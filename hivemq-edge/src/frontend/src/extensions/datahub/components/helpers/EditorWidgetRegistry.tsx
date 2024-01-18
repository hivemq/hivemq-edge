import { RegistryWidgetsType, WidgetProps } from '@rjsf/utils'
import { Editor } from '@monaco-editor/react'

// eslint-disable-next-line react-refresh/only-export-components
const MyCustomWidget = (lng: string, props: WidgetProps) => {
  return (
    <Editor
      height="40vh"
      // id={"schema-editor"}
      defaultLanguage={lng}
      defaultValue={props.value}
      onChange={(event) => props.onChange(event)}
    />
  )
}

// eslint-disable-next-line react-refresh/only-export-components
const JavascriptEditor = (props: WidgetProps) => MyCustomWidget('javascript', props)
// eslint-disable-next-line react-refresh/only-export-components
const JSONSchemaEditor = (props: WidgetProps) => MyCustomWidget('json', props)

export const datahubRJSFWidgets: RegistryWidgetsType = {
  'application/schema+json': JSONSchemaEditor,
  'text/javascript': JavascriptEditor,
}
