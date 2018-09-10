package gcg.testproject.activity.RefreshAndLoadMore;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import gcg.testproject.R;
import gcg.testproject.activity.http.Xutils;
import gcg.testproject.base.BaseActivity;
import gcg.testproject.bean.DaJiaKanListBean;
import gcg.testproject.common.Constants;
import gcg.testproject.utils.LogUtils;
import gcg.testproject.utils.NetUtils;
import gcg.testproject.utils.ToastUtils;
import gcg.testproject.widget.RefreshLayout;

/**
 * 上拉加载下拉刷新
 *
 * @ClassName:RefreshAndLoadMoreActivity
 * @PackageName:gcg.testproject.activity.RefreshAndLoadMore
 * @Create On 2018/1/26   14:15
 * @Site:http://www.handongkeji.com
 * @author:gongchenghao
 * @Copyrights 2018/1/26 宫成浩 All rights reserved.
 */

public class RefreshAndLoadMoreActivity extends BaseActivity {

    @Bind(R.id.lv_ji_lu)
    ListView listView;
    @Bind(R.id.swipe_layout)
    RefreshLayout swipe_layout;
    @Bind(R.id.iv_zanwu)
    ImageView ivZanWu;

    private int currentpage =1;
    private int pageSize = 5;
    private boolean isLoading = false; //用来判断是在  加载更多  还是  下拉刷新
    private List<DaJiaKanListBean.DataEntity.PerlookEntity> list = new ArrayList<>();
    private MyListViewAdapter myAdapter = new MyListViewAdapter();
    private int totalSize = 15; //这个接口的总数据量，用来判断是否还有更多数据


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refresh_and_load_more);
        ButterKnife.bind(this);

        setSwip(); //设置一进界面就显示刷新的效果
        setOnRefresh();//设置下拉刷新
        setOnLoadMore();//设置上拉加载
    }

    private void setSwip() {
        swipe_layout.post(new Thread(new Runnable() {

            @Override
            public void run() {
                boolean connected = NetUtils.isConnected(RefreshAndLoadMoreActivity.this);
                if (connected) {
                    getDataFromServer();
                    swipe_layout.setRefreshing(true);
                } else {
                    ToastUtils.showShort(RefreshAndLoadMoreActivity.this, "请检查网络...");
                    swipe_layout.setVisibility(View.GONE);
                    ivZanWu.setVisibility(View.VISIBLE);
                }
            }
        }));

    }

    //下拉刷新
    private void setOnRefresh() {
        // 设置下拉刷新监听器
        swipe_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipe_layout.removeNoMoreFootView(); //================== 注意：必须调用
                currentpage = 1;
                getDataFromServer();
            }
        });

    }

    //上拉加载
    private void setOnLoadMore() {
        //当不是在下拉刷新的时候才能执行上拉加载操作
        swipe_layout.setOnLoadListener(new RefreshLayout.OnLoadListener() {
            @Override
            public void onLoad() {
                currentpage += 1;
                isLoading = true;
                //防止用户在加载更多正在执行的时候，进行下拉刷新操作
                swipe_layout.setEnabled(false);
                getDataFromServer();
            }
        });
    }

    private void getDataFromServer() {
        final HashMap<String, String> params = new HashMap<>();
        params.put("type", "1");
        params.put("currentPage", currentpage + "");
        params.put("pageSize", pageSize + "");
        Xutils.getInstance().post(Constants.URL_DA_JIA_LIE_BIAO, params, new Xutils.XCallBack() {
            @Override
            public void onResponse(String json) {
                swipe_layout.setRefreshLayout(swipe_layout); //获取完数据后，设置refreshlayout的状态 ================== 注意：必须调用
                if (json != null) {
                    Gson gson = new Gson();
                    DaJiaKanListBean daJiaKanListbean = gson.fromJson(json, DaJiaKanListBean.class);
                    int status = daJiaKanListbean.getStatus();
                    if (status == 1) {
                        if (isLoading == true) { //如果是上拉加载操作
                            isLoading = false;
                            if (daJiaKanListbean.getData().getPerlook().size() != 0) {
                                list.addAll(daJiaKanListbean.getData().getPerlook());
                                myAdapter.notifyDataSetChanged();
                            }
                            if (list.size() == totalSize)
                            {
                                LogUtils.i("list.size() == totalSize");
                                swipe_layout.hasMore(false); //================== 注意：必须调用
                            }

                        } else { //如果是下拉刷新操作
                            list = daJiaKanListbean.getData().getPerlook();
                            swipe_layout.setFirstLoadFootView(myAdapter); //在第一次加载时进行以下设置 ================== 注意：必须调用
                        }
                        //设置未获取到数据和获取到数据后的界面显示效果
                        swipe_layout.setGetNoDate(list.size() == 0 ? false:true,swipe_layout,ivZanWu); //================== 注意：必须调用
                    } else {
                        ToastUtils.showShort(RefreshAndLoadMoreActivity.this, "服务器错误");
                    }
                } else {
                    ToastUtils.showShort(RefreshAndLoadMoreActivity.this, "服务器错误");
                }
            }
        });
    }

    class MyListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(RefreshAndLoadMoreActivity.this).inflate(R.layout.item_dajiakan_shou_ye, null, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            }
            holder = (ViewHolder) convertView.getTag();

            final DaJiaKanListBean.DataEntity.PerlookEntity dataEntity = list.get(position);
            int lookid = dataEntity.getLookid();
            holder.tvDate.setText("第"+(position+1)+"条数据,LookID:"+lookid);
            return convertView;
        }
    }
    static class ViewHolder {
        @Bind(R.id.tv_date)
        TextView tvDate;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

}
