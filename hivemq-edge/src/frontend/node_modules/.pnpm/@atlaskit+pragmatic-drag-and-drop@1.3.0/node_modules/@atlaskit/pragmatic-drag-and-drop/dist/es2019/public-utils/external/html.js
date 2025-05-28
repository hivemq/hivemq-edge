import { HTMLMediaType } from '../../util/media-types/html-media-type';
export function containsHTML({
  source
}) {
  return source.types.includes(HTMLMediaType);
}
export function getHTML({
  source
}) {
  return source.getStringData(HTMLMediaType);
}