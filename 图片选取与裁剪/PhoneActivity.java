import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.tangram.nazi.myapplication.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PhoneActivity extends AppCompatActivity {


	private static final int PICK_FROM_CAMERA = 1;
	private static final int CROP_FROM_CAMERA = 2;
	private static final int PICK_FROM_FILE = 3;
	@BindView(R.id.mImageView)
	ImageView mImageView;
	/*@BindView(R.id.picture_selector_take_photo_btn)
	Button pictureSelectorTakePhotoBtn;
	@BindView(R.id.picture_selector_pick_picture_btn)
	Button pictureSelectorPickPictureBtn;
	@BindView(R.id.picture_selector_cancel_btn)
	Button pictureSelectorCancelBtn;
	@BindView(R.id.pop_layout)
	LinearLayout popLayout;*/
	private Uri imgUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_phone);
		ButterKnife.bind(this);
	}


	@OnClick({R.id.picture_selector_take_photo_btn, R.id.picture_selector_pick_picture_btn, R.id.picture_selector_cancel_btn})
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.picture_selector_take_photo_btn:
				// 方式1，直接打开图库，只能选择图库的图片
				Intent i = new Intent(Intent.ACTION_PICK,
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				// 方式2，会先让用户选择接收到该请求的APP，可以从文件系统直接选取图片
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT,
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				intent.setType("image/*");
				startActivityForResult(intent, PICK_FROM_FILE);
				break;
			case R.id.picture_selector_pick_picture_btn:
				Intent intent1 = new Intent(
						MediaStore.ACTION_IMAGE_CAPTURE);
				imgUri = Uri.fromFile(new File(Environment
						.getExternalStorageDirectory(), "avatar_"
						+ String.valueOf(System.currentTimeMillis())
						+ ".png"));
				intent1.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
				startActivityForResult(intent1, PICK_FROM_CAMERA);
			case R.id.picture_selector_cancel_btn:
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != RESULT_OK) {
			return;
		}
		switch (requestCode) {
			case PICK_FROM_CAMERA:
				doCrop();
				break;
			case PICK_FROM_FILE:
				imgUri = data.getData();
				doCrop();
				break;
			case CROP_FROM_CAMERA:
				if (null != data) {
					setCropImg(data);
				}
				break;
		}
	}

	private void doCrop() {
		final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(
				intent, 0);
		int size = list.size();
		if (size == 0) {
			Toast.makeText(this, "can't find crop app", Toast.LENGTH_SHORT)
					.show();
			return;
		} else {
			intent.setData(imgUri);
			intent.putExtra("outputX", 300);
			intent.putExtra("outputY", 300);
			intent.putExtra("aspectX", 1);
			intent.putExtra("aspectY", 1);
			intent.putExtra("scale", true);
			intent.putExtra("return-data", true);

			// only one
			if (size == 1) {
				Intent i = new Intent(intent);
				ResolveInfo res = list.get(0);
				i.setComponent(new ComponentName(res.activityInfo.packageName,
						res.activityInfo.name));
				startActivityForResult(i, CROP_FROM_CAMERA);
			} else {
				// many crop app
				for (ResolveInfo res : list) {
					final CropOption co = new CropOption();
					co.title = getPackageManager().getApplicationLabel(
							res.activityInfo.applicationInfo);
					co.icon = getPackageManager().getApplicationIcon(
							res.activityInfo.applicationInfo);
					co.appIntent = new Intent(intent);
					co.appIntent
							.setComponent(new ComponentName(
									res.activityInfo.packageName,
									res.activityInfo.name));
					cropOptions.add(co);
				}

				CropOptionAdapter adapter = new CropOptionAdapter(
						getApplicationContext(), cropOptions);

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("choose a app");
				builder.setAdapter(adapter,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								startActivityForResult(
										cropOptions.get(item).appIntent,
										CROP_FROM_CAMERA);
							}
						});

				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						if (imgUri != null) {
							getContentResolver().delete(imgUri, null, null);
							imgUri = null;
						}
					}
				});

				AlertDialog alert = builder.create();
				alert.show();
			}
		}
	}

	/**
	 * set the bitmap
	 *
	 * @param picdata
	 */
	private void setCropImg(Intent picdata) {
		Bundle bundle = picdata.getExtras();
		if (null != bundle) {
			Bitmap mBitmap = bundle.getParcelable("data");
			mImageView.setImageBitmap(mBitmap);
			saveBitmap(Environment.getExternalStorageDirectory() + "/crop_"
					+ System.currentTimeMillis() + ".png", mBitmap);
		}
	}

	/**
	 * save the crop bitmap
	 *
	 * @param fileName
	 * @param mBitmap
	 */
	public void saveBitmap(String fileName, Bitmap mBitmap) {
		File f = new File(fileName);
		FileOutputStream fOut = null;
		try {
			f.createNewFile();
			fOut = new FileOutputStream(f);
			mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
			fOut.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fOut.close();
				Toast.makeText(this, "save success", Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
