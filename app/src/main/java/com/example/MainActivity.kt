package com.example

import android.annotation.SuppressLint
import android.arch.paging.PagedList
import android.arch.paging.PagedListAdapter
import android.arch.paging.PositionalDataSource
import android.arch.paging.TiledDataSource
import android.content.Context
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.concurrent.Executor
import java.util.stream.Collector
import java.util.stream.Collectors


class MainActivity : AppCompatActivity() {

    private lateinit var pageList : PagedList<DataBean>
    private lateinit var dataSource : MyDataSource
    private lateinit var recyclerView: RecyclerView
    private lateinit var pagedListAdapter: MyAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataSource = MyDataSource()

        val config = PagedList.Config.Builder()
                .setPageSize(3)
                .setPrefetchDistance(10)
                .setEnablePlaceholders(false)
                .build()

        pageList = PagedList.Builder(dataSource, config)
                .setNotifyExecutor(MainThreadTask())
                .setFetchExecutor(BackgroundThreadTask())
                .build()


        recyclerView = findViewById(R.id.recyclerview)

        layoutManager = LinearLayoutManager(this).apply { orientation=LinearLayoutManager.VERTICAL }
        recyclerView.layoutManager = layoutManager

        pagedListAdapter = MyAdapter(this)

        recyclerView.adapter = pagedListAdapter

        // setlist
        pagedListAdapter.submitList(pageList)
        Log.e("pagelist", pageList.size.toString())

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var lastPositon=-1
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                lastPositon = layoutManager.findLastVisibleItemPosition()
                pageList.loadAround(lastPositon)
            }
        })


    }


    inner class MyAdapter(var context: Context) : PagedListAdapter<DataBean, MyViewHolder>(diffCallback) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, null))
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val data = pageList[position]
            data?.let {
                holder.tv1.text = position.toString()
                holder.tv2.text = it.content
            }
        }

    }
}

class BackgroundThreadTask : Executor {
    init {
        this.execute{
            Log.d("BackgroundThreadTask", "run")
        }
    }
    override fun execute(command: Runnable?) {
        command?.run()
    }

}

class MainThreadTask : Executor {
    init {
        execute {
            Log.d("MainThreadTask", "run")
        }
    }
    override fun execute(command: Runnable?) {
        command?.run()
    }
}


class MyDataSource : PositionalDataSource<DataBean>() {
    private fun computeCount(): Int {
        // actual count code here
        return 2
    }

    private fun loadRangeInternal(start: Int, count : Int) : MutableList<DataBean> {
        return loadData(start, count)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<DataBean>) {
        Log.e("loadRangeParams", "startPosition:${params.startPosition}; loadSize:${params.loadSize}")
        callback.onResult(loadRangeInternal(params.startPosition, params.loadSize))

    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<DataBean>) {
        val count = computeCount()
        val position = computeInitialLoadPosition(params, count)
        val loadSize = computeInitialLoadSize(params, position, count)
        Log.e("loadInitial", "position: $position; loadSize: $loadSize")
        callback.onResult(loadRangeInternal(position, loadSize), position, count)
    }

}

fun loadData(startPosition: Int, count: Int) : MutableList<DataBean> {
    if (startPosition > 100) {
        return mutableListOf()
    }
    Log.e("loaddata=>", "startpo:$startPosition;count:$count")
    val list = mutableListOf<DataBean>()
    var i = 0
    while (i < count) {
        var id = startPosition + i
        val data = DataBean(id, "xx@$id")
        list.add(data)
        i++
    }
    return list
}



class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var tv1: TextView = itemView.findViewById(android.R.id.text1)
    var tv2: TextView = itemView.findViewById(android.R.id.text2)

    init {
        tv1.setTextColor(Color.RED)
        tv2.setTextColor(Color.BLUE)
    }
}


val diffCallback = object : DiffUtil.ItemCallback<DataBean>() {
    override fun areItemsTheSame(oldItem: DataBean?, newItem: DataBean?): Boolean {
        Log.d("DiffCallback", "areItemsTheSame")
        if (oldItem == null) {
            return false
        }
        if (newItem == null) {
            return false
        }

        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DataBean?, newItem: DataBean?): Boolean {

        Log.d("DiffCallback", "areContentsTheSame")
        if (oldItem == null) {
            return false
        }

        if (newItem == null) {
            return false
        }
        return TextUtils.equals(oldItem.content, newItem.content);
    }

}


data class DataBean(
        val id : Int,
        val content: String
)