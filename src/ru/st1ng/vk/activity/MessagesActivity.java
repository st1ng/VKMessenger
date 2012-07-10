package ru.st1ng.vk.activity;

import java.util.List;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.data.RecordsProvider.RecordsWatcher;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.SearchDialogsTask;
import ru.st1ng.vk.network.async.SearchMessagesTask;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.UIUtil;
import ru.st1ng.vk.views.adapter.DialogListAdapter;
import ru.st1ng.vk.views.adapter.FriendListAdapter;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MessagesActivity extends Activity {

    TextView titleText;
    ListView dialogListView;
    ImageView updatingView;
    AnimationDrawable updatingDrawable;

    EditText searchView;
    ImageView clearSearchView;
    ImageView openNewDialog;
    ImageView errorImage;

    TextView searchInMessages;
    AnimationDrawable loadingAnimation;

    RecordsProvider recordsManager;
    RecordsProvider.RecordsWatcher recordsWatcher;

    DialogListAdapter dialogAdapter;
    FriendListAdapter friendAdapter;
    View headerView;
    View footerView;
    ImageView loadingView;

    private boolean mBound = false;
    private boolean searchMode = false;
    private boolean searchInMessagesFlag = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_messages);
        titleText = (TextView) findViewById(R.id.titleText);
        updatingView = (ImageView) findViewById(R.id.updatingImage);
        updatingDrawable = (AnimationDrawable) updatingView.getDrawable();
        dialogListView = (ListView) findViewById(R.id.dialogList);
        openNewDialog = (ImageView) findViewById(R.id.openNewDialog);
        errorImage = (ImageView) findViewById(R.id.errorCaused);

        titleText.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        headerView = getLayoutInflater().inflate(R.layout.widget_search, null);
        footerView = getLayoutInflater().inflate(R.layout.widget_loading, null);
        footerView.setVisibility(View.GONE);
        loadingView = (ImageView) footerView.findViewById(R.id.loadingImage);
        loadingAnimation = (AnimationDrawable) loadingView.getDrawable();

        searchView = (EditText) headerView.findViewById(R.id.search);
        clearSearchView = (ImageView) headerView.findViewById(R.id.clear_field);
        searchInMessages = (TextView) headerView.findViewById(R.id.search_in_messages);
        searchInMessages.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                searchInMessagesFlag =!searchInMessagesFlag;
                searchView.setText(searchView.getText().toString());
                searchInMessages.setText(searchInMessagesFlag ? R.string.search_in_dialogs : R.string.search_in_messages);
            }
        });
        clearSearchView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                searchView.setText("");
            }
        });
        searchView.addTextChangedListener(new TextWatcher() {

            long lastUpdated = 0;

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                clearSearchView.setVisibility(s.length() > 0 ? View.VISIBLE : View.INVISIBLE);
                if (mBound) {
                    if (s.length() == 0) {
                        dialogAdapter.setItems(recordsManager.getDialogs());
                        dialogListView.setAdapter(dialogAdapter);
                        searchMode = false;
                        searchInMessages.setVisibility(View.GONE);
                    } else if (s.length() >= 2) {
                        searchInMessages.setVisibility(View.VISIBLE);
                        updatingView.setVisibility(View.VISIBLE);
                        updatingDrawable.start();
                        lastUpdated = System.currentTimeMillis();
                        searchMode = true;
                        Log.d(VKApplication.TAG, "Search users!");
                        if(searchInMessagesFlag) {
                            new SearchMessagesTask(new AsyncCallback<List<Message>>() {

                                @Override
                                public void OnSuccess(final List<Message> str) {
                                    if (!s.toString().equals(searchView.getText().toString()))
                                        return;                          
                                    dialogAdapter.setItems(str);
                                    dialogListView.setAdapter(dialogAdapter);
                                    dialogAdapter.notifyDataSetChanged();
                                    updatingView.setVisibility(View.INVISIBLE);
                                    new Thread(new Runnable() {
                                        
                                        @Override
                                        public void run() {
                                            recordsManager.populateMessageUsers(str,true);
                                            runOnUiThread(new Runnable() {
                                                
                                                @Override
                                                public void run() {
                                                    dialogAdapter.notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    }).start();     
                                }

                                @Override
                                public void OnError(ErrorCode errorCode) {
                                    updatingView.setVisibility(View.INVISIBLE);
                                }
                                
                            },null).execute(s.toString());
                        } else {
                            new SearchDialogsTask(new AsyncCallback<List<User>>() {
    
                                @Override
                                public void OnSuccess(List<User> str) {
                                    if(mBound) {
                                        if (!s.toString().equals(searchView.getText().toString()))
                                            return;
                                        
                                        recordsManager.addUsers(str);
                                        friendAdapter.setItems(str);
                                        dialogListView.setAdapter(friendAdapter);
                                        friendAdapter.notifyDataSetChanged();
                                        updatingView.setVisibility(View.INVISIBLE);
                                    }
                                }
    
                                @Override
                                public void OnError(ErrorCode errorCode) {
                                    updatingView.setVisibility(View.INVISIBLE);
                                }
                            }, VKApplication.getInstance().getAPILang()).execute(s.toString());
                        }
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        openNewDialog.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MessagesActivity.this, ComposeMessageActivity.class);
                startActivity(intent);
            }
        });
        dialogListView.addHeaderView(headerView);
        dialogListView.addFooterView(footerView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getApplicationContext().bindService(new Intent(MessagesActivity.this, RecordsProvider.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        searchView.setText("");
        if (mBound) {
            recordsManager.removeWatcher(recordsWatcher);
            getApplicationContext().unbindService(mConnection);
            mBound = false;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
    }

    private TabhostActivity getParentActivity() {
        return (TabhostActivity) getParent();
    }

    private void serviceConnected() {
        dialogAdapter = new DialogListAdapter(MessagesActivity.this, recordsManager.getDialogs());
        friendAdapter = new FriendListAdapter(MessagesActivity.this, recordsManager.getAllUsers());
        recordsManager.performFriendsUpdate();
        friendAdapter.setGenerateSections(false);
//        searchView.setDropDownVerticalOffset(10);
//        searchView.setAdapter(friendAdapter);

//        dialogListView.setOnItemClickListener(new OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> adapter, View arg1, int pos, long arg3) {
//                if (adapter == null || adapter.getItemAtPosition(pos) == null)
//                    return;
//                Intent intent = new Intent(MessagesActivity.this, ConversationActivity.class);
//                intent.putExtra(ConversationActivity.EXTRA_USERID, ((Message) adapter.getItemAtPosition(pos)).uid);
//                startActivity(intent);
//            }
//        });

        dialogListView.setAdapter(dialogAdapter);

        recordsWatcher = new RecordsWatcher() {

            @Override
            public void OnChangedRecords(final boolean needInvalidate) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        updatingView.setVisibility(View.INVISIBLE);
                        updatingDrawable.stop();
                        if (needInvalidate) {
                            dialogAdapter.notifyDataSetChanged();
                        }
                        int incomingMessagesCount = 0;
                        for (Message msg : recordsManager.getDialogs()) {
                            if (!msg.read_state && !msg.out)
                                incomingMessagesCount++;
                        }
                        getParentActivity().setNotifyMessagesCount(incomingMessagesCount);
                        if (recordsManager.getLastError() == ErrorCode.NoError) {
                            errorImage.setVisibility(View.INVISIBLE);
                            openNewDialog.setVisibility(View.VISIBLE);
                        }
                        if (recordsManager.getRequestList() != null) {
                            getParentActivity().setRequestsCount(recordsManager.getRequestList().size());
                        }
                    }
                });
            }

            @Override
            public void OnError(ErrorCode errorCode) {
                if (errorCode == ErrorCode.NetworkUnavailable) {
                    if (errorImage.getVisibility() == View.INVISIBLE)
                        UIUtil.showToast(MessagesActivity.this, getString(R.string.network_unavailable_show_cached));
                    Log.d(VKApplication.TAG, "Network unavailable. Show only cached dialogs");
                    errorImage.setVisibility(View.VISIBLE);
                    openNewDialog.setVisibility(View.INVISIBLE);
                    errorImage.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            UIUtil.showToast(MessagesActivity.this, getString(R.string.network_unavailable_show_cached));
                        }
                    });
                }
            }
        };

        recordsManager.addWatcher(recordsWatcher);

        dialogListView.postDelayed(new Runnable() {

            @Override
            public void run() {
                dialogListView.setOnScrollListener(new OnScrollListener() {
                    private int visibleThreshold = 10;
                    private int previousTotal = 0;
                    boolean loading = false;

                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                        dialogAdapter.scrollingNow.set(scrollState == OnScrollListener.SCROLL_STATE_FLING || scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if(searchMode) {
                            footerView.setVisibility(View.GONE);
                            return;
                        }
                        if (totalItemCount <= 5)
                            return;
                        if (loading) {
                            loadingAnimation.start();
                            footerView.setVisibility(View.VISIBLE);
                            if (totalItemCount > previousTotal) {
                                loading = false;
                                previousTotal = totalItemCount;
                            }
                        }
                        if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                            loading = true;
                            loadingAnimation.stop();
                            footerView.setVisibility(View.VISIBLE);
                            recordsManager.performDialogsUpdate(20, dialogAdapter.getCount(), MessagesActivity.this);
                        }
                    }
                });
            }
        }, 1500);

        recordsManager.performDialogsUpdate(20, 0, MessagesActivity.this);
        updatingView.setVisibility(View.VISIBLE);
        updatingDrawable.start();
        recordsManager.performRequestsUpdate();
        recordsManager.clearIncomingMessagesCount();

    }

    @Override
    public void onBackPressed() {
        // ImageCache.getInstance().clearCache();
        super.onBackPressed();
    }

    @Override
    public boolean onSearchRequested() {
        searchView.requestFocus();
        return false;
    }
    
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            LocalBinder binder = (LocalBinder) service;
            recordsManager = binder.getService();
            mBound = true;
            serviceConnected();
            recordsManager.addWatcher(recordsWatcher);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
