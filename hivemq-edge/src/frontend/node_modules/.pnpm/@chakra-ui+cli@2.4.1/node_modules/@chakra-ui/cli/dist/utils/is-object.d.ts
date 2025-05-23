type Dict<T = any> = Record<string, T>;
declare function isObject(value: any): value is Dict;

export { isObject };
