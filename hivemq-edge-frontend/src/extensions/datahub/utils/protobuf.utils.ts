import type { Root } from 'protobufjs'
import { parse, Type } from 'protobufjs'

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
