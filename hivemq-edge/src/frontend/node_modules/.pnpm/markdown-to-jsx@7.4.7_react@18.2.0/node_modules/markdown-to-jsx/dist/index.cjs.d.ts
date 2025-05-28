/// <reference types="react" />
import { compiler } from './';
declare const _default: import("react").FC<{
    [key: string]: any;
    children: string;
    options?: Partial<{
        createElement: (tag: string | import("react").FunctionComponent<{}> | import("react").ComponentClass<{}, any>, props: JSX.IntrinsicAttributes, ...children: import("react").ReactChild[]) => import("react").ReactChild;
        disableParsingRawHTML: boolean;
        enforceAtxHeadings: boolean;
        forceBlock: boolean;
        forceInline: boolean;
        forceWrapper: boolean;
        namedCodesToUnicode: {
            [key: string]: string;
        };
        overrides: import("./").MarkdownToJSX.Overrides;
        renderRule: (next: () => import("react").ReactChild, node: import("./").MarkdownToJSX.ParserResult, renderChildren: import("./").MarkdownToJSX.RuleOutput, state: import("./").MarkdownToJSX.State) => import("react").ReactChild;
        slugify: (source: string) => string;
        wrapper: import("react").ElementType<any>;
    }>;
}> & {
    compiler: typeof compiler;
};
export default _default;
