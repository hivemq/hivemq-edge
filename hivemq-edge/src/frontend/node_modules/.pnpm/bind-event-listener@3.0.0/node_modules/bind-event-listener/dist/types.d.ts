export type UnbindFn = () => void;
type UnknownFunction = (...args: any[]) => any;
export type InferEventType<TTarget> = TTarget extends {
    addEventListener(type: infer P, ...args: any): void;
    addEventListener(type: infer P2, ...args: any): void;
} ? P & string : never;
type InferEvent<TTarget, TType extends string> = `on${TType}` extends keyof TTarget ? Parameters<Extract<TTarget[`on${TType}`], UnknownFunction>>[0] : Event;
type ListenerObject<TEvent extends Event> = {
    handleEvent(this: ListenerObject<TEvent>, event: TEvent): void;
};
export type Listener<TTarget extends EventTarget, TType extends string> = ListenerObject<InferEvent<TTarget, TType>> | {
    (this: TTarget, ev: InferEvent<TTarget, TType>): void;
};
export type Binding<TTarget extends EventTarget = EventTarget, TType extends string = string> = {
    type: TType;
    listener: Listener<TTarget, TType>;
    options?: boolean | AddEventListenerOptions;
};
export {};
