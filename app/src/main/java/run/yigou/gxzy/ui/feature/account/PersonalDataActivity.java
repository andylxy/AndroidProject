package run.yigou.gxzy.ui.feature.account;

import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import run.yigou.gxzy.R;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.data.remote.api.UpdateImageApi;
import run.yigou.gxzy.network.glide.GlideApp;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.ui.dialog.AddressDialog;
import run.yigou.gxzy.ui.dialog.InputDialog;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import run.yigou.gxzy.data.remote.model.FileContentResolver;
import com.hjq.widget.layout.SettingBar;
import run.yigou.gxzy.ui.feature.media.activity.ImageCropActivity;
import run.yigou.gxzy.ui.feature.media.activity.ImagePreviewActivity;
import run.yigou.gxzy.ui.feature.media.activity.ImageSelectActivity;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/04/20
 *    desc   : ????
 */
public final class PersonalDataActivity extends AppActivity {

    private ViewGroup mAvatarLayout;
    private ImageView mAvatarView;
    private SettingBar mIdView;
    private SettingBar mNameView;
    private SettingBar mAddressView;

    /** ? */
    private String mProvince = "???";
    /** ? */
    private String mCity = "???";
    /** ? */
    private String mArea = "???";

    /** ???? */
    private Uri mAvatarUrl;

    @Override
    protected int getLayoutId() {
        return R.layout.personal_data_activity;
    }

    @Override
    protected void initView() {
        mAvatarLayout = findViewById(R.id.fl_person_data_avatar);
        mAvatarView = findViewById(R.id.iv_person_data_avatar);
        mIdView = findViewById(R.id.sb_person_data_id);
        mNameView = findViewById(R.id.sb_person_data_name);
        mAddressView = findViewById(R.id.sb_person_data_address);
        setOnClickListener(mAvatarLayout, mAvatarView, mNameView, mAddressView);
    }

    @Override
    protected void initData() {
        GlideApp.with(getActivity())
                .load(R.drawable.avatar_placeholder_ic)
                .placeholder(R.drawable.avatar_placeholder_ic)
                .error(R.drawable.avatar_placeholder_ic)
                .transform(new MultiTransformation<>(new CenterCrop(), new CircleCrop()))
                .into(mAvatarView);

        mIdView.setRightText("880634");
        mNameView.setRightText("Android ???");

        String address = mProvince + mCity + mArea;
        mAddressView.setRightText(address);
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        if (view == mAvatarLayout) {
            ImageSelectActivity.start(this, data -> {
                // ????
                cropImageFile(new File(data.get(0)));
            });
        } else if (view == mAvatarView) {
            if (mAvatarUrl != null) {
                // ????
                ImagePreviewActivity.start(getActivity(), mAvatarUrl.toString());
            } else {
                // ????
                onClick(mAvatarLayout);
            }
        } else if (view == mNameView) {
            new InputDialog.Builder(this)
                    // ????????
                    .setTitle(getString(R.string.personal_data_name_hint))
                    .setContent(mNameView.getRightText())
                    //.setHint(getString(R.string.personal_data_name_hint))
                    //.setConfirm("??")
                    // ?? null ?????????
                    //.setCancel("??")
                    // ?????????????
                    //.setAutoDismiss(false)
                    .setListener((dialog, content) -> {
                        if (!mNameView.getRightText().equals(content)) {
                            mNameView.setRightText(content);
                        }
                    })
                    .show();
        } else if (view == mAddressView) {
            new AddressDialog.Builder(this)
                    //.setTitle("????")
                    // ??????
                    .setProvince(mProvince)
                    // ??????????????????
                    .setCity(mCity)
                    // ???????
                    //.setIgnoreArea()
                    .setListener((dialog, province, city, area) -> {
                        String address = province + city + area;
                        if (!mAddressView.getRightText().equals(address)) {
                            mProvince = province;
                            mCity = city;
                            mArea = area;
                            mAddressView.setRightText(address);
                        }
                    })
                    .show();
        }
    }

    /**
     * ????
     */
    private void cropImageFile(File sourceFile) {
        ImageCropActivity.start(this, sourceFile, 1, 1, new ImageCropActivity.OnCropListener() {

            @Override
            public void onSucceed(Uri fileUri, String fileName) {
                File outputFile;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    outputFile = new FileContentResolver(getActivity(), fileUri, fileName);
                } else {
                    try {
                        outputFile = new File(new URI(fileUri.toString()));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        outputFile = new File(fileUri.toString());
                    }
                }
                updateCropImage(outputFile, true);
            }

            @Override
            public void onError(String details) {
                // ????????????????
                // ?????????????????
                updateCropImage(sourceFile, false);
            }
        });
    }

    /**
     * ????????
     */
    private void updateCropImage(File file, boolean deleteFile) {
        if (true) {
            if (file instanceof FileContentResolver) {
                mAvatarUrl = ((FileContentResolver) file).getContentUri();
            } else {
                mAvatarUrl = Uri.fromFile(file);
            }
            GlideApp.with(getActivity())
                    .load(mAvatarUrl)
                    .transform(new MultiTransformation<>(new CenterCrop(), new CircleCrop()))
                    .into(mAvatarView);
            return;
        }

        EasyHttp.post(this)
                .api(new UpdateImageApi()
                        .setImage(file))
                .request(new HttpCallback<HttpData<String>>(this) {

                    @Override
                    public void onSucceed(HttpData<String> data) {
                        mAvatarUrl = Uri.parse(data.getData());
                        GlideApp.with(getActivity())
                                .load(mAvatarUrl)
                                .transform(new MultiTransformation<>(new CenterCrop(), new CircleCrop()))
                                .into(mAvatarView);
                        if (deleteFile) {
                            file.delete();
                        }
                    }
                });
    }
}