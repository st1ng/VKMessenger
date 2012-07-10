package ru.st1ng.vk.views;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.util.HttpUtil;
import ru.st1ng.vk.util.ImageUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class UserImageView extends ImageView {

    private static String photoDir = VKApplication.getInstance().getAvatarsDir();
    private AtomicBoolean scrollingNow = new AtomicBoolean(false);

    // Use threadPool for decoding and downloading bitmaps
    // 3 threads will be more, than enought
    private static ExecutorService threadPool = Executors.newFixedThreadPool(3);

    public UserImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public UserImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UserImageView(Context context) {
        super(context);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    public void setScrollingIndicator(AtomicBoolean scrollingIndicator) {
        this.scrollingNow = scrollingIndicator;
    }

    public void setUsersFromDialog(final Message dialog) {
        setTag(dialog);
        if (dialog == null) {
            setImageResource(R.drawable.im_nophoto);
            return;
        }
        if (dialog.dialogBitmap != null) {
            setImageBitmap(dialog.dialogBitmap);
            return;
        }
        setImageResource(R.drawable.im_nophoto);
        if (dialog.chat_users == null)
            return;
        postDelayed(new Runnable() {

            @Override
            public void run() {

                if (getTag() != dialog)
                    return;

                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {
                        while (scrollingNow.get())
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                            }
                        if(getTag()!=dialog)
                            return;

                        final Bitmap[] bitmaps = new Bitmap[dialog.chat_users.size()];
                        for (int i = 0; i < bitmaps.length; i++) {
                            final User user = dialog.chat_users.get(i);
                            if (user == null)
                                continue;
                            if (user.photo_bitmap == null)
                                if (ImageCache.getInstance().isPhotoPresentForUser(user)) {
                                    user.photo_bitmap = ImageCache.getInstance().getPhotoForUser(user);
                                } else {
                                    user.photo_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.im_nophoto);
                                    final int j = i;
                                    threadPool.execute(new Runnable() {

                                        @Override
                                        public void run() {
                                            // obtain an input stream of the
                                            // image
                                            InputStream is = null;
                                            try {
                                                is = (InputStream) HttpUtil.getInputStream(user.photo);
                                            } catch (Exception e) {
                                                return;
                                            }

                                            final InputStream imgStream = is;

                                            Bitmap bm = BitmapFactory.decodeStream(imgStream);
                                            int size = (int) getContext().getResources().getDimension(R.dimen.avatar_size);
                                            bm = Bitmap.createScaledBitmap(bm, size, size, false);
                                            final Bitmap rounded = ImageUtil.processRoundedCornerBitmap(bm, bm.getWidth() / 10);
                                            ImageCache.getInstance().storeBitmapForUser(rounded, user, photoDir);

                                            if (UserImageView.this.getHandler() != null) {
                                                UserImageView.this.post(new Runnable() {
                                                    public void run() {
                                                        user.photo_bitmap = rounded;
                                                        bitmaps[j] = user.photo_bitmap;
                                                        dialog.dialogBitmap = ImageUtil.processSeveralBitmapsIntoOne(bitmaps);
                                                        if (getTag() == dialog) {
                                                            setImageBitmap(dialog.dialogBitmap);
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            bitmaps[i] = user.photo_bitmap;
                        }
                        dialog.dialogBitmap = ImageUtil.processSeveralBitmapsIntoOne(bitmaps);
                        if (UserImageView.this.getHandler() != null) {
                            UserImageView.this.post(new Runnable() {
                                public void run() {
                                    if(getTag()==dialog) {
                                        setImageBitmap(dialog.dialogBitmap);
                                    }
                                }
                            });
                        }
                    }
                });

            }
        }, 500);

    }

    public void setUser(final User user) {
        setTag(user);
        if (user == null) {
            setImageResource(R.drawable.im_nophoto);
            return;
        }
        if (user.photo_bitmap != null) {
            setImageBitmap(user.photo_bitmap);
            return;
        }
        setImageResource(R.drawable.im_nophoto);
        postDelayed(new Runnable() {

            @Override
            public void run() {
                if (getTag() != user)
                    return;

                if (ImageCache.getInstance().isPhotoPresentForUser(user)) {
                    threadPool.execute(new Runnable() {

                        @Override
                        public void run() {
                            while (scrollingNow.get())
                                try {
                                    Thread.sleep(300);
                                } catch (InterruptedException e) {
                                }
                            if(getTag()!=user)
                                return;
                            user.photo_bitmap = ImageCache.getInstance().getPhotoForUser(user);
                            post(new Runnable() {

                                @Override
                                public void run() {
                                    if (getTag() != user)
                                        return;
                                    setImageBitmap(user.photo_bitmap);
                                }
                            });

                        }
                    });
                    return;
                }
                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {
                        while (scrollingNow.get())
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                            }
                        if(getTag()!=user)
                            return;
                        // obtain an input stream of the image
                        InputStream is = null;
                        try {
                            is = (InputStream) HttpUtil.getInputStream(user.photo);
                        } catch (Exception e) {
                            return;
                        }

                        final InputStream imgStream = is;
                        Options opts = new Options();
                        opts.inSampleSize = 2;
                        Bitmap bm = BitmapFactory.decodeStream(imgStream);
                        int size = (int) getContext().getResources().getDimension(R.dimen.avatar_size);
                        Log.d(VKApplication.TAG, "Decoding avatar. Size " + size);
                        bm = Bitmap.createScaledBitmap(bm, size, size, false);
                        if (bm == null)
                            return;

                        final Bitmap rounded = ImageUtil.processRoundedCornerBitmap(bm, bm.getWidth() / 10);
                        ImageCache.getInstance().storeBitmapForUser(rounded, user, photoDir);
                        if (UserImageView.this.getHandler() != null) {
                            UserImageView.this.post(new Runnable() {
                                public void run() {
                                    user.photo_bitmap = rounded;
                                    if (getTag() == user) {
                                        setImageBitmap(user.photo_bitmap);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }, 500);

    }
}