/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.browser;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.android.browser.stub.NullController;
import com.google.common.annotations.VisibleForTesting;

public class BrowserActivity extends Activity {

    public static final String ACTION_SHOW_BOOKMARKS = "show_bookmarks";
    public static final String ACTION_SHOW_BROWSER = "show_browser";
    public static final String ACTION_RESTART = "--restart--";
    private static final String EXTRA_STATE = "state";

    private final static String LOGTAG = "browser";

    private final static boolean LOGV_ENABLED = Browser.LOGV_ENABLED;

    private ActivityController mController = NullController.INSTANCE;

    @Override
    public void onCreate(Bundle icicle) {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, this + " onStart, has state: "
                    + (icicle == null ? "false" : "true"));
        }
        super.onCreate(icicle);

        if (shouldIgnoreIntents()) {
            finish();
            return;
        }

        // If this was a web search request, pass it on to the default web
        // search provider and finish this activity.
        if (IntentHandler.handleWebSearchIntent(this, null, getIntent())) {
            finish();
            return;
        }
        mController = createController();

        Intent intent = (icicle == null) ? getIntent() : null;
        mController.start(intent);
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    private Controller createController() {
        Controller controller = new Controller(this);
        boolean xlarge = isTablet(this);
        UI ui = null;
        if (xlarge) {
            ui = new XLargeUi(this, controller);
        } else {
            ui = new PhoneUi(this, controller);
        }
        controller.setUi(ui);
        return controller;
    }

    @VisibleForTesting
    Controller getController() {
        return (Controller) mController;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (shouldIgnoreIntents()) return;
        if (ACTION_RESTART.equals(intent.getAction())) {
            Bundle outState = new Bundle();
            mController.onSaveInstanceState(outState);
            finish();
            getApplicationContext().startActivity(
                    new Intent(getApplicationContext(), BrowserActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_STATE, outState));
            return;
        }
<<<<<<< HEAD
        mController.handleNewIntent(intent);
=======
        // In case the SearchDialog is open.
        ((SearchManager) getSystemService(Context.SEARCH_SERVICE))
                .stopSearch();
        boolean activateVoiceSearch = RecognizerResultsIntent
                .ACTION_VOICE_SEARCH_RESULTS.equals(action);
        if (Intent.ACTION_VIEW.equals(action)
                || Intent.ACTION_SEARCH.equals(action)
                || MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(action)
                || Intent.ACTION_WEB_SEARCH.equals(action)
                || activateVoiceSearch) {
            if (current.isInVoiceSearchMode()) {
                String title = current.getVoiceDisplayTitle();
                if (title != null && title.equals(intent.getStringExtra(
                        SearchManager.QUERY))) {
                    // The user submitted the same search as the last voice
                    // search, so do nothing.
                    return;
                }
                if (Intent.ACTION_SEARCH.equals(action)
                        && current.voiceSearchSourceIsGoogle()) {
                    Intent logIntent = new Intent(
                            LoggingEvents.ACTION_LOG_EVENT);
                    logIntent.putExtra(LoggingEvents.EXTRA_EVENT,
                            LoggingEvents.VoiceSearch.QUERY_UPDATED);
                    logIntent.putExtra(
                            LoggingEvents.VoiceSearch.EXTRA_QUERY_UPDATED_VALUE,
                            intent.getDataString());
                    sendBroadcast(logIntent);
                    // Note, onPageStarted will revert the voice title bar
                    // When http://b/issue?id=2379215 is fixed, we should update
                    // the title bar here.
                }
            }
            // If this was a search request (e.g. search query directly typed into the address bar),
            // pass it on to the default web search provider.
            if (handleWebSearchIntent(intent)) {
                return;
            }

            UrlData urlData = getUrlDataFromIntent(intent);
            if (urlData.isEmpty()) {
                urlData = new UrlData(mSettings.getHomePage());
            }

            final String appId = intent
                    .getStringExtra(Browser.EXTRA_APPLICATION_ID);
            if ((Intent.ACTION_VIEW.equals(action)
                    // If a voice search has no appId, it means that it came
                    // from the browser.  In that case, reuse the current tab.
                    || (activateVoiceSearch && appId != null))
                    && !getPackageName().equals(appId)
                    && (flags & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
                Tab appTab = mTabControl.getTabFromId(appId);
                if (appTab != null) {
                    Log.i(LOGTAG, "Reusing tab for " + appId);
                    // Dismiss the subwindow if applicable.
                    dismissSubWindow(appTab);
                    // Since we might kill the WebView, remove it from the
                    // content view first.
                    removeTabFromContentView(appTab);
                    // Recreate the main WebView after destroying the old one.
                    // If the WebView has the same original url and is on that
                    // page, it can be reused.
                    boolean needsLoad =
                            mTabControl.recreateWebView(appTab, urlData);

                    if (current != appTab) {
                        switchToTab(mTabControl.getTabIndex(appTab));
                        if (needsLoad) {
                            loadUrlDataIn(appTab, urlData);
                        }
                    } else {
                        // If the tab was the current tab, we have to attach
                        // it to the view system again.
                        attachTabToContentView(appTab);
                        if (needsLoad) {
                            loadUrlDataIn(appTab, urlData);
                        }
                    }
                    return;
                } else {
                    // No matching application tab, try to find a regular tab
                    // with a matching url.
                    appTab = mTabControl.findUnusedTabWithUrl(urlData.mUrl);
                    if (appTab != null) {
                        if (current != appTab) {
                            switchToTab(mTabControl.getTabIndex(appTab));
                        }
                        // Otherwise, we are already viewing the correct tab.
                    } else {
                        // if FLAG_ACTIVITY_BROUGHT_TO_FRONT flag is on, the url
                        // will be opened in a new tab unless we have reached
                        // MAX_TABS. Then the url will be opened in the current
                        // tab. If a new tab is created, it will have "true" for
                        // exit on close.
                        openTabAndShow(urlData, true, appId);
                    }
                }
            } else {
                if (!urlData.isEmpty()
                        && urlData.mUrl.startsWith("about:debug")) {
                    if ("about:debug.dom".equals(urlData.mUrl)) {
                        current.getWebView().dumpDomTree(false);
                    } else if ("about:debug.dom.file".equals(urlData.mUrl)) {
                        if (hasPermissionForTest()) {
                            current.getWebView().dumpDomTree(true);
                        }
                    } else if ("about:debug.render".equals(urlData.mUrl)) {
                        current.getWebView().dumpRenderTree(false);
                    } else if ("about:debug.render.file".equals(urlData.mUrl)) {
                        if (hasPermissionForTest()) {
                            current.getWebView().dumpRenderTree(true);
                        }
                    } else if ("about:debug.display".equals(urlData.mUrl)) {
                        if (hasPermissionForTest()) {
                            current.getWebView().dumpDisplayTree();
                        }
                    } else if (urlData.mUrl.startsWith("about:debug.drag")) {
                        int index = urlData.mUrl.codePointAt(16) - '0';
                        if (index <= 0 || index > 9) {
                            current.getWebView().setDragTracker(null);
                        } else {
                            current.getWebView().setDragTracker(new MeshTracker(index));
                        }
                    } else {
                        mSettings.toggleDebugSettings();
                    }
                    return;
                }
                // Get rid of the subwindow if it exists
                dismissSubWindow(current);
                // If the current Tab is being used as an application tab,
                // remove the association, since the new Intent means that it is
                // no longer associated with that application.
                current.setAppId(null);
                loadUrlDataIn(current, urlData);
            }
        }
    }

    /**
     * Launches the default web search activity with the query parameters if the given intent's data
     * are identified as plain search terms and not URLs/shortcuts.
     * @return true if the intent was handled and web search activity was launched, false if not.
     */
    private boolean handleWebSearchIntent(Intent intent) {
        if (intent == null) return false;

        String url = null;
        final String action = intent.getAction();
        if (RecognizerResultsIntent.ACTION_VOICE_SEARCH_RESULTS.equals(
                action)) {
            return false;
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) url = data.toString();
        } else if (Intent.ACTION_SEARCH.equals(action)
                || MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(action)
                || Intent.ACTION_WEB_SEARCH.equals(action)) {
            url = intent.getStringExtra(SearchManager.QUERY);
        }
        return handleWebSearchRequest(url, intent.getBundleExtra(SearchManager.APP_DATA),
                intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
    }

    /**
     * Launches the default web search activity with the query parameters if the given url string
     * was identified as plain search terms and not URL/shortcut.
     * @return true if the request was handled and web search activity was launched, false if not.
     */
    private boolean handleWebSearchRequest(String inUrl, Bundle appData, String extraData) {
        if (inUrl == null) return false;

        // In general, we shouldn't modify URL from Intent.
        // But currently, we get the user-typed URL from search box as well.
        String url = fixUrl(inUrl).trim();

        // URLs are handled by the regular flow of control, so
        // return early.
        if (Patterns.WEB_URL.matcher(url).matches()
                || ACCEPTED_URI_SCHEMA.matcher(url).matches()) {
            return false;
        }

        final ContentResolver cr = mResolver;
        final String newUrl = url;
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... unused) {
                Browser.updateVisitedHistory(cr, newUrl, false);
                Browser.addSearchUrl(cr, newUrl);
                return null;
            }
        }.execute();

        SearchEngine searchEngine = mSettings.getSearchEngine();
        if (searchEngine == null) return false;
        searchEngine.startSearch(this, url, appData, extraData);

        return true;
>>>>>>> c8346a7... Lockdown Browser to mitigate vulnerabilities
    }

    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private boolean shouldIgnoreIntents() {
        // Only process intents if the screen is on and the device is unlocked
        // aka, if we will be user-visible
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        }
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
        boolean ignore = !mPowerManager.isScreenOn();
        ignore |= mKeyguardManager.inKeyguardRestrictedInputMode();
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "ignore intents: " + ignore);
        }
        return ignore;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onResume: this=" + this);
        }
        mController.onResume();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (Window.FEATURE_OPTIONS_PANEL == featureId) {
            mController.onMenuOpened(featureId, menu);
        }
        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        mController.onOptionsMenuClosed(menu);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        mController.onContextMenuClosed(menu);
    }

    /**
     *  onSaveInstanceState(Bundle map)
     *  onSaveInstanceState is called right before onStop(). The map contains
     *  the saved state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onSaveInstanceState: this=" + this);
        }
        mController.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        mController.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onDestroy: this=" + this);
        }
        super.onDestroy();
        mController.onDestroy();
        mController = NullController.INSTANCE;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mController.onConfgurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mController.onLowMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return mController.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return mController.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mController.onOptionsItemSelected(item)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        mController.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return mController.onContextItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mController.onKeyDown(keyCode, event) ||
            super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mController.onKeyLongPress(keyCode, event) ||
            super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mController.onKeyUp(keyCode, event) ||
            super.onKeyUp(keyCode, event);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        mController.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        mController.onActionModeFinished(mode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        mController.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onSearchRequested() {
        return mController.onSearchRequested();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mController.dispatchKeyEvent(event)
                || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return mController.dispatchKeyShortcutEvent(event)
                || super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mController.dispatchTouchEvent(ev)
                || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return mController.dispatchTrackballEvent(ev)
                || super.dispatchTrackballEvent(ev);
    }

    @Override
<<<<<<< HEAD
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return mController.dispatchGenericMotionEvent(ev) ||
                super.dispatchGenericMotionEvent(ev);
=======
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (getTopWindow() == null) return;

        switch (requestCode) {
            case COMBO_PAGE:
                if (resultCode == RESULT_OK && intent != null) {
                    String data = intent.getAction();
                    Bundle extras = intent.getExtras();
                    if (extras != null && extras.getBoolean("new_window", false)) {
                        openTab(data);
                    } else {
                        final Tab currentTab =
                                mTabControl.getCurrentTab();
                        dismissSubWindow(currentTab);
                        if (data != null && data.length() != 0) {
                            loadUrl(getTopWindow(), data);
                        }
                    }
                }
                // Deliberately fall through to PREFERENCES_PAGE, since the
                // same extra may be attached to the COMBO_PAGE
            case PREFERENCES_PAGE:
                if (resultCode == RESULT_OK && intent != null) {
                    String action = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (BrowserSettings.PREF_CLEAR_HISTORY.equals(action)) {
                        mTabControl.removeParentChildRelationShips();
                    }
                }
                break;
            // Choose a file from the file picker.
            case FILE_SELECTED:
                if (null == mUploadMessage) break;
                Uri result = intent == null || resultCode != RESULT_OK ? null
                        : intent.getData();
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
                break;
            default:
                break;
        }
        getTopWindow().requestFocus();
    }

    /*
     * This method is called as a result of the user selecting the options
     * menu to see the download window. It shows the download window on top of
     * the current window.
     */
    private void viewDownloads() {
        Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        startActivity(intent);
    }

    /**
     * Open the Go page.
     * @param startWithHistory If true, open starting on the history tab.
     *                         Otherwise, start with the bookmarks tab.
     */
    /* package */ void bookmarksOrHistoryPicker(boolean startWithHistory) {
        WebView current = mTabControl.getCurrentWebView();
        if (current == null) {
            return;
        }
        Intent intent = new Intent(this,
                CombinedBookmarkHistoryActivity.class);
        String title = current.getTitle();
        String url = current.getUrl();
        Bitmap thumbnail = createScreenshot(current);

        // Just in case the user opens bookmarks before a page finishes loading
        // so the current history item, and therefore the page, is null.
        if (null == url) {
            url = mLastEnteredUrl;
            // This can happen.
            if (null == url) {
                url = mSettings.getHomePage();
            }
        }
        // In case the web page has not yet received its associated title.
        if (title == null) {
            title = url;
        }
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        intent.putExtra("thumbnail", thumbnail);
        // Disable opening in a new window if we have maxed out the windows
        intent.putExtra("disable_new_window", !mTabControl.canCreateNewTab());
        intent.putExtra("touch_icon_url", current.getTouchIconUrl());
        if (startWithHistory) {
            intent.putExtra(CombinedBookmarkHistoryActivity.STARTING_TAB,
                    CombinedBookmarkHistoryActivity.HISTORY_TAB);
        }
        startActivityForResult(intent, COMBO_PAGE);
    }

    // Called when loading from context menu or LOAD_URL message
    private void loadUrlFromContext(WebView view, String url) {
        // In case the user enters nothing.
        if (url != null && url.length() != 0 && view != null) {
            url = smartUrlFilter(url);
            if (!view.getWebViewClient().shouldOverrideUrlLoading(view, url)) {
                loadUrl(view, url);
            }
        }
    }

    /**
     * Load the URL into the given WebView and update the title bar
     * to reflect the new load.  Call this instead of WebView.loadUrl
     * directly.
     * @param view The WebView used to load url.
     * @param url The URL to load.
     */
    private void loadUrl(WebView view, String url) {
        updateTitleBarForNewLoad(view, url);
        view.loadUrl(url);
    }

    /**
     * Load UrlData into a Tab and update the title bar to reflect the new
     * load.  Call this instead of UrlData.loadIn directly.
     * @param t The Tab used to load.
     * @param data The UrlData being loaded.
     */
    private void loadUrlDataIn(Tab t, UrlData data) {
        updateTitleBarForNewLoad(t.getWebView(), data.mUrl);
        data.loadIn(t);
    }

    /**
     * If the WebView is the top window, update the title bar to reflect
     * loading the new URL.  i.e. set its text, clear the favicon (which
     * will be set once the page begins loading), and set the progress to
     * INITIAL_PROGRESS to show that the page has begun to load. Called
     * by loadUrl and loadUrlDataIn.
     * @param view The WebView that is starting a load.
     * @param url The URL that is being loaded.
     */
    private void updateTitleBarForNewLoad(WebView view, String url) {
        if (view == getTopWindow()) {
            setUrlTitle(url, null);
            setFavicon(null);
            onProgressChanged(view, INITIAL_PROGRESS);
        }
    }

    private String smartUrlFilter(Uri inUri) {
        if (inUri != null) {
            return smartUrlFilter(inUri.toString());
        }
        return null;
    }

    protected static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile(
            "(?i)" + // switch on case insensitive matching
            "(" +    // begin group for schema
            "(?:http|https|file):\\/\\/" +
            "|(?:inline|data|about|content|javascript):" +
            ")" +
            "(.*)" );

    /**
     * Attempts to determine whether user input is a URL or search
     * terms.  Anything with a space is passed to search.
     *
     * Converts to lowercase any mistakenly uppercased schema (i.e.,
     * "Http://" converts to "http://"
     *
     * @return Original or modified URL
     *
     */
    String smartUrlFilter(String url) {

        String inUrl = url.trim();
        boolean hasSpace = inUrl.indexOf(' ') != -1;

        Matcher matcher = ACCEPTED_URI_SCHEMA.matcher(inUrl);
        if (matcher.matches()) {
            // force scheme to lowercase
            String scheme = matcher.group(1);
            String lcScheme = scheme.toLowerCase();
            if (!lcScheme.equals(scheme)) {
                inUrl = lcScheme + matcher.group(2);
            }
            if (hasSpace) {
                inUrl = inUrl.replace(" ", "%20");
            }
            return inUrl;
        }
        if (!hasSpace) {
            if (Patterns.WEB_URL.matcher(inUrl).matches()) {
                return URLUtil.guessUrl(inUrl);
            }
        }

        // FIXME: Is this the correct place to add to searches?
        // what if someone else calls this function?

        Browser.addSearchUrl(mResolver, inUrl);
        return URLUtil.composeSearchUrl(inUrl, QuickSearch_G, QUERY_PLACE_HOLDER);
    }

    /* package */ void setShouldShowErrorConsole(boolean flag) {
        if (flag == mShouldShowErrorConsole) {
            // Nothing to do.
            return;
        }
        Tab t = mTabControl.getCurrentTab();
        if (t == null) {
            // There is no current tab so we cannot toggle the error console
            return;
        }

        mShouldShowErrorConsole = flag;

        ErrorConsoleView errorConsole = t.getErrorConsole(true);

        if (flag) {
            // Setting the show state of the console will cause it's the layout to be inflated.
            if (errorConsole.numberOfErrors() > 0) {
                errorConsole.showConsole(ErrorConsoleView.SHOW_MINIMIZED);
            } else {
                errorConsole.showConsole(ErrorConsoleView.SHOW_NONE);
            }

            // Now we can add it to the main view.
            mErrorConsoleContainer.addView(errorConsole,
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                  ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            mErrorConsoleContainer.removeView(errorConsole);
        }

    }

    boolean shouldShowErrorConsole() {
        return mShouldShowErrorConsole;
    }

    private boolean hasPermissionForTest() {
        boolean permission = checkPermission("android.permission.WRITE_EXTERNAL_STORAGE",
                android.os.Process.myPid(), android.os.Process.myUid()) ==
                    PackageManager.PERMISSION_GRANTED;
        if (!permission) {
            Log.w(LOGTAG, "Missing WRITE_EXTERNAL_STORAGE permission needed for test");
        }
        return permission;
    }

    private void setStatusBarVisibility(boolean visible) {
        int flag = visible ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setFlags(flag, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    private void sendNetworkType(String type, String subtype) {
        WebView w = mTabControl.getCurrentWebView();
        if (w != null) {
            w.setNetworkType(type, subtype);
        }
>>>>>>> c8346a7... Lockdown Browser to mitigate vulnerabilities
    }

}
