package com.luowei.asyncadapater

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.chad.library.adapter.base.BaseViewHolder
import com.luowei.dataloadadapterlibrary.DataLoader
import com.unistrong.luowei.commlib.Log

/**
 * Created by luowei on 2017/12/23.
 */
class Adapter : RecyclerView.Adapter<BaseViewHolder>(), DataLoader.Loader<String> {

    private val dataLoader = DataLoader<String, BaseViewHolder>(this)


    override fun getItemCount(): Int {
        return 10000
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val data = dataLoader.getItem(position)
        if (data != null) {
            convert(holder, data)
        } else {
            convert(holder, "position= $position")
        }
    }


    override fun loadData(start: Int, end: Int): Array<String> {
        Log.d("start=$start, end=$end start")
        Thread.sleep(2000)
        val list = (start until end).map { it.toString() }
        Log.d("start=$start, end=$end finish")
        return list.toTypedArray()
    }

    private fun convert(helper: BaseViewHolder, item: String) {
        helper.setText(R.id.textView, item)
    }


}
