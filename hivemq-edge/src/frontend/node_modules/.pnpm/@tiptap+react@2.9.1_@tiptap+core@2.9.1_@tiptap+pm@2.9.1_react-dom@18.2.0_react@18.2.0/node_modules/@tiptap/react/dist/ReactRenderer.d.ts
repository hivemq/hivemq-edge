import { Editor } from '@tiptap/core';
import React from 'react';
export interface ReactRendererOptions {
    /**
     * The editor instance.
     * @type {Editor}
     */
    editor: Editor;
    /**
     * The props for the component.
     * @type {Record<string, any>}
     * @default {}
     */
    props?: Record<string, any>;
    /**
     * The tag name of the element.
     * @type {string}
     * @default 'div'
     */
    as?: string;
    /**
     * The class name of the element.
     * @type {string}
     * @default ''
     * @example 'foo bar'
     */
    className?: string;
}
type ComponentType<R, P> = React.ComponentClass<P> | React.FunctionComponent<P> | React.ForwardRefExoticComponent<React.PropsWithoutRef<P> & React.RefAttributes<R>>;
/**
 * The ReactRenderer class. It's responsible for rendering React components inside the editor.
 * @example
 * new ReactRenderer(MyComponent, {
 *   editor,
 *   props: {
 *     foo: 'bar',
 *   },
 *   as: 'span',
 * })
*/
export declare class ReactRenderer<R = unknown, P extends Record<string, any> = {}> {
    id: string;
    editor: Editor;
    component: any;
    element: Element;
    props: P;
    reactElement: React.ReactNode;
    ref: R | null;
    /**
     * Immediately creates element and renders the provided React component.
     */
    constructor(component: ComponentType<R, P>, { editor, props, as, className, }: ReactRendererOptions);
    /**
     * Render the React component.
     */
    render(): void;
    /**
     * Re-renders the React component with new props.
     */
    updateProps(props?: Record<string, any>): void;
    /**
     * Destroy the React component.
     */
    destroy(): void;
    /**
     * Update the attributes of the element that holds the React component.
     */
    updateAttributes(attributes: Record<string, string>): void;
}
export {};
//# sourceMappingURL=ReactRenderer.d.ts.map