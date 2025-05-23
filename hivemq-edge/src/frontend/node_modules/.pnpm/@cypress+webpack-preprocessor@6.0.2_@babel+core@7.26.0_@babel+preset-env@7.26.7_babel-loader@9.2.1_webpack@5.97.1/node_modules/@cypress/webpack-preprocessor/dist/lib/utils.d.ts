import Bluebird from 'bluebird';
declare function createDeferred<T>(): {
    resolve: (thenableOrResult?: T | PromiseLike<T> | undefined) => void;
    reject: any;
    promise: Bluebird<T>;
};
declare const _default: {
    createDeferred: typeof createDeferred;
};
export default _default;
