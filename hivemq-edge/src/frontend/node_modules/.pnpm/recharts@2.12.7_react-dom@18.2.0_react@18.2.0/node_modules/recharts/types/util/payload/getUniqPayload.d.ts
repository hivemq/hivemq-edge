type UniqueFunc<T> = (entry: T) => unknown;
export type UniqueOption<T> = boolean | UniqueFunc<T>;
export declare function getUniqPayload<T>(payload: Array<T>, option: UniqueOption<T>, defaultUniqBy: UniqueFunc<T>): Array<T>;
export {};
