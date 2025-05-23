import { LinkCanvasRendererProps, NodeCanvasRendererProps, LabelCanvasRendererProps } from './types';
export declare const renderNode: <Datum>(ctx: CanvasRenderingContext2D, { node }: NodeCanvasRendererProps<Datum>) => void;
export declare const renderLink: <Datum>(ctx: CanvasRenderingContext2D, { link, linkGenerator }: LinkCanvasRendererProps<Datum>) => void;
export declare const renderLabel: <Datum>(ctx: CanvasRenderingContext2D, { label, theme }: LabelCanvasRendererProps<Datum>) => void;
//# sourceMappingURL=canvas.d.ts.map