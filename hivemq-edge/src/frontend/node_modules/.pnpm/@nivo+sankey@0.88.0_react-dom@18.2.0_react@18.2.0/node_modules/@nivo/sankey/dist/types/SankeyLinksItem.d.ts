/// <reference types="react" />
import { DefaultLink, DefaultNode, SankeyCommonProps, SankeyLinkDatum } from './types';
interface SankeyLinksItemProps<N extends DefaultNode, L extends DefaultLink> {
    link: SankeyLinkDatum<N, L>;
    layout: SankeyCommonProps<N, L>['layout'];
    path: string;
    color: string;
    opacity: number;
    blendMode: SankeyCommonProps<N, L>['linkBlendMode'];
    enableGradient: SankeyCommonProps<N, L>['enableLinkGradient'];
    setCurrent: (link: SankeyLinkDatum<N, L> | null) => void;
    isInteractive: SankeyCommonProps<N, L>['isInteractive'];
    onClick?: SankeyCommonProps<N, L>['onClick'];
    tooltip: SankeyCommonProps<N, L>['linkTooltip'];
}
export declare const SankeyLinksItem: <N extends DefaultNode, L extends DefaultLink>({ link, layout, path, color, opacity, blendMode, enableGradient, setCurrent, tooltip, isInteractive, onClick, }: SankeyLinksItemProps<N, L>) => JSX.Element;
export {};
//# sourceMappingURL=SankeyLinksItem.d.ts.map