import { describe, expect } from 'vitest'
import { decodeDataUriJsonSchema } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'

interface Suite {
  data: string
  error: string
}

describe('decodeDataUriJsonSchema', () => {
  const tests: Suite[] = [
    { data: '', error: 'Not a valid data-url encoded JSONSchema' },
    { data: '123', error: 'Not a valid data-url encoded JSONSchema' },
    { data: '123,456', error: 'No scheme defined in the URI' },
    { data: '123:test,456', error: "The scheme of the uri is not defined as 'data'" },
    { data: 'data:test,456', error: "The media types doesn't include the mandatory `application/schema+json`" },
    { data: 'data:application/json,456', error: "The media types doesn't include the mandatory `base64`" },
    { data: 'data:application/json;base64,456', error: 'The data is not properly encoded as a `base64` string' },
    {
      data: 'data:application/json;base64,ewogICJ0ZXN0IjogMQp9Cg==',
      error: 'Not a valid JSONSchema: `properties` is missing',
    },
  ]

  it.each<Suite>(tests)('$data should throw $error', ({ data, error }) => {
    expect(() => decodeDataUriJsonSchema(data)).toThrowError(error)
  })

  it('should return a valid json object', () => {
    expect(
      decodeDataUriJsonSchema(
        'data:application/json;base64,IHsKICAgICJ0eXBlIiA6ICJvYmplY3QiLAogICAgInByb3BlcnRpZXMiIDogewogICAgICAiZGVmaW5pdGlvbiIgOiB7CiAgICAgICAgInR5cGUiIDogIm9iamVjdCIsCiAgICAgICAgInByb3BlcnRpZXMiIDogewogICAgICAgICAgImRhdGFUeXBlIiA6IHsKICAgICAgICAgICAgInR5cGUiIDogInN0cmluZyIsCiAgICAgICAgICAgICJlbnVtIiA6IFsgIk5VTEwiLCAiQk9PTCIsICJCWVRFIiwgIldPUkQiLCAiRFdPUkQiLCAiTFdPUkQiLCAiVVNJTlQiLCAiVUlOVCIsICJVRElOVCIsICJVTElOVCIsICJTSU5UIiwgIklOVCIsICJESU5UIiwgIkxJTlQiLCAiUkVBTCIsICJMUkVBTCIsICJDSEFSIiwgIldDSEFSIiwgIlNUUklORyIsICJXU1RSSU5HIiwgIlRJTUUiLCAiTFRJTUUiLCAiREFURSIsICJMREFURSIsICJUSU1FX09GX0RBWSIsICJMVElNRV9PRl9EQVkiLCAiREFURV9BTkRfVElNRSIsICJMREFURV9BTkRfVElNRSIsICJSQVdfQllURV9BUlJBWSIgXSwKICAgICAgICAgICAgInRpdGxlIiA6ICJEYXRhIFR5cGUiLAogICAgICAgICAgICAiZGVzY3JpcHRpb24iIDogIlRoZSBleHBlY3RlZCBkYXRhIHR5cGUgb2YgdGhlIHRhZyIsCiAgICAgICAgICAgICJlbnVtTmFtZXMiIDogWyAiTnVsbCIsICJCb29sZWFuIiwgIkJ5dGUiLCAiV29yZCAodW5pdCAxNikiLCAiRFdvcmQgKHVpbnQgMzIpIiwgIkxXb3JkICh1aW50IDY0KSIsICJVU2ludCAodWludCA4KSIsICJVaW50ICh1aW50IDE2KSIsICJVRGludCAodWludCAzMikiLCAiVUxpbnQgKHVpbnQgNjQpIiwgIlNpbnQgKGludCA4KSIsICJJbnQgKGludCAxNikiLCAiRGludCAoaW50IDMyKSIsICJMaW50IChpbnQgNjQpIiwgIlJlYWwgKGZsb2F0IDMyKSIsICJMUmVhbCAoZG91YmxlIDY0KSIsICJDaGFyICgxIGJ5dGUgY2hhcikiLCAiV0NoYXIgKDIgYnl0ZSBjaGFyKSIsICJTdHJpbmciLCAiV1N0cmluZyIsICJUaW1pbmcgKER1cmF0aW9uIG1zKSIsICJMb25nIFRpbWluZyAoRHVyYXRpb24gbnMpIiwgIkRhdGUgKERhdGVTdGFtcCkiLCAiTG9uZyBEYXRlIChEYXRlU3RhbXApIiwgIlRpbWUgT2YgRGF5IChUaW1lU3RhbXApIiwgIkxvbmcgVGltZSBPZiBEYXkgKFRpbWVTdGFtcCkiLCAiRGF0ZSBUaW1lIChEYXRlVGltZVN0YW1wKSIsICJMb25nIERhdGUgVGltZSAoRGF0ZVRpbWVTdGFtcCkiLCAiUmF3IEJ5dGUgQXJyYXkiIF0KICAgICAgICAgIH0sCiAgICAgICAgICAidGFnQWRkcmVzcyIgOiB7CiAgICAgICAgICAgICJ0eXBlIiA6ICJzdHJpbmciLAogICAgICAgICAgICAidGl0bGUiIDogIlRhZyBBZGRyZXNzIiwKICAgICAgICAgICAgImRlc2NyaXB0aW9uIiA6ICJUaGUgd2VsbCBmb3JtZWQgYWRkcmVzcyBvZiB0aGUgdGFnIHRvIHJlYWQiCiAgICAgICAgICB9CiAgICAgICAgfSwKICAgICAgICAicmVxdWlyZWQiIDogWyAiZGF0YVR5cGUiLCAidGFnQWRkcmVzcyIgXSwKICAgICAgICAidGl0bGUiIDogImRlZmluaXRpb24iLAogICAgICAgICJkZXNjcmlwdGlvbiIgOiAiVGhlIGFjdHVhbCBkZWZpbml0aW9uIG9mIHRoZSB0YWcgb24gdGhlIGRldmljZSIKICAgICAgfSwKICAgICAgImRlc2NyaXB0aW9uIiA6IHsKICAgICAgICAidHlwZSIgOiAic3RyaW5nIiwKICAgICAgICAidGl0bGUiIDogImRlc2NyaXB0aW9uIiwKICAgICAgICAiZGVzY3JpcHRpb24iIDogIkEgaHVtYW4gcmVhZGFibGUgZGVzY3JpcHRpb24gb2YgdGhlIHRhZyIKICAgICAgfSwKICAgICAgIm5hbWUiIDogewogICAgICAgICJ0eXBlIiA6ICJzdHJpbmciLAogICAgICAgICJ0aXRsZSIgOiAibmFtZSIsCiAgICAgICAgImRlc2NyaXB0aW9uIiA6ICJuYW1lIG9mIHRoZSB0YWcgdG8gYmUgdXNlZCBpbiBtYXBwaW5ncyIKICAgICAgfQogICAgfSwKICAgICJyZXF1aXJlZCIgOiBbICJkZWZpbml0aW9uIiwgImRlc2NyaXB0aW9uIiwgIm5hbWUiIF0KICB9Cg=='
      )
    ).toBeFalsy
  })
})
