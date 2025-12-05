import type { Root } from 'protobufjs'
import { parse, Type, util as protobufUtils } from 'protobufjs'
import descriptor from 'protobufjs/ext/descriptor'

import i18n from '@/config/i18n.config.ts'

/**
 * Encodes a protobuf source string as a base64-encoded FileDescriptorSet
 * This is the format required by the HiveMQ DataHub API for protobuf schemas
 *
 * @param source - The protobuf schema source code
 * @returns Base64-encoded FileDescriptorSet
 * @throws Error if encoding fails or verification fails
 */
export function encodeProtobufSchema(source: string): string {
  // Parse the protobuf source
  const root = parse(source).root

  // Convert to FileDescriptorSet
  // @ts-ignore No typescript definition for toDescriptor
  const descriptorMessage = root.toDescriptor('proto3')
  const buffer = descriptor.FileDescriptorSet.encode(descriptorMessage).finish()
  const encoded = protobufUtils.base64.encode(buffer, 0, buffer.length)

  // Verify the encoding by decoding and comparing
  const encodedGraphBytes = new Uint8Array(protobufUtils.base64.length(encoded))
  protobufUtils.base64.decode(encoded, encodedGraphBytes, 0)
  const decodedMessage = descriptor.FileDescriptorSet.decode(encodedGraphBytes)

  if (JSON.stringify(descriptorMessage) !== JSON.stringify(decodedMessage)) {
    throw new Error('Protobuf encoding verification failed')
  }

  return encoded
}

/**
 * Decodes a base64-encoded FileDescriptorSet back to protobuf source code
 * Note: This creates a placeholder template since the original source cannot be fully recovered
 *
 * @param encoded - Base64-encoded FileDescriptorSet
 * @returns Protobuf source code (template showing the message type)
 * @throws Error if decoding fails
 */
export function decodeProtobufSchema(encoded: string): string {
  // Decode the base64-encoded FileDescriptorSet
  const encodedGraphBytes = new Uint8Array(protobufUtils.base64.length(encoded))
  protobufUtils.base64.decode(encoded, encodedGraphBytes, 0)
  const decodedMessage = descriptor.FileDescriptorSet.decode(encodedGraphBytes)

  // Extract the message type name from the descriptor
  // @ts-ignore No typescript definition for FileDescriptorSet structure
  const messageTypeName = decodedMessage.file[0]?.messageType[0]?.name

  if (!messageTypeName) {
    throw new Error('Cannot decode protobuf schema: no message type found')
  }

  // Return a template showing the message type
  // Note: The original source code cannot be fully recovered from FileDescriptorSet
  return i18n.t('datahub:error.validation.protobuf.template', { source: 'UNKNOWN' })
}

/**
 * Extracts all message type names from a protobuf source string
 * @param source - The protobuf schema source code
 * @returns Array of message type names found in the source
 */
export function extractProtobufMessageTypes(source: string): string[] {
  try {
    const root = parse(source).root
    return extractMessagesFromRoot(root)
  } catch (e) {
    // If parsing fails, return empty array
    return []
  }
}

/**
 * Recursively extracts message type names from a protobuf Root
 * @param root - The parsed protobuf root
 * @param prefix - Namespace prefix for nested messages
 * @returns Array of message type names
 */
function extractMessagesFromRoot(root: Root, prefix = ''): string[] {
  const messages: string[] = []

  // Iterate through all nested objects in the root
  root.nestedArray.forEach((nested) => {
    if (nested instanceof Type) {
      // This is a message type
      const messageName = prefix ? `${prefix}.${nested.name}` : nested.name
      messages.push(messageName)

      // Recursively check for nested messages within this message
      if (nested.nestedArray.length > 0) {
        const nestedMessages = extractMessagesFromRoot(nested as unknown as Root, messageName)
        messages.push(...nestedMessages)
      }
    }
  })

  return messages
}
