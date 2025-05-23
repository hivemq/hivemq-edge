/// <reference types="react" />
import { DefaultLink, DefaultNode, SankeyCommonProps, SankeyLinkDatum, SankeyNodeDatum } from './types';
interface SankeyLinksProps<N extends DefaultNode, L extends DefaultLink> {
    layout: SankeyCommonProps<N, L>['layout'];
    links: SankeyLinkDatum<N, L>[];
    linkOpacity: SankeyCommonProps<N, L>['linkOpacity'];
    linkHoverOpacity: SankeyCommonProps<N, L>['linkHoverOpacity'];
    linkHoverOthersOpacity: SankeyCommonProps<N, L>['linkHoverOthersOpacity'];
    linkContract: SankeyCommonProps<N, L>['linkContract'];
    linkBlendMode: SankeyCommonProps<N, L>['linkBlendMode'];
    enableLinkGradient: SankeyCommonProps<N, L>['enableLinkGradient'];
    tooltip: SankeyCommonProps<N, L>['linkTooltip'];
    setCurrentLink: (link: SankeyLinkDatum<N, L> | null) => void;
    currentLink: SankeyLinkDatum<N, L> | null;
    currentNode: SankeyNodeDatum<N, L> | null;
    isCurrentLink: (link: SankeyLinkDatum<N, L>) => boolean;
    isInteractive: SankeyCommonProps<N, L>['isInteractive'];
    onClick?: SankeyCommonProps<N, L>['onClick'];
}
export declare const SankeyLinks: <N extends DefaultNode, L extends DefaultLink>({ links, layout, linkOpacity, linkHoverOpacity, linkHoverOthersOpacity, linkContract, linkBlendMode, enableLinkGradient, setCurrentLink, currentLink, currentNode, isCurrentLink, isInteractive, onClick, tooltip, }: SankeyLinksProps<N, L>) => JSX.Element;
export {};
//# sourceMappingURL=SankeyLinks.d.ts.map