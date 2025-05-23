import { kDispatchEvent } from '../handlers/WebSocketHandler.mjs';
import { webSocketInterceptor } from './webSocketInterceptor.mjs';
import {
  onUnhandledRequest
} from '../utils/request/onUnhandledRequest.mjs';
import { isHandlerKind } from '../utils/internal/isHandlerKind.mjs';
function handleWebSocketEvent(options) {
  webSocketInterceptor.on("connection", async (connection) => {
    const handlers = options.getHandlers();
    const connectionEvent = new MessageEvent("connection", {
      data: connection
    });
    const matchingHandlers = [];
    for (const handler of handlers) {
      if (isHandlerKind("EventHandler")(handler) && handler.predicate({
        event: connectionEvent,
        parsedResult: handler.parse({
          event: connectionEvent
        })
      })) {
        matchingHandlers.push(handler);
      }
    }
    if (matchingHandlers.length > 0) {
      options?.onMockedConnection(connection);
      for (const handler of matchingHandlers) {
        handler[kDispatchEvent](connectionEvent);
      }
    } else {
      const request = new Request(connection.client.url, {
        headers: {
          upgrade: "websocket",
          connection: "upgrade"
        }
      });
      await onUnhandledRequest(
        request,
        options.getUnhandledRequestStrategy()
      ).catch((error) => {
        const errorEvent = new Event("error");
        Object.defineProperty(errorEvent, "cause", {
          enumerable: true,
          configurable: false,
          value: error
        });
        connection.client.socket.dispatchEvent(errorEvent);
      });
      options?.onPassthroughConnection(connection);
      connection.server.connect();
    }
  });
}
export {
  handleWebSocketEvent
};
//# sourceMappingURL=handleWebSocketEvent.mjs.map