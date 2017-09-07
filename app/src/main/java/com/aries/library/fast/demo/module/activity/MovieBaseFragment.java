package com.aries.library.fast.demo.module.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.aries.library.fast.demo.R;
import com.aries.library.fast.demo.adapter.SubjectMovieAdapter;
import com.aries.library.fast.demo.base.BaseMovieEntity;
import com.aries.library.fast.demo.base.BaseRefreshLoadFragment;
import com.aries.library.fast.demo.constant.MovieConstant;
import com.aries.library.fast.demo.entity.SubjectsEntity;
import com.aries.library.fast.demo.module.WebViewActivity;
import com.aries.library.fast.demo.retrofit.repository.ApiRepository;
import com.aries.library.fast.manager.LoggerManager;
import com.aries.library.fast.retrofit.FastError;
import com.aries.library.fast.retrofit.FastObserver;
import com.aries.library.fast.util.ToastUtil;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.trello.rxlifecycle2.android.FragmentEvent;

/**
 * Created: AriesHoo on 2017/8/25 17:03
 * Function: 电影列表
 * Desc:
 */
public class MovieBaseFragment extends BaseRefreshLoadFragment<SubjectsEntity> {

    private BaseQuickAdapter mAdapter;
    private int mType = 0;
    private ImageView imageViewTop;

    public static MovieBaseFragment newInstance(int type) {
        Bundle args = new Bundle();
        MovieBaseFragment fragment = new MovieBaseFragment();
        args.putInt("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void beforeSetContentView() {
        super.beforeSetContentView();
        mType = getArguments().getInt("type");
    }

    @Override
    public BaseQuickAdapter<SubjectsEntity, BaseViewHolder> getAdapter() {
        mAdapter = new SubjectMovieAdapter(mType == MovieConstant.MOVIE_TOP);
        return mAdapter;
    }

    @Override
    public int getContentLayout() {
        return R.layout.fast_layout_multi_status_refresh_recycler;
    }

    @Override
    public void initView(Bundle savedInstanceState) {
        mContentView.setBackgroundResource(R.color.colorBackground);
//        setBackToTop(true);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                    int firstVisibleItemPosition = linearManager.findFirstVisibleItemPosition();
                    if (firstVisibleItemPosition > 10) {
                        setBackToTop(true);
                    } else {
                        setBackToTop(false);
                    }
                }
            }
        });
    }

    /**
     * 控制回到顶部
     *
     * @param enable
     */
    private void setBackToTop(boolean enable) {
        if (imageViewTop == null) {
            imageViewTop = new ImageView(mContext);
            imageViewTop.setImageResource(R.drawable.ic_top);
            mEasyStatusView.addView(imageViewTop);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) imageViewTop.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);//与父容器的左侧对齐
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);//与父容器的上侧对齐
            lp.rightMargin = 30;
            lp.bottomMargin = 30;
            imageViewTop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mRecyclerView.scrollToPosition(0);
                }
            });
        }
        imageViewTop.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void loadData(int page) {
        DEFAULT_PAGE_SIZE = 15;//接口最大支持单页100
        ApiRepository.getInstance().getBaseMovie(mType, page * DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE)
                .compose(bindUntilEvent(FragmentEvent.DESTROY))
                .subscribe(new FastObserver<BaseMovieEntity>() {
                    @Override
                    public void _onNext(BaseMovieEntity entity) {
                        mRefreshLayout.finishRefresh();
                        mAdapter.loadMoreComplete();
                        if (entity == null || entity.subjects == null || entity.subjects.size() == 0) {
                            if (page == 0) {
                                mEasyStatusView.empty();
                            } else {
                                mAdapter.loadMoreEnd();
                            }
                            return;
                        }
                        mEasyStatusView.content();
                        if (mRefreshLayout.isRefreshing())
                            mAdapter.setNewData(null);
                        mAdapter.addData(entity.subjects);
                        LoggerManager.d("ApiRepository", "title:" + entity.title + ";start:" + entity.start + ";count:" + entity.count + ";total:" + entity.total);
                        if (!entity.hasMore()) {
                            mAdapter.loadMoreEnd(page == 0);
                        }
                    }

                    @Override
                    public void _onError(int errorRes, int errorCode, Throwable e) {
                        mRefreshLayout.finishRefresh();
                        mAdapter.loadMoreComplete();
                        LoggerManager.e("error:" + getString(errorRes) + ";errorCode:" + errorCode + ";Throwable:" + e.getMessage());
                        if (page == 0) {
                            mEasyStatusView.error();
                            if (errorCode == FastError.EXCEPTION_ACCOUNTS) {

                            } else if (errorCode == FastError.EXCEPTION_JSON_SYNTAX) {

                            } else if (errorCode == FastError.EXCEPTION_OTHER_ERROR) {

                            } else if (errorCode == FastError.EXCEPTION_TIME_OUT) {

                            } else {
                                mEasyStatusView.noNet();
                            }
                        } else {
                            ToastUtil.show(errorRes);
                        }
                    }
                });
    }

    @Override
    public void onItemClicked(BaseQuickAdapter<SubjectsEntity, BaseViewHolder> adapter, View view, int position) {
        super.onItemClicked(adapter, view, position);
        WebViewActivity.start(mContext, adapter.getItem(position).alt);
    }

    @Override
    protected void onVisibleChanged(boolean isVisibleToUser) {
        super.onVisibleChanged(isVisibleToUser);
        LoggerManager.d(TAG, "isVisibleToUser:" + isVisibleToUser + ";type:" + mType);
    }
}