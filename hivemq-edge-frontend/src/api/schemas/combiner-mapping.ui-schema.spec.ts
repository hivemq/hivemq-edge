import { expect } from 'vitest'
import { combinerMappingUiSchema } from './combiner-mapping.ui-schema.ts'

describe('combinerMappingUiSchema', () => {
  it('should detect errors in  schema', () => {
    expect(combinerMappingUiSchema()).toStrictEqual(
      expect.objectContaining({
        'ui:submitButtonOptions': {
          norender: true,
        },
        'ui:initTab': undefined,
        'ui:tabs': [
          expect.objectContaining({
            id: 'combinerTab',
            properties: ['id', 'name', 'description'],
          }),
          expect.objectContaining({
            id: 'sourcesTab',
            properties: ['sources'],
          }),
          expect.objectContaining({
            id: 'mappingsTab',
            title: 'Mappings',
            properties: ['mappings'],
          }),
        ],
      })
    )
  })

  it('should detect errors in  schema', () => {
    expect(combinerMappingUiSchema(true)).toStrictEqual(
      expect.objectContaining({
        'ui:submitButtonOptions': {
          norender: true,
        },
        'ui:initTab': undefined,
        'ui:tabs': [
          expect.objectContaining({
            id: 'combinerTab',
            properties: ['id', 'name', 'description'],
          }),
          expect.objectContaining({
            id: 'sourcesTab',
            properties: ['sources'],
          }),
          expect.objectContaining({
            id: 'mappingsTab',
            title: 'Asset mappings',
            properties: ['mappings'],
          }),
        ],
      })
    )
  })

  it('should detect errors in  schema', () => {
    expect(combinerMappingUiSchema(false, 'sourcesTab')).toStrictEqual(
      expect.objectContaining({
        'ui:submitButtonOptions': {
          norender: true,
        },
        'ui:initTab': 'sourcesTab',
        'ui:tabs': [
          expect.objectContaining({
            id: 'combinerTab',
            properties: ['id', 'name', 'description'],
          }),
          expect.objectContaining({
            id: 'sourcesTab',
            properties: ['sources'],
          }),
          expect.objectContaining({
            id: 'mappingsTab',
            title: 'Mappings',
            properties: ['mappings'],
          }),
        ],
      })
    )
  })
})
