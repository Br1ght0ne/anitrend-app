package com.mxt.anitrend.view.activity.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.DialogAction;
import com.mxt.anitrend.R;
import com.mxt.anitrend.adapter.pager.detail.StaffPageAdapter;
import com.mxt.anitrend.base.custom.activity.ActivityBase;
import com.mxt.anitrend.base.custom.view.widget.FavouriteToolbarWidget;
import com.mxt.anitrend.model.entity.base.StaffBase;
import com.mxt.anitrend.presenter.base.BasePresenter;
import com.mxt.anitrend.util.CompatUtil;
import com.mxt.anitrend.util.DialogUtil;
import com.mxt.anitrend.util.KeyUtil;
import com.mxt.anitrend.util.NotifyUtil;
import com.mxt.anitrend.util.graphql.GraphUtil;
import com.ogaclejapan.smarttablayout.SmartTabLayout;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.wax911.library.model.request.QueryContainerBuilder;

/**
 * Created by max on 2017/12/14.
 * staff activity
 */

public class StaffActivity extends ActivityBase<StaffBase, BasePresenter> {

    protected @BindView(R.id.toolbar) Toolbar toolbar;
    protected @BindView(R.id.page_container) ViewPager viewPager;
    protected @BindView(R.id.smart_tab) SmartTabLayout smartTabLayout;
    protected @BindView(R.id.coordinator) CoordinatorLayout coordinatorLayout;

    private Boolean onList;

    private StaffBase model;

    private FavouriteToolbarWidget favouriteWidget;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager_generic);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        setPresenter(new BasePresenter(this));
        setViewModel(true);
        id = getIntent().getLongExtra(KeyUtil.arg_id, -1);
        onList = (Boolean) getIntent().getSerializableExtra(KeyUtil.arg_onList);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getViewModel().getParams().putLong(KeyUtil.arg_id, id);
        getViewModel().getParams().putSerializable(KeyUtil.arg_onList, onList);
        onActivityReady();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isAuth = getPresenter().getSettings().isAuthenticated();
        getMenuInflater().inflate(R.menu.staff_menu, menu);
        menu.findItem(R.id.action_favourite).setVisible(isAuth);
        menu.findItem(R.id.action_on_my_list).setVisible(isAuth);
        if(isAuth) {
            MenuItem favouriteMenuItem = menu.findItem(R.id.action_favourite);
            favouriteWidget = (FavouriteToolbarWidget) favouriteMenuItem.getActionView();
            if(model != null)
                favouriteWidget.setModel(model);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(model != null) {
            switch (item.getItemId()) {
                case R.id.action_share:
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_TEXT, String.format(Locale.getDefault(),
                            "%s - %s", model.getName().getFullName(), model.getSiteUrl()));
                    intent.setType("text/plain");
                    startActivity(Intent.createChooser(intent, getString(R.string.abc_shareactionprovider_share_with)));
                    break;
                case R.id.action_on_my_list:
                    DialogUtil.createSelection(this, R.string.app_filter_on_list,
                            onList == null ? 0 : !onList ? 1 : 2,
                            CompatUtil.INSTANCE.getStringList(this, R.array.on_list_values),
                            (dialog, which) -> {
                                if (which == DialogAction.POSITIVE) {
                                    switch (dialog.getSelectedIndex()) {
                                        case 0:
                                            onList = null;
                                            break;
                                        case 1:
                                            onList = false;
                                            break;
                                        case 2:
                                            onList = true;
                                            break;
                                    }
                                    reloadViewPager();
                                }
                            });
                    break;
            }
        } else
            NotifyUtil.INSTANCE.makeText(getApplicationContext(), R.string.text_activity_loading, Toast.LENGTH_SHORT).show();
        return super.onOptionsItemSelected(item);
    }

    /**
     * Make decisions, check for permissions or fire background threads from this method
     * N.B. Must be called after onPostCreate
     */
    @Override
    protected void onActivityReady() {
        StaffPageAdapter pageAdapter = new StaffPageAdapter(getSupportFragmentManager(), getApplicationContext());
        pageAdapter.setParams(getViewModel().getParams());
        viewPager.setAdapter(pageAdapter);
        viewPager.setOffscreenPageLimit(offScreenLimit + 1);
        smartTabLayout.setViewPager(viewPager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(model == null)
            makeRequest();
        else
            updateUI();
    }

    @Override
    protected void updateUI() {
        if(model != null)
            if(favouriteWidget != null)
                favouriteWidget.setModel(model);
    }

    @Override
    protected void makeRequest() {
        QueryContainerBuilder queryContainer = GraphUtil.INSTANCE.getDefaultQuery(false)
                .putVariable(KeyUtil.arg_id, id);
        getViewModel().getParams().putParcelable(KeyUtil.arg_graph_params, queryContainer);
        getViewModel().requestData(KeyUtil.STAFF_BASE_REQ, getApplicationContext());
    }

    /**
     * Called when the model state is changed.
     *
     * @param model The new data
     */
    @Override
    public void onChanged(@Nullable StaffBase model) {
        super.onChanged(model);
        this.model = model;
        updateUI();
    }

    private void reloadViewPager() {
        StaffPageAdapter adapter = new StaffPageAdapter(getSupportFragmentManager(), getApplicationContext());

        // Update params if necessary
        getViewModel().getParams().putLong(KeyUtil.arg_id, id);
        getViewModel().getParams().putSerializable(KeyUtil.arg_onList, onList);
        adapter.setParams(getViewModel().getParams());

        // Re-set adapter while preserving currently selected item
        int currentItem = viewPager.getCurrentItem();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentItem);
    }
}
