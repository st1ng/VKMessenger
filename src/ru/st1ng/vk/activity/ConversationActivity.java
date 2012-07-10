package ru.st1ng.vk.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.data.RecordsProvider.RecordsWatcher;
import ru.st1ng.vk.model.Attachment;
import ru.st1ng.vk.model.Attachment.Type;
import ru.st1ng.vk.model.ForwardMessageAttach;
import ru.st1ng.vk.model.GeoAttach;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.PhotoAttach;
import ru.st1ng.vk.model.ServerUploadFile;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.network.CaptchaNeededException;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.ChatGetTask;
import ru.st1ng.vk.network.async.GetMessageImageUploadServer;
import ru.st1ng.vk.network.async.MarkReadMessagesTask;
import ru.st1ng.vk.network.async.SaveUploadFilesTask;
import ru.st1ng.vk.network.async.SendMessageTask;
import ru.st1ng.vk.network.async.SendMessageTask.SendAsyncCallback;
import ru.st1ng.vk.network.async.UploadPhotoToServer;
import ru.st1ng.vk.network.async.UploadPhotoToServer.UploadAsyncCallback;
import ru.st1ng.vk.util.DateFormatUtil;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.ImageUtil;
import ru.st1ng.vk.util.UIUtil;
import ru.st1ng.vk.views.UserImageView;
import ru.st1ng.vk.views.WebImageView;
import ru.st1ng.vk.views.WebImageView.ImageListener;
import ru.st1ng.vk.views.adapter.MessagesListAdapter;
import ru.st1ng.vk.views.adapter.MessagesListAdapter.OnResendClickListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

public class ConversationActivity extends Activity {

    private int conversationId;
    ProgressDialog uploadDialog;
    TextView titleText;
    TextView userStatusText;
    UserImageView avatarImage;
    View backFrame;
    ImageView onlineImage;
    EditText sendField;
    Button sendButton;
    ImageView attachImage;
    LinearLayout attachMenu;
    TextView attachTakePhoto;
    TextView attachChoosePhoto;
    TextView attachGeo;
    ImageView attachTakePhotoMore;
    ImageView attachChoosePhotoMore;
    ImageView attachLocationMore;
    Button forwardButton;
    Button cancelButton;
    Button buttonGroup;
    Button deleteButton;

    String photoOutput;
    LinearLayout attachesLayout;
    View attachesList;

    RecordsProvider recordsProvider;
    RecordsWatcher recordsWatcher;
    private boolean mBound = false;

    ListView messagesList;
    MessagesListAdapter messagesAdapter;
    public static final String EXTRA_USERID = "userid";
    public static final String EXTRA_CHATID = "chatid";
    public static final String EXTRA_CHATID_MESSAGEID = "chatidmessage";
    public static final String EXTRA_FORWARD_MESSAGES = "forwardmessages";
    private static final int LOAD_AT_ONCE = 30;
    int currentCount = -1;
    private volatile boolean updating = false;

    ArrayList<Attachment> sendAttaches;

    Message currentChat;
    private boolean googleMapsSupported = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Hack for checking is google maps supported on device. If not, disable
        // Location adding
        try {
            Class.forName("com.google.android.maps.MapActivity");
        } catch (ClassNotFoundException e) {
            googleMapsSupported = false;
        }

        setContentView(R.layout.screen_conversation);

        conversationId = getIntent().getIntExtra(EXTRA_USERID, 0);
        if (conversationId == 0) {
            conversationId = getIntent().getIntExtra(EXTRA_CHATID, 0);
        }

        titleText = (TextView) findViewById(R.id.titleText);
        titleText.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);

        userStatusText = (TextView) findViewById(R.id.userStatusText);
        userStatusText.setTypeface(FontsUtil.Helvetica);
        avatarImage = (UserImageView) findViewById(R.id.avatarImage);
        backFrame = findViewById(R.id.backFrame);
        onlineImage = (ImageView) findViewById(R.id.onlineImage);
        messagesList = (ListView) findViewById(R.id.messagesList);
        sendField = (EditText) findViewById(R.id.sendField);
        sendField.setTypeface(FontsUtil.Helvetica);

        sendField.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButton();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        attachImage = (ImageView) findViewById(R.id.attachImage);
        attachMenu = (LinearLayout) findViewById(R.id.attachesMenu);
        attachTakePhoto = (TextView) findViewById(R.id.attachTakePhoto);
        attachTakePhoto.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        attachChoosePhoto = (TextView) findViewById(R.id.attachChoosePhoto);
        attachChoosePhoto.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        attachGeo = (TextView) findViewById(R.id.attachLocation);
        attachGeo.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        attachLocationMore = (ImageView) findViewById(R.id.attachLocationMore);
        attachesLayout = (LinearLayout) findViewById(R.id.attachesLayout);
        attachesList = findViewById(R.id.attachesList);
        attachTakePhotoMore = (ImageView) findViewById(R.id.attachTakePhotoMore);
        attachChoosePhotoMore = (ImageView) findViewById(R.id.attachChoosePhotoMore);
        forwardButton = (Button) findViewById(R.id.buttonForward);
        forwardButton.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        deleteButton = (Button) findViewById(R.id.buttonDelete);
        deleteButton.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        cancelButton = (Button) findViewById(R.id.buttonCancel);
        cancelButton.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        buttonGroup = (Button) findViewById(R.id.buttonGroup);

        buttonGroup.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConversationActivity.this, GroupInfoActivity.class);
                intent.putExtra(GroupInfoActivity.EXTRA_CHATID, conversationId);
                startActivityForResult(intent, 10);
            }
        });
        cancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (messagesAdapter != null)
                    messagesAdapter.clearCheckedItems();
            }
        });
        deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mBound) {
                    List<Integer> messages = new ArrayList<Integer>();
                    for (Long mes : messagesAdapter.getCheckedItems())
                        messages.add(mes.intValue());
                    recordsProvider.deleteMessages(conversationId, messages);
                }
            }
        });
        forwardButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConversationActivity.this, ComposeMessageActivity.class);
                StringBuilder forwarded = new StringBuilder();
                for (Long checked : messagesAdapter.getCheckedItems())
                    forwarded.append(checked + ",");
                if (forwarded.length() > 0)
                    forwarded.deleteCharAt(forwarded.length() - 1);
                intent.putExtra(EXTRA_FORWARD_MESSAGES, forwarded.toString());
                startActivity(intent);
                if (messagesAdapter != null)
                    messagesAdapter.clearCheckedItems();
            }
        });

        uploadDialog = new ProgressDialog(this);

        attachTakePhoto.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                attachMenu.setVisibility(View.INVISIBLE);
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                photoOutput = VKApplication.getInstance().getCameraDir() + "/" + System.currentTimeMillis() + ".jpg";
                File outFile = new File(photoOutput);
                if (!outFile.getParentFile().exists())
                    outFile.getParentFile().mkdirs();
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoOutput)));
                startActivityForResult(takePhotoIntent, 0);
            }
        });
        attachChoosePhoto.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                attachMenu.setVisibility(View.INVISIBLE);
                startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), 1);
            }
        });
        attachGeo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                attachMenu.setVisibility(View.INVISIBLE);
                startActivityForResult(new Intent(ConversationActivity.this, ChooseMapActivity.class), 2);
            }
        });
        attachLocationMore.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(ConversationActivity.this, ChooseMapActivity.class), 2);
            }
        });

        attachTakePhotoMore.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                photoOutput = VKApplication.getInstance().getCameraDir() + "/" + System.currentTimeMillis() + ".jpg";
                File outFile = new File(photoOutput);
                if (!outFile.getParentFile().exists())
                    outFile.getParentFile().mkdirs();
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoOutput)));
                startActivityForResult(takePhotoIntent, 0);
            }
        });
        attachChoosePhotoMore.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), 1);
            }
        });

        attachImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (sendAttaches == null) {
                    attachMenu.setVisibility(attachMenu.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                    attachImage.setImageResource(attachMenu.getVisibility() == View.VISIBLE ? R.drawable.ic_attach_pressed : R.drawable.ic_attach_selector);
                } else {
                    attachesList.setVisibility(attachesList.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                    attachMenu.setVisibility(View.INVISIBLE);
                    attachImage.setImageResource(R.drawable.ic_attach_pressed);
                }
            }
        });
        backFrame.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        recordsWatcher = new RecordsWatcher() {

            @Override
            public void OnError(ErrorCode errorCode) {
                if (errorCode.equals(ErrorCode.MessagesSuccessfullyDeleted)) {
                    UIUtil.showToast(ConversationActivity.this, getString(R.string.messages_deleted));
                    messagesAdapter.getCheckedItems().clear();
                    onItemsCheckChanged();
                }

            }

            @Override
            public void OnChangedRecords(boolean needInvalidate) {
                if (needInvalidate) {
                    updateMessages(false);
                }
            }
        };
        if (getIntent().getStringExtra(EXTRA_FORWARD_MESSAGES) != null) {
            if (sendAttaches == null)
                sendAttaches = new ArrayList<Attachment>();
            final ForwardMessageAttach attach = new ForwardMessageAttach();
            attach.forwardMessages = getIntent().getStringExtra(EXTRA_FORWARD_MESSAGES);
            attachesList.setVisibility(View.VISIBLE);
            sendAttaches.add(attach);
            updateSendButton();
            View v = getLayoutInflater().inflate(R.layout.widget_attach_remove, null);
            ImageView iv = (ImageView) v.findViewById(R.id.attachImage);
            v.findViewById(R.id.forwardedText).setVisibility(View.VISIBLE);
            v.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    sendAttaches.remove(attach);
                    attachesLayout.removeView(v);
                    if (sendAttaches.size() == 0) {
                        sendAttaches = null;
                        updateSendButton();
                        attachesList.setVisibility(View.GONE);
                        attachImage.setImageResource(R.drawable.ic_attach_selector);
                    }
                    updateLocationVisibility();
                }
            });
            iv.setScaleType(ScaleType.FIT_XY);
            LayoutParams params = new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.image_size), (int) getResources().getDimension(R.dimen.image_size));
            params.setMargins(10, 0, 0, 0);
            v.setLayoutParams(params);
            iv.setImageResource(R.drawable.bg_attach_empty);
            attachesLayout.addView(v, 0);
            attachImage.setImageResource(R.drawable.ic_attach_pressed);
        }

        if (!googleMapsSupported) {
            attachLocationMore.setVisibility(View.GONE);
            attachGeo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        if (mBound) {
            recordsProvider.removeWatcher(recordsWatcher);
            unbindService(mConnection);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        bindService(new Intent(ConversationActivity.this, RecordsProvider.class), mConnection, BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        conversationId = intent.getIntExtra(EXTRA_USERID, 0);
        if (conversationId == 0) {
            conversationId = intent.getIntExtra(EXTRA_CHATID, 0);
        }
        super.onNewIntent(intent);
    }

    long lastUpdatedTime;
    private Message lastMessage;

    private void updateMessages(final boolean initLoad) {
        if (mBound) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    populateInterface(initLoad);

                    if (currentCount == messagesAdapter.getCount())
                        return;
                    int offset = 0;
                    if (currentCount > 0 && !initLoad) {
                        currentCount = messagesList.getFirstVisiblePosition() + (messagesAdapter.getCount() - currentCount);
                        View top = messagesList.getChildAt(0);
                        offset = top == null ? 0 : top.getTop();
                        updating = true;
                        new Handler().postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                updating = false;
                            }
                        }, 100);
                    }
                    messagesAdapter.notifyDataSetChanged();
                    if (currentCount >= 0 && !initLoad) {
                        messagesList.setSelectionFromTop(currentCount, offset);
                        Log.d(VKApplication.TAG, "Scrolling to " + currentCount);
                        currentCount = -1;

                    }
                    int size = messagesAdapter.getCount();

                    final ArrayList<Message> markReadMessages = new ArrayList<Message>();
                    for (int i = 0; i < size; i++) {
                        final Message message = messagesAdapter.getItem(i);
                        if (!message.read_state && !message.out)
                            markReadMessages.add(message);
                    }
                    if (markReadMessages.size() > 0)
                        new Handler().postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                if (mBound) {
                                    for (Message message : markReadMessages)
                                        message.read_state = true;
                                    new MarkReadMessagesTask(null).execute(markReadMessages.toArray(new Message[0]));
                                    messagesAdapter.notifyDataSetChanged();
                                }
                            }
                        }, 5000);

                    if (messagesAdapter.getCount() > 0) {
                        if (lastMessage == null || !lastMessage.equals(messagesAdapter.getItem(messagesAdapter.getCount() - 1))) {
                            // messagesList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                            // messagesAdapter.notifyDataSetChanged();
                            // messagesList.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
                            // messagesList.setSelectionFromTop(messagesAdapter.getCount()-1,
                            // y)
                            if (Build.VERSION.SDK_INT < 8)
                                messagesList.setSelection(messagesAdapter.getCount() - 1);
                            // else
                            // messagesList.smoothScrollToPosition(messagesAdapter.getCount());
                            // messagesList.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
                        }
                        lastMessage = messagesAdapter.getItem(messagesAdapter.getCount() - 1);
                    }
                }

            });
        }
    }

    private void populateInterface(boolean init) {
        if (conversationId > 0 || conversationId <= -200000000) {
            User currentUser = recordsProvider.getUserById(conversationId, true);
            if (currentUser == null) {
                titleText.setText(R.string.loading);
                return;
            }
            titleText.setText(currentUser.first_name + " " + currentUser.last_name);
            avatarImage.setVisibility(View.VISIBLE);
            buttonGroup.setVisibility(View.INVISIBLE);
            avatarImage.setUser(currentUser);
            avatarImage.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    Intent info = new Intent(ConversationActivity.this, UserInfoActivity.class);
                    info.putExtra(UserInfoActivity.EXTRA_USERID, conversationId);
                    startActivityForResult(info, 20);
                }
            });
            onlineImage.setVisibility(currentUser.online ? View.VISIBLE : View.INVISIBLE);
            userStatusText.setText(getStatusText(currentUser));
            if (currentUser.writing) {
                currentUser.writing = false;
                lastUpdatedTime = System.currentTimeMillis();
                userStatusText.setText(R.string.writing);
                userStatusText.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (System.currentTimeMillis() - lastUpdatedTime > 5000)
                            userStatusText.setText("");
                    }
                }, 7000);
            }
        } else {
            currentChat = recordsProvider.getChatById(conversationId);
            avatarImage.setVisibility(View.INVISIBLE);
            buttonGroup.setVisibility(View.VISIBLE);
            if (init)
                new ChatGetTask(new AsyncCallback<Message>() {

                    @Override
                    public void OnSuccess(Message str) {
                        currentChat = str;
                        titleText.setText(currentChat.title);
                    }

                    @Override
                    public void OnError(ErrorCode errorCode) {

                    }
                }).execute(-conversationId);
            if (currentChat == null) {
                titleText.setText(R.string.loading);
                return;
            }
            titleText.setText(currentChat.title);
            if (currentChat != null && currentChat.chat_users != null)
                buttonGroup.setText("" + (currentChat.chat_users.size()));
        }
    }

    private String getStatusText(User user) {
        if (user.online) {
            return "";
        }
        if (user.last_seen > 0) {
            Calendar lastSeen = Calendar.getInstance();
            Calendar current = Calendar.getInstance();
            lastSeen.setTimeInMillis(user.last_seen * 1000);

            if (lastSeen.get(Calendar.YEAR) == current.get(Calendar.YEAR)) {
                if (lastSeen.get(Calendar.MONTH) == current.get(Calendar.MONTH)) {
                    if (lastSeen.get(Calendar.DATE) == current.get(Calendar.DATE) - 1)
                        return user.first_name + " " + getString(user.sex == 1 ? R.string.was_female : R.string.was_male) + " " + getString(R.string.yesterday) + " " + getString(R.string.at_time) + " "
                                + DateFormatUtil.getTimeFormat(this).format(lastSeen.getTime());

                    if (lastSeen.get(Calendar.DATE) == current.get(Calendar.DATE)) {
                        if (lastSeen.get(Calendar.HOUR_OF_DAY) == current.get(Calendar.HOUR_OF_DAY)) {
                            return user.first_name + " " + getString(user.sex == 1 ? R.string.was_female : R.string.was_male) + " " + (current.get(Calendar.MINUTE) - lastSeen.get(Calendar.MINUTE)) + " " + getString(R.string.minutes) + " "
                                    + getString(R.string.ago);
                        }
                        return user.first_name + " " + getString(user.sex == 1 ? R.string.was_female : R.string.was_male) + " " + getString(R.string.today) + " " + getString(R.string.at_time) + " "
                                + DateFormatUtil.getTimeFormat(this).format(lastSeen.getTime());
                    }
                }

            }
            return user.first_name + " " + getString(user.sex == 1 ? R.string.was_female : R.string.was_male) + " " + DateFormatUtil.getDateFormat(this).format(lastSeen.getTime());
        }
        return "";
    }

    private void serviceConnected() {
        sendButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (sendField.getText().toString().trim().length() == 0 && sendAttaches == null)
                    return;

                if (sendAttaches != null) {
                    for (int i = 0; i < sendAttaches.size(); i++) {
                        if (sendAttaches.get(i).getType() != Type.Geo && sendAttaches.get(i).getType() != Type.Forward) {
                            uploadAttachesAndSend(sendAttaches, sendField.getText().toString());
                            sendField.setText("");
                            return;
                        }
                    }
                }

                String geoCoords = null;
                String forwardedMessages = null;
                if (sendAttaches != null)
                    for (int i = 0; i < sendAttaches.size(); i++) {
                        if (sendAttaches.get(i).getType() == Type.Geo) {
                            geoCoords = ((GeoAttach) sendAttaches.get(i)).latitude + "," + ((GeoAttach) sendAttaches.get(i)).longtitude;
                            continue;
                        }
                        if (sendAttaches.get(i).getType() == Type.Forward) {
                            forwardedMessages = ((ForwardMessageAttach) sendAttaches.get(i)).forwardMessages;
                            continue;
                        }
                    }
                sendMessage(sendField.getText().toString(), null, geoCoords, forwardedMessages,sendAttaches);
                sendField.setText("");

            }
        });
        messagesAdapter = new MessagesListAdapter(this, recordsProvider.getMessagesForDialog(conversationId, 0, LOAD_AT_ONCE, this, 0), conversationId < 0 && conversationId > -200000000, this);
        messagesAdapter.setOnResendClickListener(new OnResendClickListener() {

            @Override
            public void OnResend(final Message msg) {
                final Message resend = msg;
                AlertDialog.Builder dialog = new AlertDialog.Builder(ConversationActivity.this);
                dialog.setTitle(R.string.error).setMessage(R.string.message_not_sent).setPositiveButton(R.string.resend, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mBound) {
                            Message pending = recordsProvider.getPendingMessage(conversationId, msg.guid);
                            Message resendMessage = pending != null ? pending : msg;
                            recordsProvider.deletePendingMessage(conversationId, msg.guid);

                            if (resendMessage.attachments != null) {
                                for (int i = 0; i < resendMessage.attachments.size(); i++) {
                                    if (resendMessage.attachments.get(i).getType() != Type.Geo && msg.attachments.get(i).getType() != Type.Forward) {
                                        uploadAttachesAndSend(resendMessage.attachments, resendMessage.body);
                                        return;
                                    }
                                }
                            }

                            String geoCoords = null;
                            String forwardedMessages = null;
                            if (resendMessage.attachments != null)
                                for (int i = 0; i < msg.attachments.size(); i++) {
                                    if (resendMessage.attachments.get(i).getType() == Type.Geo) {
                                        geoCoords = ((GeoAttach) msg.attachments.get(i)).latitude + "," + ((GeoAttach) resendMessage.attachments.get(i)).longtitude;
                                        continue;
                                    }
                                    if (resendMessage.attachments.get(i).getType() == Type.Forward) {
                                        forwardedMessages = ((ForwardMessageAttach) resendMessage.attachments.get(i)).forwardMessages;
                                        continue;
                                    }
                                }

                            sendMessage(resendMessage.body, null, geoCoords, forwardedMessages,resendMessage.attachments);
                        }
                    }
                }).setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mBound) {
                            recordsProvider.deletePendingMessage(conversationId, msg.guid);
                        }
                    }
                }).show();
            }
        });
        messagesList.setAdapter(messagesAdapter);
        messagesList.setStackFromBottom(true);
        messagesList.setSelection(messagesAdapter.getCount() - 1);
        updateMessages(true);
        SparseBooleanArray ch = messagesList.getCheckedItemPositions();

        messagesList.setOnScrollListener(new OnScrollListener() {
            private int visibleThreshold = 5;
            private int previousTotal = 0;
            AtomicBoolean loading = new AtomicBoolean(false);

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0)
                    loading.set(false);
                if (totalItemCount <= 2)
                    return;
                if (loading.get()) {
                    if (totalItemCount > previousTotal) {
                        loading.set(false);
                        previousTotal = totalItemCount;
                    }
                }
                if (!loading.get() && firstVisibleItem < visibleThreshold && currentCount <= 0 && !updating) {
                    Log.d(VKApplication.TAG, "Scroll reached at " + firstVisibleItem + " With total " + totalItemCount + " " + messagesAdapter.getCount());
                    loading.set(true);

                    int count = 0;
                    List<Message> items = messagesAdapter.getItems();
                    int size = items.size();
                    for (int i = 0; i < size; i++) {
                        if (items.get(i).fromDb)
                            continue;
                        count++;
                    }
                    currentCount = messagesAdapter.getCount();
                    recordsProvider.getMessagesForDialog(conversationId, count, LOAD_AT_ONCE, ConversationActivity.this, messagesAdapter.getCount());
                }
            }
        });

        recordsProvider.clearIncomingMessagesCount();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            LocalBinder binder = (LocalBinder) service;
            recordsProvider = binder.getService();
            mBound = true;
            serviceConnected();
            recordsProvider.addWatcher(recordsWatcher);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 10 || requestCode==20) && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra(GroupInfoActivity.EXTRA_LEAVE, false)) {
                ConversationActivity.this.finish();
                return;
            }
        }
        if (requestCode == 0 && resultCode == RESULT_OK) {
            if (photoOutput == null)
                return;
            File photoFile = new File(photoOutput);
            if (!photoFile.exists())
                return;

            if (sendAttaches == null)
                sendAttaches = new ArrayList<Attachment>();
            attachesList.setVisibility(View.VISIBLE);

            final PhotoAttach attach = new PhotoAttach();
            attach.photo_src = photoOutput;
            attach.photo_src_big = photoOutput;
            sendAttaches.add(attach);
            updateSendButton();

            View v = getLayoutInflater().inflate(R.layout.widget_attach_remove, null);
            ImageView iv = (ImageView) v.findViewById(R.id.attachImage);
            v.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    sendAttaches.remove(attach);
                    attachesLayout.removeView(v);
                    if (sendAttaches.size() == 0) {
                        sendAttaches = null;
                        updateSendButton();
                        attachesList.setVisibility(View.GONE);
                        attachImage.setImageResource(R.drawable.ic_attach_selector);
                    }
                    updateLocationVisibility();
                }
            });
            iv.setScaleType(ScaleType.FIT_XY);
            LayoutParams params = new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.image_size), (int) getResources().getDimension(R.dimen.image_size));
            params.setMargins(10, 0, 0, 0);
            v.setLayoutParams(params);
            Bitmap bmp = ImageUtil.decodeBitmapScaledSquare(photoOutput, 100);
            bmp = ImageUtil.processRoundedCornerBitmap(bmp, 10);
            attach.bitmap = bmp;
            iv.setImageBitmap(bmp);
            attachesLayout.addView(v, 0);
            attachImage.setImageResource(R.drawable.ic_attach_pressed);
        } else if (requestCode == 1 && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();

            if (sendAttaches == null)
                sendAttaches = new ArrayList<Attachment>();
            attachesList.setVisibility(View.VISIBLE);

            final PhotoAttach attach = new PhotoAttach();
            attach.photo_src = filePath;
            attach.photo_src_big = filePath;
            sendAttaches.add(attach);
            updateSendButton();

            View v = getLayoutInflater().inflate(R.layout.widget_attach_remove, null);
            ImageView iv = (ImageView) v.findViewById(R.id.attachImage);
            v.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    sendAttaches.remove(attach);
                    attachesLayout.removeView(v);
                    if (sendAttaches.size() == 0) {
                        sendAttaches = null;
                        updateSendButton();
                        attachesList.setVisibility(View.GONE);
                        attachImage.setImageResource(R.drawable.ic_attach_selector);
                    }
                    updateLocationVisibility();
                }
            });
            iv.setScaleType(ScaleType.FIT_XY);
            LayoutParams params = new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.image_size), (int) getResources().getDimension(R.dimen.image_size));
            params.setMargins(10, 0, 0, 0);
            v.setLayoutParams(params);

            Bitmap bmp = ImageUtil.decodeBitmapScaledSquare(filePath, 100);
            bmp = ImageUtil.processRoundedCornerBitmap(bmp, 10);
            attach.bitmap = bmp;
            iv.setImageBitmap(bmp);
            attachesLayout.addView(v, 0);
            attachImage.setImageResource(R.drawable.ic_attach_pressed);
        } else if (resultCode == RESULT_OK && requestCode == 2) {
            if (sendAttaches == null) {
                sendAttaches = new ArrayList<Attachment>();
            }
            attachesList.setVisibility(View.VISIBLE);

            final GeoAttach attach = new GeoAttach();
            attach.latitude = data.getIntExtra(ChooseMapActivity.LATITUDE, 0) / 1000000.0f;
            attach.longtitude = data.getIntExtra(ChooseMapActivity.LONGTITUDE, 0) / 1000000.0f;
            sendAttaches.add(attach);
            updateSendButton();

            View v = getLayoutInflater().inflate(R.layout.widget_attach_remove, null);
            WebImageView iv = (WebImageView) v.findViewById(R.id.attachImage);
            v.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    sendAttaches.remove(attach);
                    attachesLayout.removeView(v);
                    if (sendAttaches.size() == 0) {
                        sendAttaches = null;
                        updateSendButton();
                        attachesList.setVisibility(View.GONE);
                        attachImage.setImageResource(R.drawable.ic_attach_selector);
                    }
                    updateLocationVisibility();
                }
            });
            iv.setScaleType(ScaleType.FIT_XY);
            LayoutParams params = new LinearLayout.LayoutParams((int) getResources().getDimension(R.dimen.image_size), (int) getResources().getDimension(R.dimen.image_size));
            params.setMargins(10, 0, 0, 0);
            v.setLayoutParams(params);
            iv.setImageListener(new ImageListener() {

                @Override
                public void onImageLoaded(WebImageView arg0, Bitmap bmp, String arg2) {
                    bmp = ImageUtil.processRoundedCornerBitmap(bmp, 10);
                    attach.bitmap = bmp;
                    arg0.setImageBitmap(bmp);
                }
            });

            if (attach.bitmap != null)
                iv.setImageBitmap(attach.bitmap);
            else
                iv.setImageFromURL(attach.getUrl());
            attachesLayout.addView(v, 0);
            attachImage.setImageResource(R.drawable.ic_attach_pressed);
        }
        updateLocationVisibility();
    };

    private void uploadAttachesAndSend(List<Attachment> attachments, String message) {
        uploadAttachesAndSend(attachments, message, null,null);
    }
    
    private void uploadAttachesAndSend(final List<Attachment> attachments,final String message, String captchaSid, String captchaKey) {
        sendButton.setEnabled(false);
        uploadDialog.setMax(100);
        uploadDialog.setIndeterminate(false);
        uploadDialog.setTitle(R.string.uploading);
        uploadDialog.setMessage("0%. File 1/" + sendAttaches.size());
        uploadDialog.setCancelable(false);
        uploadDialog.show();
        new GetMessageImageUploadServer(new AsyncCallback<String>() {

            @Override
            public void OnSuccess(String str) {
                final ArrayList<String> sendPhotoIds = new ArrayList<String>();

                final ArrayList<String> files = new ArrayList<String>();
                files.add(str);
                for (Attachment attach : attachments) {
                    if (attach.getType() == Type.Photo) {
                        PhotoAttach att = (PhotoAttach) attach;
                        files.add(att.photo_src);
                    }
                }
                new UploadPhotoToServer(new UploadAsyncCallback<ServerUploadFile[]>() {

                    @Override
                    public void OnSuccess(ServerUploadFile[] str) {
                        for (int i = 0; i < str.length; i++) {
                            new SaveUploadFilesTask(new AsyncCallback<String>() {

                                @Override
                                public void OnSuccess(String str) {
                                    synchronized (sendPhotoIds) {
                                        sendPhotoIds.add(str);
                                        if (sendPhotoIds.size() == (files.size() - 1)) {
                                            sendButton.setEnabled(true);
                                            uploadDialog.cancel();
                                            String geoCoords = null;
                                            String forwardedMessages = null;
                                            for (int i = 0; i < sendAttaches.size(); i++) {
                                                if (attachments.get(i).getType() == Type.Geo) {
                                                    geoCoords = ((GeoAttach) attachments.get(i)).latitude + "," + ((GeoAttach) attachments.get(i)).longtitude;
                                                    continue;
                                                }
                                                if (attachments.get(i).getType() == Type.Forward) {
                                                    forwardedMessages = ((ForwardMessageAttach) attachments.get(i)).forwardMessages;
                                                    continue;
                                                }
                                            }
                                            sendMessage(message, sendPhotoIds.toArray(new String[sendPhotoIds.size()]), geoCoords, forwardedMessages, null,null,sendAttaches);
                                            sendAttaches = null;
                                            updateSendButton();

                                            sendPhotoIds.clear();
                                        }
                                    }
                                }

                                @Override
                                public void OnError(ErrorCode errorCode) {
                                    sendButton.setEnabled(true);
                                }
                            }).execute(str[i]);
                        }
                    }

                    @Override
                    public void OnError(ErrorCode errorCode) {
                        sendButton.setEnabled(true);
                    }

                    @Override
                    public void OnProgress(int percent, int fileCount) {
                        uploadDialog.setProgress(percent);
                        uploadDialog.setMessage("" + percent + "%. File " + fileCount + "/" + sendAttaches.size());
                    }
                }).execute(files.toArray(new String[files.size()]));
            }

            @Override
            public void OnError(ErrorCode errorCode) {
                sendButton.setEnabled(true);
            }
        }).execute();
    }

    private void sendMessage(String messageText, String[] attachIds, String geoCoords, String forwardedMessages, List<Attachment> attachments) {
        sendMessage(messageText, attachIds, geoCoords, forwardedMessages, null, null, attachments);
    }
    
    private void sendMessage(String messageText, String[] attachIds, String geoCoords, String forwardedMessages, final String captchaSid, final String captchaKey,List<Attachment> attachments) {
        String[] args;
        if (attachIds != null) {
            args = new String[3 + attachIds.length];
            args[0] = messageText;
            args[1] = conversationId + "";
            for (int i = 0; i < attachIds.length; i++) {
                args[i + 2] = attachIds[i];
            }
        } else {
            args = new String[] { messageText, conversationId + "" };
        }

        Message pendingMessage = new Message();
        pendingMessage.date = System.currentTimeMillis() / 1000;
        pendingMessage.uid = conversationId;
        if (conversationId < 0 && conversationId > -200000000)
            pendingMessage.chat_id = -conversationId;
        pendingMessage.body = messageText;
        pendingMessage.out = true;
        pendingMessage.pendingSend = true;
        pendingMessage.guid = String.valueOf(new Random().nextLong());
        if (attachments != null && attachments.size() > 0) {
            pendingMessage.attachments = new ArrayList<Attachment>();
            for (Attachment attach : attachments)
                pendingMessage.attachments.add(attach);
        }
        recordsProvider.addPendingMessage(pendingMessage);
        messagesAdapter.notifyDataSetChanged();
        messagesList.setSelection(messagesAdapter.getCount() - 1);
        for (int i = 0; i < attachesLayout.getChildCount() - 2; i++)
            attachesLayout.removeViewAt(i);
        attachesList.setVisibility(View.GONE);
        attachImage.setImageResource(R.drawable.ic_attach_selector);
        new SendMessageTask(new SendAsyncCallback<Message>() {

            @Override
            public void OnSuccess(Message sent) {
                if (mBound) {

                    // recordsProvider.addSentMessage(sent);
                    messagesAdapter.notifyDataSetChanged();
                    messagesList.setSelection(messagesList.getCount() - 1);

                }
            }

            @Override
            public void OnError(ErrorCode errorCode) {
            }

            @Override
            public void OnSendError(JsonParseException error) {
                if(error.getErrorCode().equals(ErrorCode.FloodControl)) {
                    recordsProvider.addNetworkErrorMessage(conversationId, error.getParam());
                    return;
                }
                if (error instanceof CaptchaNeededException) {
                    final CaptchaNeededException exc = (CaptchaNeededException) error;
                    if (mBound) {
                        recordsProvider.addNetworkErrorMessage(conversationId, exc.getMsg().guid);
                        final String sid = exc.getCaptchaSid();
                        String image = exc.getCaptchaImg();
                        View v = getLayoutInflater().inflate(R.layout.dialog_captcha, null);
                        final WebImageView captchaImage = (WebImageView) v.findViewById(R.id.captchaImage);
                        captchaImage.setImageListener(new ImageListener() {
                            
                            @Override
                            public void onImageLoaded(final WebImageView im, final Bitmap bm, String url) {
                                captchaImage.postDelayed(new Runnable() {
                                    
                                    @Override
                                    public void run() {
                                        im.setImageBitmap(bm);
                                    }
                                }, 500);
                            }
                        });
                        captchaImage.setImageFromURL(image);
                        final EditText captchaKey = (EditText) v.findViewById(R.id.captchaKey);
                        
                        AlertDialog.Builder dialog = new AlertDialog.Builder(ConversationActivity.this);
                        dialog.setView(v).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Message msg = exc.getMsg();
                                if (mBound) {
                                    Message pending = recordsProvider.getPendingMessage(conversationId, msg.guid);
                                    Message resendMessage = pending != null ? pending : msg;
                                    recordsProvider.deletePendingMessage(conversationId, resendMessage.guid);

                                    if (resendMessage.attachments != null) {
                                        for (int i = 0; i < resendMessage.attachments.size(); i++) {
                                            if (resendMessage.attachments.get(i).getType() != Type.Geo && resendMessage.attachments.get(i).getType() != Type.Forward) {
                                                uploadAttachesAndSend(resendMessage.attachments, resendMessage.body, sid, captchaKey.getText().toString());
                                                return;
                                            }
                                        }
                                    }

                                    String geoCoords = null;
                                    String forwardedMessages = null;
                                    if (resendMessage.attachments != null)
                                        for (int i = 0; i < resendMessage.attachments.size(); i++) {
                                            if (resendMessage.attachments.get(i).getType() == Type.Geo) {
                                                geoCoords = ((GeoAttach) resendMessage.attachments.get(i)).latitude + "," + ((GeoAttach) resendMessage.attachments.get(i)).longtitude;
                                                continue;
                                            }
                                            if (resendMessage.attachments.get(i).getType() == Type.Forward) {
                                                forwardedMessages = ((ForwardMessageAttach) resendMessage.attachments.get(i)).forwardMessages;
                                                continue;
                                            }
                                        }

                                    sendMessage(resendMessage.body, null, geoCoords, forwardedMessages, sid, captchaKey.getText().toString(), resendMessage.attachments);
                                }
                            }
                        }).show();
                        exc.getMsg();
                    }
                }
            }

            @Override
            public void OnNetworkError(String guid) {
                if (mBound) {
                    recordsProvider.addNetworkErrorMessage(conversationId, guid);
                }
            }
        }, forwardedMessages, geoCoords, pendingMessage.guid, captchaSid, captchaKey).execute(args);

        if (sendAttaches != null) {
            sendAttaches.clear();
            sendAttaches = null;
            updateSendButton();
            updateLocationVisibility();
        }
    }

    private void updateLocationVisibility() {
        if (!googleMapsSupported) {
            findViewById(R.id.attachLocation).setVisibility(View.GONE);
            findViewById(R.id.attachLocationMore).setVisibility(View.GONE);
            return;
        }

        if (sendAttaches == null) {
            findViewById(R.id.attachLocation).setVisibility(View.VISIBLE);
            findViewById(R.id.attachLocationMore).setVisibility(View.VISIBLE);
            return;
        }

        for (int i = 0; i < sendAttaches.size(); i++) {
            if (sendAttaches.get(i).getType() == Type.Geo) {
                findViewById(R.id.attachLocation).setVisibility(View.GONE);
                findViewById(R.id.attachLocationMore).setVisibility(View.GONE);
                return;
            }
        }
        findViewById(R.id.attachLocation).setVisibility(View.VISIBLE);
        findViewById(R.id.attachLocationMore).setVisibility(View.VISIBLE);

    }

    public void onItemsCheckChanged() {
        if (messagesAdapter.getCheckedItems().size() > 0) {
            findViewById(R.id.infoFrame).setVisibility(View.INVISIBLE);
            findViewById(R.id.messageOptions).setVisibility(View.VISIBLE);
            forwardButton.setText(getString(R.string.forward) + " " + messagesAdapter.getCheckedItems().size() + "");
            deleteButton.setText(getString(R.string.delete) + " " + messagesAdapter.getCheckedItems().size() + "");
        } else {
            findViewById(R.id.infoFrame).setVisibility(View.VISIBLE);
            findViewById(R.id.messageOptions).setVisibility(View.INVISIBLE);
        }
    }

    private void updateSendButton() {
        if (sendField.getText().toString().trim().length() == 0 && sendAttaches == null) {
            sendButton.setEnabled(false);
            return;
        }
        sendButton.setEnabled(true);
    }

}
