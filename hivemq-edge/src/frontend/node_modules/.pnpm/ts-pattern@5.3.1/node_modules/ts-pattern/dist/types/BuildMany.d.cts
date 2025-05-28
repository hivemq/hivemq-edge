import { Compute, Iterator, UpdateAt } from './helpers.cjs';
export type BuildMany<data, xs extends readonly any[]> = xs extends any ? BuildOne<data, xs> : never;
type BuildOne<data, xs extends readonly any[]> = xs extends [
    [
        infer value,
        infer path
    ],
    ...infer tail
] ? BuildOne<Update<data, value, Extract<path, readonly PropertyKey[]>>, tail> : data;
type Update<data, value, path> = path extends readonly [
    infer head,
    ...infer tail
] ? data extends readonly [any, ...any] ? head extends number ? UpdateAt<data, Iterator<head>, Update<data[head], value, tail>> : never : data extends readonly (infer a)[] ? Update<a, value, tail>[] : data extends Set<infer a> ? Set<Update<a, value, tail>> : data extends Map<infer k, infer v> ? Map<k, Update<v, value, tail>> : head extends keyof data ? Compute<{
    [k in Exclude<keyof data, head>]: data[k];
} & {
    [k in head]: Update<data[k], value, tail>;
}> : data : value;
export {};
