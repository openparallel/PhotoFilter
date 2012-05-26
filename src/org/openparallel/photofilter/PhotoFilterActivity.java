package org.openparallel.photofilter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.openparallel.photofilter.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class PhotoFilterActivity extends Activity {
	/** Called when the activity is first created. */

	static {
		System.loadLibrary("opencv");
	}

	//public native byte[] findContours(int[] data, int w, int h);
	public native byte[] getSourceImage();
	public native boolean setSourceImage(int[] data, int w, int h);
	
	//Image capture constants
	final int PICTURE_ACTIVITY = 1000; // This is only really needed if you are catching the results of more than one activity.  It'll make sense later.
	public static final String TEMP_PREFIX = "tmp_";
	
	//private variables needed for image capture
	private ImageView imageView;
	private Uri imageUri;
	/* Override the onCreate method */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState); // Blah blah blah call the super.
		setContentView(R.layout.main); // It is VERY important that you do this FIRST.  If you don't, the next line will throw a null pointer exception.  And God will kill a kitten.
		
		final Button cameraButton = (Button)findViewById(R.id.camera_button); // Get a handle to the button so we can add a handler for the click event 
		cameraButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){

				Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // Normally you would populate this with your custom intent.
						
				ContentValues values = new ContentValues();
		        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
		        imageUri = getContentResolver().insert(
		                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
				
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

				startActivityForResult(cameraIntent, PICTURE_ACTIVITY); // This will cause the onActivityResult event to fire once it's done

			}
		});

	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		AlertDialog msgDialog;

		/*
			This is where you would trap the requestCode (in this case PICTURE_ACTIVITY).  Seeing as how this is the ONLY 
			Activity that we are calling from THIS activity, it's kind of a moot point.  If you had more than one activity that
			you were calling for results, you would need to throw a switch statement in here or a bunch of if-then-else
			constructs.  Whatever floats your boat.
		 */

		if (requestCode == PICTURE_ACTIVITY) { 
			if(resultCode == RESULT_OK){

				//initialise the imageview
				this.imageView = (ImageView)this.findViewById(R.id.imageView1);

				//load the image from camera and set it as the imageview
				try{
					if(intent != null){
						//Bitmap photo = Media.getBitmap(this.getContentResolver(), intent.getData());	
						Bitmap photo =  (Bitmap) getIntent().getExtras().get("data");
						imageView.setImageBitmap(photo);
						//photo.recycle();
					}
					else{
						
						Bitmap photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
						
						int w = photo.getWidth();
						int h = photo.getHeight();
						int[] data = new int[w * h];
						
						setSourceImage(data, w, h);
						
						byte[] resultData = getSourceImage();
						
						//int w = bitmap.getWidth();
		                //int h = bitmap.getHeight();
		                //int[] pixels = new int[w * h];
		                //bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

		                //byte[] data = findContours(pixels, w, h);
		                //Bitmap faceDetectBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		                
						
						//imageView.setImageURI(imageUri);
						imageView.setImageBitmap(photo);
						
						//msgDialog = createAlertDialog(":(", "Bummer... the AVD or current device doesn't support camera capture", "OK!");
						//msgDialog.show();
					}

				}catch (Exception e) {
					e.printStackTrace();
					// TODO: handle exception
				}

			}

			if (resultCode == RESULT_CANCELED) { // The user didn't like the photo.  ;_;
				msgDialog = createAlertDialog(":)", "You hit the cancel button... why not try again later!", "OK!");
			}

		}



		msgDialog = createAlertDialog(":)", "The Photo you have taken has been filtered", "Ok!");

		/*
			Yes, I know that throwing a simple alert dialog doesn't really do anything impressive.
			If you wanna do something with the picture (save it, display it, shoot it to a web server, etc) then you can get the 
			image data like this:

			Bitmap = getIntent().getExtras().get("data");

			Then do whatever you want with it.

		 */


		msgDialog.show();
	}

	/*
	public Bitmap getThumbnail(Uri uri) throws FileNotFoundException, IOException{
        InputStream input = this.getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;
        double THUMBNAIL_SIZE = 1.0;
        double ratio = (originalSize > THUMBNAIL_SIZE) ? (originalSize / THUMBNAIL_SIZE) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true;//optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        input = this.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }
	 */

	@SuppressWarnings("deprecation")
	private AlertDialog createAlertDialog(String title, String msg, String buttonText){
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		AlertDialog msgDialog = dialogBuilder.create();
		msgDialog.setTitle(title);
		msgDialog.setMessage(msg);
		msgDialog.setButton(buttonText, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int idx){
				return; // Nothing to see here...
			}
		});

		return msgDialog;
	}


}