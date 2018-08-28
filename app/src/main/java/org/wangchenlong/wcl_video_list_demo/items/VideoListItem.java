package org.wangchenlong.wcl_video_list_demo.items;

import android.graphics.Rect;
import android.support.annotation.DrawableRes;
import android.view.View;

import com.volokh.danylo.video_player_manager.manager.VideoItem;
import com.volokh.danylo.video_player_manager.manager.VideoPlayerManager;
import com.volokh.danylo.video_player_manager.meta.CurrentItemMetaData;
import com.volokh.danylo.video_player_manager.meta.MetaData;
import com.volokh.danylo.visibility_utils.items.ListItem;

import org.wangchenlong.wcl_video_list_demo.lists.VideoListAdapter;

/**
 * 基本视频项, 实现适配项和列表项
 * <p/>
 * Created by wangchenlong on 16/1/27.
 */
public abstract class VideoListItem implements VideoItem, ListItem {
    private final Rect mCurrentViewRect; // 当前视图的方框
    private final VideoPlayerManager<MetaData> mVideoPlayerManager; // 视频播放管理器
    private final String mTitle; // 标题
    @DrawableRes
    private final int mImageResource; // 图片资源

    // 构造器, 输入视频播放管理器
    public VideoListItem(VideoPlayerManager<MetaData> videoPlayerManager, String title, @DrawableRes int imageResource) {
        mVideoPlayerManager = videoPlayerManager;
        mTitle = title;
        mImageResource = imageResource;
        mCurrentViewRect = new Rect();// 当前视图的方框
    }

    public String getTitle() {  // 视频项的标题
        return mTitle;
    }

    public int getImageResource() { // 视频项的背景
        return mImageResource;
    }


    // 当前视频在屏幕中露出的比例
    @Override
    public int getVisibilityPercents(View view) { // 显示可视的百分比程度
        int percents = 100;
        // 获取当前视频项的位置信息,存入mCurrentViewRect
        view.getLocalVisibleRect(mCurrentViewRect);
        // 获取当前视图的高度
        int height = view.getHeight();

        // 根据当前视频项的位置信息和当前视频的高度,计算视频露出的百分比
        if (viewIsPartiallyHiddenTop()) {
            percents = (height - mCurrentViewRect.top) * 100 / height;
        } else if (viewIsPartiallyHiddenBottom(height)) {
            percents = mCurrentViewRect.bottom * 100 / height;
        }
        // 设置百分比
        setVisibilityPercentsText(view, percents);

        return percents;
    }

    // 启动播放视频
    @Override
    public void setActive(View newActiveView, int newActiveViewPosition) {
        VideoListAdapter.VideoViewHolder viewHolder = (VideoListAdapter.VideoViewHolder) newActiveView.getTag();
        // playNewVideo是抽象类VideoListItem的子类唯一需要实现的方法
        playNewVideo(new CurrentItemMetaData(newActiveViewPosition, newActiveView), viewHolder.getVpvPlayer(), mVideoPlayerManager);
    }

    // 停止播放视频
    @Override
    public void deactivate(View currentView, int position) {
        // 关闭正在播放的视频
        stopPlayback(mVideoPlayerManager);
    }

    // 停止播放
    @Override
    public void stopPlayback(VideoPlayerManager videoPlayerManager) {
        // 关闭全部正在播放的视频
        videoPlayerManager.stopAnyPlayback();
    }

    // 显示百分比
    private void setVisibilityPercentsText(View currentView, int percents) {
        VideoListAdapter.VideoViewHolder vh = (VideoListAdapter.VideoViewHolder) currentView.getTag();
        String percentsText = "可视百分比: " + String.valueOf(percents);
        vh.getTvPercents().setText(percentsText);
    }

    // 顶部出现
    private boolean viewIsPartiallyHiddenTop() {
        return mCurrentViewRect.top > 0;
    }

    // 底部出现
    private boolean viewIsPartiallyHiddenBottom(int height) {
        return mCurrentViewRect.bottom > 0 && mCurrentViewRect.bottom < height;
    }
}