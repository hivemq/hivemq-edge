import { describe, it, expect } from 'vitest'
import { extractProtobufMessageTypes } from './protobuf.utils'

describe('extractProtobufMessageTypes', () => {
  // ✅ ACTIVE - Basic functionality test
  it('should extract message types from simple protobuf source', () => {
    const source = `
      syntax = "proto3";

      message Person {
        string name = 1;
        int32 age = 2;
      }

      message Address {
        string street = 1;
        string city = 2;
      }
    `

    const messages = extractProtobufMessageTypes(source)
    expect(messages).toContain('Person')
    expect(messages).toContain('Address')
    expect(messages).toHaveLength(2)
  })

  // ✅ ACTIVE - Nested messages
  it('should extract nested message types with dot notation', () => {
    const source = `
      syntax = "proto3";

      message Outer {
        message Inner {
          string value = 1;
        }
        Inner inner = 1;
      }
    `

    const messages = extractProtobufMessageTypes(source)
    expect(messages).toContain('Outer')
    expect(messages).toContain('Outer.Inner')
    expect(messages).toHaveLength(2)
  })

  // ✅ ACTIVE - Empty source
  it('should return empty array for empty source', () => {
    const messages = extractProtobufMessageTypes('')
    expect(messages).toEqual([])
  })

  // ✅ ACTIVE - Invalid syntax
  it('should return empty array for invalid protobuf syntax', () => {
    const source = 'this is not valid protobuf'
    const messages = extractProtobufMessageTypes(source)
    expect(messages).toEqual([])
  })

  // ✅ ACTIVE - Real-world example (GPS coordinates)
  it('should extract message from GPS coordinates example', () => {
    const source = `
      syntax = "proto3";

      message GpsCoordinates {
        double latitude = 1;
        double longitude = 2;
        double altitude = 3;
      }
    `

    const messages = extractProtobufMessageTypes(source)
    expect(messages).toContain('GpsCoordinates')
    expect(messages).toHaveLength(1)
  })

  // ✅ ACTIVE - Multiple levels of nesting
  it('should handle deeply nested messages', () => {
    const source = `
      syntax = "proto3";

      message Level1 {
        message Level2 {
          message Level3 {
            string value = 1;
          }
          Level3 deep = 1;
        }
        Level2 mid = 1;
      }
    `

    const messages = extractProtobufMessageTypes(source)
    expect(messages).toContain('Level1')
    expect(messages).toContain('Level1.Level2')
    expect(messages).toContain('Level1.Level2.Level3')
    expect(messages).toHaveLength(3)
  })
})
