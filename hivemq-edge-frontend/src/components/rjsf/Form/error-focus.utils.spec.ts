import { expect } from 'vitest'

import {
  _toPath,
  deepGet,
  isNumeric,
  isPropertyBehindCollapsedElement,
  isPropertyBehindTab,
} from '@/components/rjsf/Form/error-focus.utils.ts'

describe('_toPath', () => {
  it.each([
    {
      path: '',
      value: null,
    },
    {
      path: 'a.b.c',
      value: ['a', 'b', 'c'],
    },
    {
      path: 'a[0].b.c',
      value: ['a', '0', 'b', 'c'],
    },
  ])('should return $value for $path', ({ path, value }) => {
    expect(_toPath(path)).toStrictEqual(value)
  })
})

describe('isNumeric', () => {
  it.each(['1', '1000'])('$item should be a number', (item) => {
    expect(isNumeric(item)).toStrictEqual(true)
  })
  it.each(['1.', '1.5', '-1'])('$item should not be a number', (item) => {
    expect(isNumeric(item)).toStrictEqual(false)
  })
})

const MOCK_OBJECT = {
  foo: {
    foz: [1, 2, 3],
    bar: {
      baz: ['a', 'b', 'c'],
    },
  },
}

describe('deepGet', () => {
  it('should return null for an empty object', async () => {
    expect(deepGet({}, [1])).toStrictEqual(null)
  })

  it('should return null for an empty key', async () => {
    expect(deepGet(MOCK_OBJECT, [])).toStrictEqual(null)
  })

  it.each([
    {
      keys: ['foo', 'bar'],
      value: {
        baz: ['a', 'b', 'c'],
      },
    },
    { keys: ['foo', 'foz', 2], value: 3 },
    { keys: ['foo', 'bar', 'baz', 1], value: 'b' },
    { keys: ['foo', 'bar', 'baz', 8, 'foz'], value: null },
  ])('should return $value for $keys', ({ keys, value }) => {
    expect(deepGet(MOCK_OBJECT, keys)).toStrictEqual(value)
  })
})

describe('isPropertyBehindCollapsedElement', () => {
  it.each([
    {
      property: '',
    },
    {
      property: 'id',
    },
    {
      property: '.property',
    },
    {
      property: '.property.without.any.array',
    },
  ])('should be undefined for $property', ({ property }) => {
    expect(isPropertyBehindCollapsedElement(property, {})).toStrictEqual(undefined)
  })

  it.each([
    {
      property: '.property.0.with_no_items',
      uiSchema: {
        property: {},
      },
      value: undefined,
    },
    {
      property: '.property.0.item.without_collapsable_tag',
      uiSchema: {
        property: {
          items: {
            'ui:title': 'ss',
          },
        },
      },
      value: undefined,
    },
    {
      property: '.property.0.item.with_collapsable_tag',
      uiSchema: {
        property: {
          items: {
            'ui:title': 'ss',
            'ui:collapsable': 'ss',
          },
        },
      },
      value: ['root', 'property', '0'],
    },
    {
      property: '.property.0.subProp.1.another.level.2.item',
      uiSchema: {
        property: {
          items: {
            'ui:title': 'ss',
            'ui:collapsable': 'ss',
          },
        },
      },
      value: ['root', 'property', '0'],
    },
  ])('should return $value for $property', ({ property, uiSchema, value }) => {
    expect(isPropertyBehindCollapsedElement(property, uiSchema)).toStrictEqual(value)
  })
})

describe('isPropertyBehindTab', () => {
  it('should return undefined if tabs are not defined', async () => {
    expect(isPropertyBehindTab('whatever.the.property.path', { test: 1 })).toStrictEqual(undefined)
    expect(isPropertyBehindTab('whatever.the.property.path', { 'ui:tabs': 1 })).toStrictEqual(undefined)
  })

  it('should return undefined if property is not defined', async () => {
    expect(isPropertyBehindTab('', { 'ui:tabs': [] })).toStrictEqual(undefined)
  })

  it('should return undefined if property is not in the tabs', async () => {
    expect(isPropertyBehindTab('.test.property', { 'ui:tabs': [] })).toStrictEqual(undefined)
  })

  it('should return undefined if property is not in the tabs', async () => {
    expect(isPropertyBehindTab('.test.property', { 'ui:tabs': [{ wrongProperties: '' }] })).toStrictEqual(undefined)
  })
  it('should return undefined if property is not in the tabs', async () => {
    const mockUISchema = { 'ui:tabs': [{ id: 'tab1', title: 'tab 1', properties: ['prop1', 'prop2'] }] }
    expect(isPropertyBehindTab('.test.property', mockUISchema)).toStrictEqual(undefined)
  })
  it('should return tab1 if property is in the tab', async () => {
    const mockUISchema = { 'ui:tabs': [{ id: 'tab1', title: 'tab 1', properties: ['test', 'prop2'] }] }
    expect(isPropertyBehindTab('.test.property', mockUISchema)).toStrictEqual({
      id: 'tab1',
      title: 'tab 1',
      properties: ['test', 'prop2'],
      index: 0,
    })
  })
  it('should return rab2 if property is in the tab', async () => {
    const mockUISchema = {
      'ui:tabs': [
        { id: 'tab1', title: 'tab 1', properties: ['prop123'] },
        { id: 'tab2', title: 'tab 2', properties: ['prop456', 'test'] },
        { id: 'tab3', title: 'tab 3', properties: ['prop789'] },
      ],
    }
    expect(isPropertyBehindTab('.test.property', mockUISchema)).toStrictEqual({
      id: 'tab2',
      title: 'tab 2',
      properties: ['prop456', 'test'],
      index: 1,
    })
  })
})
