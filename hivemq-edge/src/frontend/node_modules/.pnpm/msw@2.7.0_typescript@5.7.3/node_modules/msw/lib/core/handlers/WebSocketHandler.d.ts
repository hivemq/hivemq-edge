import { Emitter } from 'strict-event-emitter';
import { WebSocketConnectionData } from '@mswjs/interceptors/WebSocket';
import { PathParams, Path, Match } from '../utils/matching/matchRequestUrl.js';

type WebSocketHandlerParsedResult = {
    match: Match;
};
type WebSocketHandlerEventMap = {
    connection: [args: WebSocketHandlerConnection];
};
interface WebSocketHandlerConnection extends WebSocketConnectionData {
    params: PathParams;
}
declare const kEmitter: unique symbol;
declare const kDispatchEvent: unique symbol;
declare const kSender: unique symbol;
declare class WebSocketHandler {
    private readonly url;
    private readonly __kind;
    id: string;
    callFrame?: string;
    protected [kEmitter]: Emitter<WebSocketHandlerEventMap>;
    constructor(url: Path);
    parse(args: {
        event: MessageEvent<WebSocketConnectionData>;
    }): WebSocketHandlerParsedResult;
    predicate(args: {
        event: MessageEvent<WebSocketConnectionData>;
        parsedResult: WebSocketHandlerParsedResult;
    }): boolean;
    [kDispatchEvent](event: MessageEvent<WebSocketConnectionData>): Promise<void>;
}

export { WebSocketHandler, type WebSocketHandlerConnection, type WebSocketHandlerEventMap, kDispatchEvent, kEmitter, kSender };
