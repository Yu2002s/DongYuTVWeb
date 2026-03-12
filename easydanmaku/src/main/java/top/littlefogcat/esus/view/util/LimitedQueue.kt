package top.littlefogcat.esus.view.util

import java.lang.StringBuilder

/**
 * A queue with a maximum length limited.
 *
 * Use [offer] to add an element to the last of the queue.
 * Use [peek] to get the first element from the queue, or null if empty.
 * Use [poll] to get and remove the first element from the queue, or null if empty.
 * Use [size] to get how much elements current in the queue.
 *
 * @param capacity The maximum length of the queue. Must be a non-negative integer.
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
@Suppress("UNCHECKED_CAST")
open class LimitedQueue<E>(val capacity: Int) {
    private val elements: Array<Any?> = arrayOfNulls(capacity)
    var size = 0
        private set
    var first = 0
        private set
    var last = 0
        private set

    init {
        assert(capacity > 0)
    }

    fun offer(element: E) {
        if (size == 0) {
            elements[0] = element
            first = 0
            last = 0
            size = 1
        } else {
            val next = (last + 1) % capacity
            elements[next] = element
            last = next
            if (first == next) {
                first = (first + 1) % capacity
            }
            if (size < capacity) {
                size++
            }
        }
    }

    fun peek(): Any? {
        return elements[last]
    }

    fun poll(): E? {
        if (size == 0) {
            return null
        }
        val delete = elements[last]
        first = (first + 1) % capacity
        size++
        return delete as E?
    }

    fun first(): E? {
        return elements[first] as E?
    }

    fun last(): E? {
        return elements[last] as E?
    }

    fun clear() {
        size = 0
        first = 0
        last = 0
    }

    override fun toString(): String {
        if (size == 0) {
            return "[]"
        }
        val sb = StringBuilder("[")
        if (first <= last) {
            for (i in first until last) {
                sb.append(elements[i]).append(", ")
            }
        } else {
            for (i in first until capacity) {
                sb.append(elements[i]).append(", ")
            }
            for (i in 0 until last) {
                sb.append(elements[i]).append(", ")
            }
        }
        sb.append(elements[last]).append("]")
        return sb.toString()
    }


}