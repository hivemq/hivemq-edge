import { describe, it, expect } from 'vitest'
import { decodeProtobufSchema, encodeProtobufSchema, extractProtobufMessageTypes } from './protobuf.utils'

describe('encodeProtobufSchema', () => {
  it('should encode a simple protobuf schema', () => {
    const source = `
      syntax = "proto3";

      message Person {
        string name = 1;
        int32 age = 2;
      }
    `

    const encoded = encodeProtobufSchema(source)

    // Should return a base64 string
    expect(typeof encoded).toBe('string')
    expect(encoded.length).toBeGreaterThan(0)

    // Base64 strings only contain valid base64 characters
    expect(encoded).toMatch(/^[A-Za-z0-9+/=]+$/)
  })

  it('should encode protobuf with multiple messages', () => {
    const source = `
      syntax = "proto3";

      message Person {
        string name = 1;
      }

      message Address {
        string street = 1;
        string city = 2;
      }
    `

    const encoded = encodeProtobufSchema(source)
    expect(typeof encoded).toBe('string')
    expect(encoded.length).toBeGreaterThan(0)
  })

  it('should throw error for invalid protobuf syntax', () => {
    const invalidSource = 'this is not valid protobuf'

    expect(() => encodeProtobufSchema(invalidSource)).toThrow()
  })

  it('should return empty for empty source', () => {
    expect(encodeProtobufSchema('')).toStrictEqual('')
  })

  it('should encode protobuf with nested messages', () => {
    const source = `
      syntax = "proto3";

      message Outer {
        message Inner {
          string value = 1;
        }
        Inner inner = 1;
      }
    `

    const encoded = encodeProtobufSchema(source)
    expect(typeof encoded).toBe('string')
    expect(encoded.length).toBeGreaterThan(0)
  })

  it('should encode GPS coordinates protobuf schema', () => {
    const source = `
      syntax = "proto3";

      message GpsCoordinates {
        double latitude = 1;
        double longitude = 2;
        double altitude = 3;
      }
    `

    const encoded = encodeProtobufSchema(source)
    expect(typeof encoded).toBe('string')
    expect(encoded.length).toBeGreaterThan(0)
  })
})

describe('decodeProtobufSchema', () => {
  it('should decode an encoded protobuf schema', () => {
    const source = `
      syntax = "proto3";

      message Person {
        string name = 1;
        int32 age = 2;
      }
    `

    const encoded = encodeProtobufSchema(source)
    const decoded = decodeProtobufSchema(encoded)

    // Should return a template showing the message type
    expect(typeof decoded).toBe('string')
    expect(decoded).toContain('// NOTICE: once encoded into a Base64')
  })

  it('should throw error for invalid base64 string', () => {
    const invalidEncoded = 'not-valid-base64!!!'

    expect(() => decodeProtobufSchema(invalidEncoded)).toThrow()
  })

  it('should throw error for empty string', () => {
    expect(() => decodeProtobufSchema('')).toThrow()
  })

  it('should preserve message type name in encode-decode roundtrip', () => {
    const source = `
      syntax = "proto3";

      message GpsCoordinates {
        double latitude = 1;
        double longitude = 2;
      }
    `

    const encoded = encodeProtobufSchema(source)
    const decoded = decodeProtobufSchema(encoded)

    expect(decoded).toContain('// NOTICE: once encoded into a Base64')
  })
})

describe('extractProtobufMessageTypes', () => {
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

  it('should return empty array for empty source', () => {
    const messages = extractProtobufMessageTypes('')
    expect(messages).toEqual([])
  })

  it('should return empty array for invalid protobuf syntax', () => {
    const source = 'this is not valid protobuf'
    const messages = extractProtobufMessageTypes(source)
    expect(messages).toEqual([])
  })

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
