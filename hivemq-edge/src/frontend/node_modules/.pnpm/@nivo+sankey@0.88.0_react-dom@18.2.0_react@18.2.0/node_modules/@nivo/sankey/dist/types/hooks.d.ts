/// <reference types="react" />
import { DefaultLink, DefaultNode, SankeyAlignFunction, SankeyCommonProps, SankeyDataProps, SankeyLinkDatum, SankeyNodeDatum, SankeySortFunction } from './types';
export declare const computeNodeAndLinks: <N extends DefaultNode, L extends DefaultLink>({ data: _data, formatValue, layout, alignFunction, sortFunction, linkSortMode, nodeThickness, nodeSpacing, nodeInnerPadding, width, height, getColor, getLabel, }: {
    data: {
        nodes: N[];
        links: L[];
    };
    formatValue: (value: number) => string;
    layout: "horizontal" | "vertical";
    alignFunction: SankeyAlignFunction;
    sortFunction: SankeySortFunction<N, L> | null | undefined;
    linkSortMode: null | undefined;
    nodeThickness: number;
    nodeSpacing: number;
    nodeInnerPadding: number;
    width: number;
    height: number;
    getColor: (node: Omit<SankeyNodeDatum<N, L>, "color" | "label">) => string;
    getLabel: (node: Omit<SankeyNodeDatum<N, L>, "color" | "label">) => string;
}) => {
    nodes: SankeyNodeDatum<N, L>[];
    links: SankeyLinkDatum<N, L>[];
};
export declare const useSankey: <N extends DefaultNode, L extends DefaultLink>({ data, valueFormat, layout, width, height, sort, align, colors, nodeThickness, nodeSpacing, nodeInnerPadding, nodeBorderColor, label, labelTextColor, }: {
    data: {
        nodes: N[];
        links: L[];
    };
    valueFormat?: import("@nivo/core").ValueFormat<number, void> | undefined;
    layout: "horizontal" | "vertical";
    width: number;
    height: number;
    sort: import("./types").SankeySortType | SankeySortFunction<N, L>;
    align: import("./types").SankeyAlignType | SankeyAlignFunction;
    colors: import("@nivo/colors").OrdinalColorScaleConfig<Omit<SankeyNodeDatum<N, L>, "color" | "label">>;
    nodeThickness: number;
    nodeSpacing: number;
    nodeInnerPadding: number;
    nodeBorderColor: import("@nivo/colors").InheritedColorConfig<SankeyNodeDatum<N, L>>;
    label: import("@nivo/core").PropertyAccessor<Omit<SankeyNodeDatum<N, L>, "color" | "label">, string>;
    labelTextColor: import("@nivo/colors").InheritedColorConfig<SankeyNodeDatum<N, L>>;
}) => {
    nodes: SankeyNodeDatum<N, L>[];
    links: SankeyLinkDatum<N, L>[];
    legendData: {
        id: string;
        label: string;
        color: string;
    }[];
    getNodeBorderColor: import("@nivo/colors").InheritedColorConfigCustomFunction<SankeyNodeDatum<N, L>> | ((d: SankeyNodeDatum<N, L>) => any);
    currentNode: SankeyNodeDatum<N, L> | null;
    setCurrentNode: import("react").Dispatch<import("react").SetStateAction<SankeyNodeDatum<N, L> | null>>;
    currentLink: SankeyLinkDatum<N, L> | null;
    setCurrentLink: import("react").Dispatch<import("react").SetStateAction<SankeyLinkDatum<N, L> | null>>;
    getLabelTextColor: import("@nivo/colors").InheritedColorConfigCustomFunction<SankeyNodeDatum<N, L>> | ((d: SankeyNodeDatum<N, L>) => any);
};
//# sourceMappingURL=hooks.d.ts.map