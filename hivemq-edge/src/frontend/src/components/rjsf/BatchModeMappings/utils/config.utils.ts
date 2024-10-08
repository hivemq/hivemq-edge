import { Accept } from 'react-dropzone'

// TODO[NVl] This should be configurable in the uiSchema of each adapter, customising the supported formats
export const acceptMimeTypes: Accept = {
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
  'application/vnd.ms-excel': ['.xls'],
  'text/csv': ['.csv'],
}

export const AUTO_MATCH_DISTANCE = 6
