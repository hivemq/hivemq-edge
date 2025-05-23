module.exports = function (filter, topic, handleSharedSubscription = false) {
  const filterArray = filter.split('/')

  // handle shared subscrition
  if (handleSharedSubscription && filterArray.length > 2 && filter.startsWith('$share/')) {
    filterArray.splice(0, 2)
  }

  const length = filterArray.length
  const topicArray = topic.split('/')

  for (let i = 0; i < length; ++i) {
    const left = filterArray[i]
    const right = topicArray[i]
    if (left === '#') return topicArray.length >= length - 1
    if (left !== '+' && left !== right) return false
  }

  return length === topicArray.length
}
