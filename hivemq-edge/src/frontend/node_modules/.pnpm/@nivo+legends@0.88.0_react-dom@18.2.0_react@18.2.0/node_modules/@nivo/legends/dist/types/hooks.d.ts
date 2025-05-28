type Scale = {
    (value: number): number;
    invertExtent: (value: number) => [number, number];
    range: () => number[];
};
export declare const useQuantizeColorScaleLegendData: ({ scale, domain: overriddenDomain, reverse, valueFormat, separator, }: {
    scale: Scale;
    domain?: number[] | undefined;
    reverse?: boolean | undefined;
    valueFormat?: (<T, U>(value: T) => T | U) | undefined;
    separator?: string | undefined;
}) => {
    id: number;
    index: number;
    extent: number[];
    label: string;
    value: number;
    color: number;
}[];
export {};
//# sourceMappingURL=hooks.d.ts.map