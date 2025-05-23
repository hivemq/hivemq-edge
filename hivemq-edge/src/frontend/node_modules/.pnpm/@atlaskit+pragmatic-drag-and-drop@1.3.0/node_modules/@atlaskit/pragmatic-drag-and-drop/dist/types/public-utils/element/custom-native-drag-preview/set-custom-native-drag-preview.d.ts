import { type ElementEventPayloadMap } from '../../../adapter/element-adapter';
import type { GetOffsetFn } from './types';
/** A function to remove the element that has been added to the `container`.
 * @example () => ReactDOM.unmountComponentAtNode(container)
 */
type CleanupFn = () => void;
/** A function that will render a preview element into a `container` `HTMLElement` */
type RenderFn = ({ container, }: {
    /** The `HTMLElement` that you need to render your preview element into.
  `container` will be appended to the `document.body` and will be removed
  after your `CleanupFn` is called
  */
    container: HTMLElement;
}) => CleanupFn | void;
/** This function provides the ability to mount an element for it to be used as the native drag preview
 *
 * @example
 * draggable({
 *  onGenerateDragPreview: ({ nativeSetDragImage }) => {
 *    setCustomNativeDragPreview({
 *      render: ({ container }) => {
 *        ReactDOM.render(<Preview item={item} />, container);
 *        return () => ReactDOM.unmountComponentAtNode(container);
 *      },
 *      nativeSetDragImage,
 *    });
 *    },
 * });
 */
export declare function setCustomNativeDragPreview({ render, nativeSetDragImage, getOffset, }: {
    getOffset?: GetOffsetFn;
    render: RenderFn;
    nativeSetDragImage: ElementEventPayloadMap['onGenerateDragPreview']['nativeSetDragImage'];
}): void;
export {};
