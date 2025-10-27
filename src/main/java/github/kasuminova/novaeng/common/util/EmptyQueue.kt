package github.kasuminova.novaeng.common.util

import it.unimi.dsi.fastutil.objects.ObjectLists
import java.util.Queue

class EmptyQueue<E> : Queue<E> {

    companion object {
        private val empty: EmptyQueue<*> = EmptyQueue<Any>()

        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): EmptyQueue<T> {
            return empty as EmptyQueue<T>
        }
    }

    private constructor()

    override fun add(e: E?): Boolean {
        return false
    }

    override fun offer(e: E?): Boolean {
        return false
    }

    override fun remove(): E? {
        return null
    }

    override fun poll(): E? {
        return null
    }

    override fun element(): E? {
        return null
    }

    override fun peek(): E? {
        return null
    }

    override fun iterator(): MutableIterator<E?> {
        return ObjectLists.emptyList<E>().iterator()
    }

    override fun remove(element: E?): Boolean {
        return false
    }

    override fun addAll(elements: Collection<E?>): Boolean {
        return false
    }

    override fun removeAll(elements: Collection<E?>): Boolean {
        return false
    }

    override fun retainAll(elements: Collection<E?>): Boolean {
        return false
    }

    override fun clear() {

    }

    override val size: Int
        get() = 0

    override fun isEmpty(): Boolean {
        return true
    }

    override fun contains(element: E?): Boolean {
        return false
    }

    override fun containsAll(elements: Collection<E?>): Boolean {
        return false
    }
}