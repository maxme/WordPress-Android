package org.wordpress.android.ui.menus;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.MenuLocationTable;
import org.wordpress.android.datasets.MenuTable;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.networking.menus.MenusRestWPCom;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.menus.views.MenuAddEditRemoveView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.List;

public class MenusFragment extends Fragment {

    private boolean mUndoPressed = false;
    private MenusRestWPCom mRestWPCom;
    private MenuAddEditRemoveView mAddEditRemoveControl;
    private boolean mRequestBeingProcessed;
    private int mCurrentLoadRequestId;
    private int mCurrentCreateRequestId;
    private int mCurrentUpdateRequestId;
    private int mCurrentDeleteRequestId;
    private boolean mIsUpdatingMenus;
    private TextView mEmptyView;
    private LinearLayout mSpinnersLayout;
    private MenusSpinner mMenuLocationsSpinner;
    private MenusSpinner mMenusSpinner;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mRestWPCom = new MenusRestWPCom(new MenusRestWPCom.MenusListener() {
            @Override public long getSiteId() {
                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
            }
            @Override public void onMenuCreated(int requestId, MenuModel menu) {

                //TODO save new menu to local DB here

                if (!isAdded()) {
                    return;
                }

                Toast.makeText(getActivity(), "menu: " + menu.name + " created", Toast.LENGTH_SHORT).show();
                // add this newly created menu to the spinner
                if (mMenusSpinner.getItems() != null) {
                    //remove "add menu option" item (which is the last one)
                    mMenusSpinner.getItems().remove(mMenusSpinner.getItems().size() - 1);

                    //add the newly created menu
                    mMenusSpinner.getItems().add(menu);

                    //re-add the "add menu option" item
                    insertAddMenuOption(mMenusSpinner.getItems());
                    mMenusSpinner.setItems(mMenusSpinner.getItems());

                    //set this newly created menu
                    mMenusSpinner.setSelection(mMenusSpinner.getItems().size() - 2);

                }
                mRequestBeingProcessed = false;
            }
            @Override public Context getContext() { return getActivity(); }
            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) {
                boolean bSpinnersUpdated = false;
                if (locations != null) {
                    if (CollectionUtils.areListsEqual(locations, mMenuLocationsSpinner.getItems())) {
                        // no op
                    } else {
                        // update Menu Locations spinner
                        mMenuLocationsSpinner.setItems((List)locations);
                        bSpinnersUpdated = true;
                    }
                }

                if (menus != null) {
                    if (CollectionUtils.areListsEqual(menus, mMenusSpinner.getItems())) {
                        // no op
                    } else {
                        //insert first Default Menu
                        prepareMenuList(menus);

                        // update Menus spinner
                        mMenusSpinner.setItems((List)menus);
                        bSpinnersUpdated = true;

                        //TODO save menus to local DB here
                    }
                }

                if (!isAdded()) {
                    return;
                }

                if (bSpinnersUpdated) {
                    hideEmptyView();
                }
                mIsUpdatingMenus = false;
            }

            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) {
                //TODO delete menus from local DB here

                if (!isAdded()) {
                    return;
                }

                if (deleted)
                    Toast.makeText(getActivity(), "menu: " + menu.name + " deleted", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(), "menu: " + menu.name + " delete request NOT DELETED", Toast.LENGTH_SHORT).show();

                //delete menu from Spinner here
                if (mMenusSpinner.getItems() != null) {
                    if (mMenusSpinner.getItems().remove(menu)) {
                        mMenusSpinner.setItems(mMenusSpinner.getItems());
                    }
                }

                mRequestBeingProcessed = false;
            }
            @Override public void onMenuUpdated(int requestId, MenuModel menu) {

                //TODO update menu in local DB here

                if (!isAdded()) {
                    return;
                }

                Toast.makeText(getActivity(), "menu: " + menu.name + " updated", Toast.LENGTH_SHORT).show();

                //update menu in Spinner here
                if (mMenusSpinner.getItems() != null) {
                    int selectedPos = -1;
                    for (int i=0; i < mMenusSpinner.getItems().size(); i++) {
                        MenuModel item = (MenuModel) mMenusSpinner.getItems().get(i);
                        if (item != null && item.menuId == menu.menuId) {
                            selectedPos = i;
                            item.name = menu.name;
                            item.details = menu.details;
                            item.locations = menu.locations;
                            item.menuItems = menu.menuItems;
                            break;
                        }
                    }

                    //I have to re-set items on the Spinner so not only the adapter will change but also the textview
                    //within the Spinner control - note that if a menu has been updated, it is currently being shown and
                    //selected within the Spinner control view, so it needs to change to reflect the update as well.
                    if (selectedPos >= 0) {
                        mMenusSpinner.setItems(mMenusSpinner.getItems());
                        mMenusSpinner.setSelection(selectedPos);
                    }
                }

                mRequestBeingProcessed = false;
            }

            @Override
            public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) {
                // load menus
                if (error == MenusRestWPCom.REST_ERROR.FETCH_ERROR) {
                    if (mMenuLocationsSpinner.getCount() == 0 || mMenusSpinner.getCount() == 0) {
                        Toast.makeText(getActivity(), getString(R.string.could_not_load_menus), Toast.LENGTH_SHORT).show();
                        updateEmptyView(EmptyViewMessageType.NO_CONTENT);
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.could_not_refresh_menus), Toast.LENGTH_SHORT).show();
                    }
                    mIsUpdatingMenus = false;
                }
                else
                if (error == MenusRestWPCom.REST_ERROR.CREATE_ERROR) {
                    Toast.makeText(getActivity(), getString(R.string.could_not_create_menu), Toast.LENGTH_SHORT).show();
                }
                else
                if (error == MenusRestWPCom.REST_ERROR.UPDATE_ERROR) {
                    Toast.makeText(getActivity(), getString(R.string.could_not_update_menu), Toast.LENGTH_SHORT).show();
                }
                mRequestBeingProcessed = false;
            }
        });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.menus_fragment, container, false);
        mAddEditRemoveControl = (MenuAddEditRemoveView) view.findViewById(R.id.menu_add_edit_remove_view);
        mAddEditRemoveControl.setMenuActionListener(new MenuAddEditRemoveView.MenuAddEditRemoveActionListener() {

            @Override
            public void onMenuCreate(MenuModel menu) {
                mCurrentCreateRequestId = mRestWPCom.createMenu(menu);
            }

            @Override
            public void onMenuDelete(final MenuModel menu) {

                //delete menu from Spinner here
                if (mMenusSpinner.getItems() != null) {
                    if (mMenusSpinner.getItems().remove(menu)) {
                        mMenusSpinner.setItems(mMenusSpinner.getItems());
                        mMenusSpinner.setSelection(-1, true);
                    }
                }

                View.OnClickListener undoListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mUndoPressed = true;
                        // user undid the trash action, so reset the control to whatever it had
                        mAddEditRemoveControl.setMenu(menu, false);
                        //restore the menu item in the spinner list
                        if (mMenusSpinner.getItems() != null) {
                            //remove "add menu option" item (which is the last one)
                            mMenusSpinner.getItems().remove(mMenusSpinner.getItems().size() - 1);

                            //add the newly created menu
                            mMenusSpinner.getItems().add(menu);

                            //re-add the "add menu option" item
                            insertAddMenuOption(mMenusSpinner.getItems());
                            mMenusSpinner.setItems(mMenusSpinner.getItems());

                            //set this newly created menu
                            mMenusSpinner.setSelection(mMenusSpinner.getItems().size() - 2);
                        }
                    }
                };

                Snackbar snackbar = Snackbar.make(getView(), getString(R.string.menus_menu_deleted), Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, undoListener);

                // wait for the undo snackbar to disappear before actually deleting the menu
                snackbar.setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (mUndoPressed) {
                            mUndoPressed = false;
                            return;
                        }

                        if (!mRequestBeingProcessed) {
                            mRequestBeingProcessed = true;
                            mCurrentDeleteRequestId = mRestWPCom.deleteMenu(menu);
                        }
                    }
                });

                snackbar.show();

            }

            @Override
            public void onMenuUpdate(MenuModel menu) {
                if (!mRequestBeingProcessed) {
                    mRequestBeingProcessed = true;
                    mCurrentUpdateRequestId = mRestWPCom.updateMenu(menu);
                }
            }
        });

        mMenuLocationsSpinner = (MenusSpinner) view.findViewById(R.id.menu_locations_spinner);
        mMenusSpinner = (MenusSpinner) view.findViewById(R.id.selected_menu_spinner);
        mEmptyView = (TextView) view.findViewById(R.id.empty_view);
        mSpinnersLayout = (LinearLayout) view.findViewById(R.id.spinner_group);

        mMenusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mMenusSpinner.getItems().size() == (position + 1)) {
                    //clicked on "add new menu"

                    //for new menus, given we might be off line, it' best to not try creating a default menu right away
                    // (as opposed to how the calypso web does this)
                    //but wait for the user to enter a name for the menu and click SAVE on the AddRemoveEdit view control
                    //that's why we set the menu within the control to null
                    mAddEditRemoveControl.setMenu(null, false);
                } else {
                    MenuModel model = (MenuModel) mMenusSpinner.getItems().get(position);
                    //TODO: check when to tell this is a default menu or not
                    mAddEditRemoveControl.setMenu(model, (position == 0));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mMenuLocationsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //auto-select the first available menu for this location
                List<MenuModel> menus = mMenusSpinner.getItems();
                if (menus != null && menus.size() > 0 && mMenuLocationsSpinner.getItems() != null &&
                        mMenuLocationsSpinner.getItems().size() > 0) {
                    MenuLocationModel menuLocationSelected = (MenuLocationModel)
                            mMenuLocationsSpinner.getItems().get(position);
                    for (int i = 0; i < menus.size(); i++) {
                        MenuModel menu = menus.get(i);
                        if (menu.locations != null) {
                            for (MenuLocationModel menuLocation : menu.locations) {
                                if (menuLocationSelected.equals(menuLocation)) {
                                    //set this one and break;
                                    mMenusSpinner.setSelection(i);
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateMenus();
    }

    private void updateMenus() {
        if (mIsUpdatingMenus) {
            AppLog.w(AppLog.T.MENUS, "update menus task already running");
            return;
        }

        updateEmptyView(EmptyViewMessageType.LOADING);

        //immediately load/refresh whatever we have in our local db
        loadMenus();

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            //we're offline
            return;
        }

        //also fetch latest menus from the server
        mIsUpdatingMenus = true;
        mCurrentLoadRequestId = mRestWPCom.fetchAllMenus();

    }


    private void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        if (mEmptyView != null) {
            int stringId = 0;

            switch (emptyViewMessageType) {
                case LOADING:
                    stringId = R.string.loading;
                    break;
                case NO_CONTENT:
                    stringId = R.string.menus_spinner_empty;
                    break;
                case NETWORK_ERROR:
                    stringId = R.string.no_network_message;
                    break;
            }

            mEmptyView.setText(getText(stringId));
            mEmptyView.setVisibility(View.VISIBLE);

            if (mSpinnersLayout != null) {
                mSpinnersLayout.setVisibility(View.GONE);
            }
        }
    }

    private void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }

        if (mSpinnersLayout != null) {
            mSpinnersLayout.setVisibility(View.VISIBLE);
        }
    }

    private void prepareMenuList(List<MenuModel> menus) {
        insertDefaultMenu(menus);
        insertAddMenuOption(menus);
    }

    private void insertDefaultMenu(List<MenuModel> menus) {
        if (menus != null) {
            MenuModel defaultMenu = new MenuModel();
            defaultMenu.name = getString(R.string.menus_default_menu_name);
            menus.add(0, defaultMenu);
        }
    }

    private void insertAddMenuOption(List<MenuModel> menus) {
        if (menus != null) {
            MenuModel addMenuOption = new MenuModel();
            addMenuOption.name = getString(R.string.menus_add_menu_name);
            menus.add(addMenuOption);
        }
    }

    /*
     * load menus using an AsyncTask
     */
    public void loadMenus() {
        if (mIsLoadTaskRunning) {
            AppLog.w(AppLog.T.MENUS, "load menus task already active");
        } else {
            new LoadMenusTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
     * AsyncTask to load menus from SQLite
     */
    private boolean mIsLoadTaskRunning = false;
    private class LoadMenusTask extends AsyncTask<Void, Void, Boolean> {
        List<MenuModel> tmpMenus;
        List<MenuLocationModel> tmpMenuLocations;

        @Override
        protected void onPreExecute() {
            mIsLoadTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsLoadTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            tmpMenus = MenuTable.getAllMenusForCurrentSite();
            tmpMenuLocations = MenuLocationTable.getAllMenuLocationsForCurrentSite();
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mMenuLocationsSpinner.setItems((List)tmpMenuLocations);
                //insert first Default Menu
                prepareMenuList(tmpMenus);
                mMenusSpinner.setItems((List)tmpMenus);
            }

            if ( (!result || tmpMenuLocations == null || tmpMenuLocations.size() == 0)
                    || tmpMenus == null || tmpMenus.size() == 0 ) {
                updateEmptyView(EmptyViewMessageType.NO_CONTENT);
            } else {
                hideEmptyView();
            }

            mIsLoadTaskRunning = false;
        }
    }


    private boolean mIsSaveTaskRunning = false;
    private class SaveMenusTask extends AsyncTask<Void, Void, Boolean> {
        List<MenuModel> tmpMenus;
        List<MenuLocationModel> tmpMenuLocations;

        @Override
        protected void onPreExecute() {
            mIsSaveTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsSaveTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            tmpMenus = MenuTable.getAllMenusForCurrentSite();
            tmpMenuLocations = MenuLocationTable.getAllMenuLocationsForCurrentSite();
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mMenuLocationsSpinner.setItems((List)tmpMenuLocations);
                //insert first Default Menu
                prepareMenuList(tmpMenus);
                mMenusSpinner.setItems((List)tmpMenus);
            }

            if ( (!result || tmpMenuLocations == null || tmpMenuLocations.size() == 0)
                    || tmpMenus == null || tmpMenus.size() == 0 ) {
                updateEmptyView(EmptyViewMessageType.NO_CONTENT);
            } else {
                hideEmptyView();
            }

            mIsSaveTaskRunning = false;
        }
    }


}

