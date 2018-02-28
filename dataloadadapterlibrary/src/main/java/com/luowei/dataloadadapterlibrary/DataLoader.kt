package com.luowei.dataloadadapterlibrary

import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.util.LruCache
import com.unistrong.luowei.commlib.Log
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by luowei on 2017/12/23.
 *
 */
class DataLoader<out K, VH : RecyclerView.ViewHolder>(private val adapter: WeakReference<RecyclerView.Adapter<VH>>) : Runnable {
    override fun run() {
        Log.d("background run is start...")
        while (isRun) {
            if (task.size == 0) synchronized(objecz, { objecz.wait(60000) })
            if (work()) break
        }
        Log.d("background run is dead !!")
        isRun = false
    }

    private fun work(): Boolean {
        val position = task.peek()
        position ?: return true
        Log.d("load [$position, ${position + SECTION}]")
        val loadData = (adapter.get() as? Loader<K>)?.loadData(position, position + SECTION)
        Log.d("load finish")
        loadData ?: return true
        loadData.forEachIndexed { index, s -> itemData.put(index + position, s) }

        //            Log.d(itemData.snapshot().entries.sortedBy { it.key }.map { "[${it.key},${it.value}]" }.toString())
        //            Log.d("firstOrNull=${itemData.snapshot().keys.firstOrNull() ?: 0}," +
        //                    "lastOrNull=${itemData.snapshot().keys.lastOrNull() ?: 0}")
        section[0] = itemData.snapshot().keys.first()
        section[1] = itemData.snapshot().keys.last()
        handler.post {
            Log.d("notify data change [$position, ${loadData.size}]")
            adapter.get()?.notifyItemRangeChanged(position, loadData.size)
        }
        task.poll()
        return false
    }

    companion object {
        private val SECTION = 100
        private val MAX_SIZE: Int = 500
    }

    private val objecz = Object()
    private val itemData = LruCache<Int, K>(MAX_SIZE)
    private val section = intArrayOf(0, 0)
    private val handler = Handler()
    private val task = LinkedList<Int>()
    private var isRun = false
    private var lastPosition = 0

    interface Loader<K> {
        fun loadData(start: Int, end: Int): Array<K>
    }

    fun clear() {
        itemData.trimToSize(-1)
    }

    fun delete(key: Int) {
        itemData.remove(key)
    }

    fun getItem(postion: Int, notNull: Boolean = false): K? {
        starBackgroundTask()
        var data = itemData[postion]
        val down = postion >= lastPosition
        lastPosition = postion

        if (data == null) {
            if (offerToTask(postion, down, false)) {
                if (notNull) {
                    work()
                    data = getItem(postion, notNull)
                } else {
                    synchronized(objecz, { objecz.notify() })
                }
            }
        } /*else {
            val itemPos = if (down) {
                section[1]
            } else {
                section[0] - SECTION
            }
            if (Math.abs(itemPos - pos) < SECTION / 2 && itemPos >= 0) {
//                Log.d("prev load $itemPos")
                offerToTask(itemPos, down, true)
            }
        }*/
        return data
    }

    private fun offerToTask(position: Int, down: Boolean, b: Boolean = false): Boolean {
        val offer = (0 until task.size).none { position in task[it] until task[it] + SECTION }
        if (offer && b) Log.d("prev load $position")
        val pos = if (!down) Math.max(position - SECTION + 1, 0) else position
//        if (task.size == 0 || pos !in task.last until task.last + SECTION) {
        if (offer) {
            Log.d("load offer $pos")
            task.offer(pos)
            if (task.size > 3) task.poll()
            return true
        }
        return false
    }

    private fun starBackgroundTask() {
        if (!isRun) {
            isRun = true
            Thread(this).start()
        }
    }


}