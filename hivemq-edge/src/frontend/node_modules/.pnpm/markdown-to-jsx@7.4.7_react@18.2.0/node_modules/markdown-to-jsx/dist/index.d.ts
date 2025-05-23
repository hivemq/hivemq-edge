/**
 * markdown-to-jsx is a fork of
 * [simple-markdown v0.2.2](https://github.com/Khan/simple-markdown)
 * from Khan Academy. Thank you Khan devs for making such an awesome
 * and extensible parsing infra... without it, half of the
 * optimizations here wouldn't be feasible. 🙏🏼
 */
import * as React from 'react';
/**
 * Analogous to `node.type`. Please note that the values here may change at any time,
 * so do not hard code against the value directly.
 */
export declare const enum RuleType {
    blockQuote = "0",
    breakLine = "1",
    breakThematic = "2",
    codeBlock = "3",
    codeFenced = "4",
    codeInline = "5",
    footnote = "6",
    footnoteReference = "7",
    gfmTask = "8",
    heading = "9",
    headingSetext = "10",
    /** only available if not `disableHTMLParsing` */
    htmlBlock = "11",
    htmlComment = "12",
    /** only available if not `disableHTMLParsing` */
    htmlSelfClosing = "13",
    image = "14",
    link = "15",
    /** emits a `link` 'node', does not render directly */
    linkAngleBraceStyleDetector = "16",
    /** emits a `link` 'node', does not render directly */
    linkBareUrlDetector = "17",
    /** emits a `link` 'node', does not render directly */
    linkMailtoDetector = "18",
    newlineCoalescer = "19",
    orderedList = "20",
    paragraph = "21",
    ref = "22",
    refImage = "23",
    refLink = "24",
    table = "25",
    tableSeparator = "26",
    text = "27",
    textBolded = "28",
    textEmphasized = "29",
    textEscaped = "30",
    textMarked = "31",
    textStrikethroughed = "32",
    unorderedList = "33"
}
declare const enum Priority {
    /**
     * anything that must scan the tree before everything else
     */
    MAX = 0,
    /**
     * scans for block-level constructs
     */
    HIGH = 1,
    /**
     * inline w/ more priority than other inline
     */
    MED = 2,
    /**
     * inline elements
     */
    LOW = 3,
    /**
     * bare text and stuff that is considered leftovers
     */
    MIN = 4
}
export declare function slugify(str: string): string;
export declare function compiler(markdown?: string, options?: MarkdownToJSX.Options): JSX.Element;
/**
 * A simple HOC for easy React use. Feed the markdown content as a direct child
 * and the rest is taken care of automatically.
 */
declare const Markdown: React.FC<{
    [key: string]: any;
    children: string;
    options?: MarkdownToJSX.Options;
}>;
export declare namespace MarkdownToJSX {
    /**
     * RequireAtLeastOne<{ ... }> <- only requires at least one key
     */
    type RequireAtLeastOne<T, Keys extends keyof T = keyof T> = Pick<T, Exclude<keyof T, Keys>> & {
        [K in Keys]-?: Required<Pick<T, K>> & Partial<Pick<T, Exclude<Keys, K>>>;
    }[Keys];
    export type CreateElement = typeof React.createElement;
    export type HTMLTags = keyof JSX.IntrinsicElements;
    export type State = {
        /** true if the current content is inside anchor link grammar */
        inAnchor?: boolean;
        /** true if parsing in an inline context (subset of rules around formatting and links) */
        inline?: boolean;
        /** true if in a table */
        inTable?: boolean;
        /** use this for the `key` prop */
        key?: React.Key;
        /** true if in a list */
        list?: boolean;
        /** true if parsing in inline context w/o links */
        simple?: boolean;
    };
    export interface BlockQuoteNode {
        children: MarkdownToJSX.ParserResult[];
        type: RuleType.blockQuote;
    }
    export interface BreakLineNode {
        type: RuleType.breakLine;
    }
    export interface BreakThematicNode {
        type: RuleType.breakThematic;
    }
    export interface CodeBlockNode {
        type: RuleType.codeBlock;
        attrs?: JSX.IntrinsicAttributes;
        lang?: string;
        text: string;
    }
    export interface CodeFencedNode {
        type: RuleType.codeFenced;
    }
    export interface CodeInlineNode {
        type: RuleType.codeInline;
        text: string;
    }
    export interface FootnoteNode {
        type: RuleType.footnote;
    }
    export interface FootnoteReferenceNode {
        type: RuleType.footnoteReference;
        target: string;
        text: string;
    }
    export interface GFMTaskNode {
        type: RuleType.gfmTask;
        completed: boolean;
    }
    export interface HeadingNode {
        type: RuleType.heading;
        children: MarkdownToJSX.ParserResult[];
        id: string;
        level: 1 | 2 | 3 | 4 | 5 | 6;
    }
    export interface HeadingSetextNode {
        type: RuleType.headingSetext;
    }
    export interface HTMLCommentNode {
        type: RuleType.htmlComment;
    }
    export interface ImageNode {
        type: RuleType.image;
        alt?: string;
        target: string;
        title?: string;
    }
    export interface LinkNode {
        type: RuleType.link;
        children: MarkdownToJSX.ParserResult[];
        target: string;
        title?: string;
    }
    export interface LinkAngleBraceNode {
        type: RuleType.linkAngleBraceStyleDetector;
    }
    export interface LinkBareURLNode {
        type: RuleType.linkBareUrlDetector;
    }
    export interface LinkMailtoNode {
        type: RuleType.linkMailtoDetector;
    }
    export interface OrderedListNode {
        type: RuleType.orderedList;
        items: MarkdownToJSX.ParserResult[][];
        ordered: true;
        start?: number;
    }
    export interface UnorderedListNode {
        type: RuleType.unorderedList;
        items: MarkdownToJSX.ParserResult[][];
        ordered: false;
    }
    export interface NewlineNode {
        type: RuleType.newlineCoalescer;
    }
    export interface ParagraphNode {
        type: RuleType.paragraph;
        children: MarkdownToJSX.ParserResult[];
    }
    export interface ReferenceNode {
        type: RuleType.ref;
    }
    export interface ReferenceImageNode {
        type: RuleType.refImage;
        alt?: string;
        ref: string;
    }
    export interface ReferenceLinkNode {
        type: RuleType.refLink;
        children: MarkdownToJSX.ParserResult[];
        fallbackChildren: MarkdownToJSX.ParserResult[];
        ref: string;
    }
    export interface TableNode {
        type: RuleType.table;
        /**
         * alignment for each table column
         */
        align: ('left' | 'right' | 'center')[];
        cells: MarkdownToJSX.ParserResult[][][];
        header: MarkdownToJSX.ParserResult[][];
    }
    export interface TableSeparatorNode {
        type: RuleType.tableSeparator;
    }
    export interface TextNode {
        type: RuleType.text;
        text: string;
    }
    export interface BoldTextNode {
        type: RuleType.textBolded;
        children: MarkdownToJSX.ParserResult[];
    }
    export interface ItalicTextNode {
        type: RuleType.textEmphasized;
        children: MarkdownToJSX.ParserResult[];
    }
    export interface EscapedTextNode {
        type: RuleType.textEscaped;
    }
    export interface MarkedTextNode {
        type: RuleType.textMarked;
        children: MarkdownToJSX.ParserResult[];
    }
    export interface StrikethroughTextNode {
        type: RuleType.textStrikethroughed;
        children: MarkdownToJSX.ParserResult[];
    }
    export interface HTMLNode {
        type: RuleType.htmlBlock;
        attrs: JSX.IntrinsicAttributes;
        children?: ReturnType<MarkdownToJSX.NestedParser> | undefined;
        noInnerParse: Boolean;
        tag: MarkdownToJSX.HTMLTags;
        text?: string | undefined;
    }
    export interface HTMLSelfClosingNode {
        type: RuleType.htmlSelfClosing;
        attrs: JSX.IntrinsicAttributes;
        tag: string;
    }
    export type ParserResult = BlockQuoteNode | BreakLineNode | BreakThematicNode | CodeBlockNode | CodeFencedNode | CodeInlineNode | FootnoteNode | FootnoteReferenceNode | GFMTaskNode | HeadingNode | HeadingSetextNode | HTMLCommentNode | ImageNode | LinkNode | LinkAngleBraceNode | LinkBareURLNode | LinkMailtoNode | OrderedListNode | UnorderedListNode | NewlineNode | ParagraphNode | ReferenceNode | ReferenceImageNode | ReferenceLinkNode | TableNode | TableSeparatorNode | TextNode | BoldTextNode | ItalicTextNode | EscapedTextNode | MarkedTextNode | StrikethroughTextNode | HTMLNode | HTMLSelfClosingNode;
    export type NestedParser = (input: string, state?: MarkdownToJSX.State) => MarkdownToJSX.ParserResult[];
    export type Parser<ParserOutput> = (capture: RegExpMatchArray, nestedParse: NestedParser, state?: MarkdownToJSX.State) => ParserOutput;
    export type RuleOutput = (ast: MarkdownToJSX.ParserResult | MarkdownToJSX.ParserResult[], state: MarkdownToJSX.State) => JSX.Element;
    export type Rule<ParserOutput = MarkdownToJSX.ParserResult> = {
        match: (source: string, state: MarkdownToJSX.State, prevCapturedString?: string) => RegExpMatchArray;
        order: Priority;
        parse: MarkdownToJSX.Parser<Omit<ParserOutput, 'type'>>;
        render?: (node: ParserOutput, 
        /**
         * Continue rendering AST nodes if applicable.
         */
        render: RuleOutput, state?: MarkdownToJSX.State) => React.ReactChild;
    };
    export type Rules = {
        [K in ParserResult['type']]: K extends RuleType.table ? Rule<Extract<ParserResult, {
            type: K | RuleType.paragraph;
        }>> : Rule<Extract<ParserResult, {
            type: K;
        }>>;
    };
    export type Override = RequireAtLeastOne<{
        component: React.ElementType;
        props: Object;
    }> | React.ElementType;
    export type Overrides = {
        [tag in HTMLTags]?: Override;
    } & {
        [customComponent: string]: Override;
    };
    export type Options = Partial<{
        /**
         * Ultimate control over the output of all rendered JSX.
         */
        createElement: (tag: Parameters<CreateElement>[0], props: JSX.IntrinsicAttributes, ...children: React.ReactChild[]) => React.ReactChild;
        /**
         * Disable the compiler's best-effort transcription of provided raw HTML
         * into JSX-equivalent. This is the functionality that prevents the need to
         * use `dangerouslySetInnerHTML` in React.
         */
        disableParsingRawHTML: boolean;
        /**
         * Forces the compiler to have space between hash sign and the header text which
         * is explicitly stated in the most of the markdown specs.
         * https://github.github.com/gfm/#atx-heading
         * `The opening sequence of # characters must be followed by a space or by the end of line.`
         */
        enforceAtxHeadings: boolean;
        /**
         * Forces the compiler to always output content with a block-level wrapper
         * (`<p>` or any block-level syntax your markdown already contains.)
         */
        forceBlock: boolean;
        /**
         * Forces the compiler to always output content with an inline wrapper (`<span>`)
         */
        forceInline: boolean;
        /**
         * Forces the compiler to wrap results, even if there is only a single
         * child or no children.
         */
        forceWrapper: boolean;
        /**
         * Supply additional HTML entity: unicode replacement mappings.
         *
         * Pass only the inner part of the entity as the key,
         * e.g. `&le;` -> `{ "le": "\u2264" }`
         *
         * By default
         * the following entities are replaced with their unicode equivalents:
         *
         * ```
         * &amp;
         * &apos;
         * &gt;
         * &lt;
         * &nbsp;
         * &quot;
         * ```
         */
        namedCodesToUnicode: {
            [key: string]: string;
        };
        /**
         * Selectively control the output of particular HTML tags as they would be
         * emitted by the compiler.
         */
        overrides: Overrides;
        /**
         * Allows for full control over rendering of particular rules.
         * For example, to implement a LaTeX renderer such as `react-katex`:
         *
         * ```
         * renderRule(next, node, renderChildren, state) {
         *   if (node.type === RuleType.codeBlock && node.lang === 'latex') {
         *     return (
         *       <TeX as="div" key={state.key}>
         *         {String.raw`${node.text}`}
         *       </TeX>
         *     )
         *   }
         *
         *   return next();
         * }
         * ```
         *
         * Thar be dragons obviously, but you can do a lot with this
         * (have fun!) To see how things work internally, check the `render`
         * method in source for a particular rule.
         */
        renderRule: (
        /** Resume normal processing, call this function as a fallback if you are not returning custom JSX. */
        next: () => React.ReactChild, 
        /** the current AST node, use `RuleType` against `node.type` for identification */
        node: ParserResult, 
        /** use as `renderChildren(node.children)` for block nodes */
        renderChildren: RuleOutput, 
        /** contains `key` which should be supplied to the topmost JSX element */
        state: State) => React.ReactChild;
        /**
         * Override normalization of non-URI-safe characters for use in generating
         * HTML IDs for anchor linking purposes.
         */
        slugify: (source: string) => string;
        /**
         * Declare the type of the wrapper to be used when there are multiple
         * children to render. Set to `null` to get an array of children back
         * without any wrapper, or use `React.Fragment` to get a React element
         * that won't show up in the DOM.
         */
        wrapper: React.ElementType | null;
    }>;
    export {};
}
export default Markdown;
