/// <reference types="react" />
import { sankeyCenter, sankeyJustify, sankeyLeft, sankeyRight } from 'd3-sankey';
import { SankeyLayerId, SankeyAlignType } from './types';
import { InheritedColorConfig } from '@nivo/colors';
export declare const sankeyAlignmentPropMapping: {
    center: typeof sankeyCenter;
    justify: typeof sankeyJustify;
    start: typeof sankeyLeft;
    end: typeof sankeyRight;
};
export declare const sankeyAlignmentPropKeys: SankeyAlignType[];
export declare const sankeyAlignmentFromProp: (prop: SankeyAlignType) => typeof sankeyCenter | typeof sankeyJustify | typeof sankeyLeft | typeof sankeyRight;
export declare const svgDefaultProps: {
    layout: "horizontal";
    align: "center";
    sort: "auto";
    colors: {
        scheme: "nivo";
    };
    nodeOpacity: number;
    nodeHoverOpacity: number;
    nodeHoverOthersOpacity: number;
    nodeThickness: number;
    nodeSpacing: number;
    nodeInnerPadding: number;
    nodeBorderWidth: number;
    nodeBorderColor: InheritedColorConfig<any>;
    nodeBorderRadius: number;
    linkOpacity: number;
    linkHoverOpacity: number;
    linkHoverOthersOpacity: number;
    linkContract: number;
    linkBlendMode: "multiply";
    enableLinkGradient: boolean;
    enableLabels: boolean;
    label: string;
    labelPosition: "inside";
    labelPadding: number;
    labelOrientation: "horizontal";
    labelTextColor: InheritedColorConfig<any>;
    isInteractive: boolean;
    nodeTooltip: <N extends import("./types").DefaultNode, L extends import("./types").DefaultLink>({ node, }: import("./SankeyNodeTooltip").SankeyNodeTooltipProps<N, L>) => JSX.Element;
    linkTooltip: <N_1 extends import("./types").DefaultNode, L_1 extends import("./types").DefaultLink>({ link, }: import("./SankeyLinkTooltip").SankeyLinkTooltipProps<N_1, L_1>) => JSX.Element;
    legends: never[];
    layers: SankeyLayerId[];
    role: string;
    animate: boolean;
    motionConfig: string;
};
//# sourceMappingURL=props.d.ts.map