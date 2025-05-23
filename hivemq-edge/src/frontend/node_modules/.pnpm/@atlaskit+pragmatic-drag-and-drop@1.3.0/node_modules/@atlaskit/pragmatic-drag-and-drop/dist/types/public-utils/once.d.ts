/** Provide a function that you only ever want to be called a single time */
export declare function once<TFunc extends (...args: any[]) => any>(fn: TFunc): (this: ThisParameterType<TFunc>, ...args: Parameters<TFunc>) => ReturnType<TFunc>;
