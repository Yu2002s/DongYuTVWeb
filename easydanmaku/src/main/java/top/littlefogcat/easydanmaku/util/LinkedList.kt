package top.littlefogcat.easydanmaku.util

import java.lang.Exception
import kotlin.collections.AbstractList

@Suppress("UNCHECKED_CAST")
open class LinkedList<E : LinkedList.Node>() : AbstractList<E>() {
    var first: E? = null
        private set
    var last: E? = null
        private set
    override var size: Int = 0
        protected set

    constructor(vararg elements: E) : this() {
        for (element in elements) {
            addLast(element)
        }
    }

    /* base functions */

    fun addLast(element: E) {
        if (size == 0) {
            first = element
            last = element
        } else {
            last!!.next = element
            element.prev = last
            last = element
        }
        size++
    }

    fun addFirst(element: E) {
        if (size == 0) {
            first = element
            last = element
        } else {
            first!!.prev = element
            element.next = first
            first = element
        }
        size++
    }

    fun removeLast(): E {
        if (size == 0) {
            throw NoSuchElementException()
        }
        val e = last!!
        if (size == 1) {
            first = null
            last = null
        } else {
            last = e.prev as E
            last!!.next = null
            e.prev = null
        }
        size--
        return e
    }

    fun removeFirst(): E {
        if (size == 0) {
            throw NoSuchElementException()
        }
        val e = first!!
        if (size == 1) {
            first = null
            last = null
        } else {
            first = e.next as E
            first!!.prev = null
            e.next = null
        }
        size--
        return e
    }

    fun insert(prev: E?, element: E) {
        if (prev == null) {
            addFirst(element)
        } else if (prev == last) {
            addLast(element)
        } else {
            val next = prev.next
            element.prev = prev
            element.next = next
            prev.next = element
            next?.prev = element
            size++
        }
    }

    fun remove(element: E): E {
        // assert element is in list
        if (element == first) {
            return removeFirst()
        }
        if (element == last) {
            return removeLast()
        }
        val prev = element.prev
        val next = element.next
        if (prev != null) {
            prev.next = next
        }
        if (next != null) {
            next.prev = prev
        }
        size--
        return element
    }

    fun replace(oldElement: E, newElement: E) {
        newElement.prev = oldElement.prev
        newElement.next = oldElement.next
        if (oldElement == first) {
            first = newElement
        } else {
            oldElement.prev!!.next = newElement
        }
        if (oldElement == last) {
            last = newElement
        } else {
            oldElement.next!!.prev = newElement
        }
    }

    fun clear() {
        first = null
        last = null
        size = 0
    }

    /* for Queue */

    fun poll(): E? {
        if (size == 0) {
            return null
        }
        return removeFirst()
    }

    fun offer(element: E) {
        addLast(element)
    }

    /* for Stack */

    fun pop(): E? {
        if (size == 0) {
            return null
        }
        return removeLast()
    }

    fun push(element: E) {
        addLast(element)
    }

    interface Node {
        var prev: Node?
        var next: Node?
    }

    /* Override */

    override fun iterator(): Iterator<E> = Itr()

    private inner class Itr(private var index: Int = 0) : MutableListIterator<E> {
        private var e: E? = get(index)
        private var ret: E? = null

        override fun hasNext() = index < size
        override fun hasPrevious() = index > 0
        override fun nextIndex() = index
        override fun previousIndex() = index - 1

        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            ret = e as E
            e = e!!.next as E?
            index++
            return ret!!
        }

        override fun previous(): E {
            if (!hasPrevious()) throw NoSuchElementException()
            e = e!!.prev as E?
            index--
            ret = e
            return ret!!
        }

        override fun remove() {
            checkNotNull(ret)
            remove(ret!!)
            ret = null
        }

        override fun set(element: E) {
            checkNotNull(ret)
            replace(ret!!, element)
            ret = element
        }

        override fun add(element: E) {
            if (!hasPrevious()) {
                insert(null, element)
            } else insert(previous(), element)
            ret = null
            index++
        }
    }

    override fun get(index: Int): E {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException()
        }
        var p: E = first!!
        for (i in 0 until index) {
            p = p.next as E
        }
        return p
    }

    override fun contains(element: E): Boolean {
        var p = first
        while (p != null) {
            if (p == element) {
                return true
            }
            p = p.next as E?
        }
        return false
    }

    override fun indexOf(element: E): Int {
        var p = first
        var i = 0
        while (p != null) {
            if (p == element) {
                return i
            }
            p = p.next as E?
            i++
        }
        return -1
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun listIterator(): ListIterator<E> = Itr()

    override fun listIterator(index: Int): ListIterator<E> = Itr(index)

    override fun toString(): String {
        if (size == 0) {
            return "[]"
        }
        var i = 1
        val sb = StringBuilder("[").append(first)
        var p = first!!.next
        while (i++ < size) {
            sb.append(", ").append(p)
            p = p!!.next
        }
        sb.append("]")
        return sb.toString()
    }
}