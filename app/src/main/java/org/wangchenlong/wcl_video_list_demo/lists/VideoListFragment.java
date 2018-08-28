package org.wangchenlong.wcl_video_list_demo.lists;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.volokh.danylo.video_player_manager.manager.PlayerItemChangeListener;
import com.volokh.danylo.video_player_manager.manager.SingleVideoPlayerManager;
import com.volokh.danylo.video_player_manager.manager.VideoPlayerManager;
import com.volokh.danylo.video_player_manager.meta.MetaData;
import com.volokh.danylo.visibility_utils.calculator.DefaultSingleItemCalculatorCallback;
import com.volokh.danylo.visibility_utils.calculator.ListItemsVisibilityCalculator;
import com.volokh.danylo.visibility_utils.calculator.SingleListViewItemActiveCalculator;
import com.volokh.danylo.visibility_utils.scroll_utils.ItemsPositionGetter;
import com.volokh.danylo.visibility_utils.scroll_utils.RecyclerViewItemPositionGetter;

import org.wangchenlong.wcl_video_list_demo.MainActivity;
import org.wangchenlong.wcl_video_list_demo.R;
import org.wangchenlong.wcl_video_list_demo.items.LocalVideoListItem;
import org.wangchenlong.wcl_video_list_demo.items.OnlineVideoListItem;
import org.wangchenlong.wcl_video_list_demo.items.VideoListItem;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 视频列表视图, 可以使用URL和本地文件.
 * <p/>
 * Created by wangchenlong on 16/1/27.
 */
public class VideoListFragment extends Fragment {

    // 视频模式  LOCAL:本地模式  ONLINE:在线模式
    public static final String VIDEO_TYPE_ARG = "me.chunyu.spike.video_list_fragment.video_type_arg";

    // 网络视频地址
    private static final String URL = "http://dn-chunyu.qbox.me/fwb/static/images/home/video/video_aboutCY_A.mp4";

    // 本地资源文件名
    private static final String[] LOCAL_NAMES = new String[]{
            "chunyu-local-1.mp4",
            "chunyu-local-2.mp4",
            "chunyu-local-3.mp4",
            "chunyu-local-4.mp4"
    };

    // 在线资源名
    private static final String ONLINE_NAME = "chunyu-online";

    @Bind(R.id.video_list_rv_list)
    RecyclerView mRecyclerView; // 列表视图

    private final ArrayList<VideoListItem> mList; // 视频项的列表
    private final ListItemsVisibilityCalculator mVisibilityCalculator; // 可视估计器
    private final VideoPlayerManager<MetaData> mVideoPlayerManager;

    private LinearLayoutManager mLayoutManager; // 布局管理器
    // 它充当ListItemsVisibilityCalculator和列表（ListView, RecyclerView）之间的适配器（Adapter）
    private ItemsPositionGetter mItemsPositionGetter; // 位置提取器
    private int mScrollState; // 滑动状态

    // 工厂模式 创建实例, 添加类型
    public static VideoListFragment newInstance(int type) {
        VideoListFragment simpleFragment = new VideoListFragment();
        Bundle args = new Bundle();
        args.putInt(VIDEO_TYPE_ARG, type);
        simpleFragment.setArguments(args);
        return simpleFragment;
    }

    // 构造
    public VideoListFragment() {
        // 视频的列表
        mList = new ArrayList<>();
        // 列表可视计算器,用于判断列表中的视频那个是最佳播放项
        mVisibilityCalculator = new SingleListViewItemActiveCalculator(new DefaultSingleItemCalculatorCallback(), mList);
        // 视频播放管理器,用于管理视频的播放器
        mVideoPlayerManager = new SingleVideoPlayerManager(new PlayerItemChangeListener() {
            @Override
            public void onPlayerItemChanged(MetaData metaData) {
            }
        });
        // 暂停滚动状态
        // mScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_video_list, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 第一部分 设置数据源,默认本地数据源
        initLocalVideoList();
        Bundle args = getArguments();
        if (args != null && args.getInt(VIDEO_TYPE_ARG) == MainActivity.ONLINE) {
            // 使用线上数据源
            initOnlineVideoList();
        }

        // 第二部分 设置RecyclerView
        mRecyclerView.setHasFixedSize(true);// setHasFixedSize将RecyclerView的尺寸设置为固定,避免重绘
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);// 将RecyclerView的滚动方向设置为竖直
        VideoListAdapter adapter = new VideoListAdapter(mList);
        mRecyclerView.setAdapter(adapter);


        // 第三部分 设置滚动播放
        autoViewPlaySetting();
    }

    /**
     * 滚动播放设置  //这里是文档上默认的写法，直接复制下来。
     *
     * 其中VisibilityCalculator.onScrollStateIdle又调用了方法calculateMostVisibleItem.
     * 用来计算滑动状态改变时的最大可见度的item. 这个方法的计算方法是这样的：
     * 在滚动的过程中，依次计算页面上每个item显示出来的高度。
     * 当滚动状态为空闲时,此时最后一个计算得出的可见度最大的item就是当前可见度最大的item
     * 而onScroll方法是处理item滚出屏幕后的计算,用于发现新的活动item
     */
    private void autoViewPlaySetting() {
        // 通过RecyclerViewItemPositionGetter的实例封装LinearLayoutManager和RecyclerView, 提供获取列表项信息的接口
        mItemsPositionGetter = new RecyclerViewItemPositionGetter(mLayoutManager, mRecyclerView);

        // 设置RecyclerView的滚动监听
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            //当recycleView的滑动状态改变时回调
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int scrollState) {
                mScrollState = scrollState;
                if (scrollState == RecyclerView.SCROLL_STATE_IDLE && !mList.isEmpty()) {
                    // SCROLL_STATE_IDLE:当前的recycleView不滚动了,整个滚动事件结束，只会触发一次
                    mVisibilityCalculator.onScrollStateIdle(
                            mItemsPositionGetter,
                            mLayoutManager.findFirstVisibleItemPosition(),
                            mLayoutManager.findLastVisibleItemPosition());
                }
            }

            //当RecycleView滑动之后被回调
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!mList.isEmpty()) {
                    mVisibilityCalculator.onScroll(
                            mItemsPositionGetter,
                            mLayoutManager.findFirstVisibleItemPosition(),
                            mLayoutManager.findLastVisibleItemPosition() -
                                    mLayoutManager.findFirstVisibleItemPosition() + 1,
                            mScrollState);
                }
            }
        });
    }



    //文档上的默认实现，复制下来
    //onResume()中,屏幕亮起时,在mRecyclerView线程中,可视计算器启动对View的可见度的计算,播放露出最大比例的视频
    @Override
    public void onResume() {
        super.onResume();
        if (!mList.isEmpty()) {
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    // 判断一些滚动状态
                    mVisibilityCalculator.onScrollStateIdle(
                            mItemsPositionGetter,
                            mLayoutManager.findFirstVisibleItemPosition(),
                            mLayoutManager.findLastVisibleItemPosition());
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    // 页面关闭时,重置视频播放管理,暂停全部播放,准备下次启动
    @Override
    public void onStop() {
        super.onStop();
        mVideoPlayerManager.resetMediaPlayer(); // 页面不显示时, 释放播放器
    }

    // 初始化本地视频
    private void initLocalVideoList() {
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[0], R.drawable.cover, getFile(LOCAL_NAMES[0])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[1], R.drawable.cover, getFile(LOCAL_NAMES[1])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[2], R.drawable.cover, getFile(LOCAL_NAMES[2])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[3], R.drawable.cover, getFile(LOCAL_NAMES[3])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[0], R.drawable.cover, getFile(LOCAL_NAMES[0])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[1], R.drawable.cover, getFile(LOCAL_NAMES[1])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[2], R.drawable.cover, getFile(LOCAL_NAMES[2])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[3], R.drawable.cover, getFile(LOCAL_NAMES[3])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[0], R.drawable.cover, getFile(LOCAL_NAMES[0])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[1], R.drawable.cover, getFile(LOCAL_NAMES[1])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[2], R.drawable.cover, getFile(LOCAL_NAMES[2])));
        mList.add(new LocalVideoListItem(mVideoPlayerManager, LOCAL_NAMES[3], R.drawable.cover, getFile(LOCAL_NAMES[3])));
    }

    // 初始化在线视频, 需要缓冲
    private void initOnlineVideoList() {
        final int count = 10;
        for (int i = 0; i < count; ++i) {
            mList.add(new OnlineVideoListItem(mVideoPlayerManager, ONLINE_NAME, R.drawable.cover, URL));
        }
    }

    // 获取资源文件
    private AssetFileDescriptor getFile(String name) {
        try {
            return getActivity().getAssets().openFd(name);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
