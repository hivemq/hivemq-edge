export function containsFiles({
  source
}) {
  return source.types.includes('Files');
}

/** Obtain an array of the dragged `File`s */
export function getFiles({
  source
}) {
  return source.items
  // unlike other media types, for files:
  // item.kind is 'file'
  // item.type is the type of file eg 'image/jpg'
  // for other media types, item.type is the mime format
  .filter(item => item.kind === 'file').map(item => item.getAsFile()).filter(file => file != null);
}