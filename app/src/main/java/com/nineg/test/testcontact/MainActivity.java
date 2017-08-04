package com.nineg.test.testcontact;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nineg.logger.Logger;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER;

/**
 * Created by jason on 7/31/17.
 */

public class MainActivity extends Activity {

    ViewGroup mView = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.d("jasonlai", "123");
        mView = (ViewGroup) findViewById(R.id.container);
        retrieveAllContactsWithPhone();
    }

    public void addView(ShortcutContactInfo info) {
        Bitmap photo = info.photo;
        String displayName = info.displayName;

        View v = LayoutInflater.from(this).inflate(R.layout.text_view, mView, false);
        ImageView iv = (ImageView) v.findViewById(R.id.photo);
        TextView tv = (TextView) v.findViewById(R.id.text);
        iv.setImageBitmap(photo);
        tv.setText(displayName);
        v.setTag(info);
        v.setOnClickListener(mClickListener);
        mView.addView(v);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onStartAddView(v);
//            int i = new Random().nextInt(2);
//            if (i < 1)
//                onStartMMS(v);
//            else
//                onStartCall(v);
        }
    };

    public void onStartCall(View v) {
        ShortcutContactInfo info = (ShortcutContactInfo) v.getTag();
        Intent intent = new Intent(Intent.ACTION_CALL);

        Uri phone = Uri.fromParts("tel", info.phone, null);
        intent.setData(phone);
        if (ActivityCompat.checkSelfPermission(v.getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivity(intent);
    }

    private static final String[] PHONE_PROJECTION = new String[] {
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
    };

    private static final String PHONE_SELECTION = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?";

    public void onStartAddView(View v) {
        ShortcutContactInfo info = (ShortcutContactInfo) v.getTag();
        String lookUpkey = info.lookUpkey;
        Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookUpkey);
        Uri contactUri = ContactsContract.Contacts.lookupContact(v.getContext().getContentResolver(), lookupUri);
        long contactId = ContentUris.parseId(contactUri);
        ContentProviderClient client = null;
        try {
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;//ContactsContract.Contacts.CONTENT_URI;//
            client = getContentResolver().acquireUnstableContentProviderClient(uri);
            Cursor cursor = client.query(uri, null, PHONE_SELECTION, new String[] {String.valueOf(contactId)}, null, null); //
            int n = 0;
            dumpCursor(cursor);
            while(cursor.moveToNext()) {
//                int index_contact_id = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                int index_display_name = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int index_id = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
//                int index_lookup_key = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                int index_phone = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                int index_phonetic_name = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHONETIC_NAME);
                int index_phone_type = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
//
//                long contact_id = cursor.getLong(index_contact_id);
//
//                Bitmap photo = retrieveContactPhoto(contact_id);
                String displayName = cursor.getString(index_display_name);
                long id = cursor.getLong(index_id);
//
                ShortcutContactInfo newinfo = new ShortcutContactInfo();
//                info.photo = photo;
                newinfo.displayName = displayName;
//                info.lookUpkey = lookUpkey;
//                info.contact_uri =  ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact_id);
                newinfo.phone = cursor.getString(index_phone);
                String phoneticName = cursor.getString(index_phonetic_name);

                int type = cursor.getInt(index_phone_type);
                String typeName = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(v.getResources(), type, "nothing");
                newinfo.displayName += type+typeName;

                Logger.d("jasonlai", "add newinfo id %s, contactId %s, %s, %s, phoneticName %s, typeName %s", id, contactId, newinfo.displayName, newinfo.phone, phoneticName, type+typeName);
//                Logger.d("jasonlai", "m %s, photo? %s, display %s, id %s, lookUpkey %s, phone %s", n, photo != null ? photo.getWidth() + "x" + photo.getHeight() : null , displayName, id, lookUpkey, info.phone);
                addView(newinfo);
//                n++;
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (client != null)
                client.close();
        }

    }

    private void dumpCursor(Cursor cursor) {
        Logger.d("jasonlai", "Cursor raws %s", cursor.getCount());
        int size = cursor.getColumnCount();
        for (int i = 0; i < size; ++i) {
            String name = cursor.getColumnName(i);
            Logger.d("jasonlai", "column %s %s", i, name);
        }
    }

    public void onStartMMS(View v) {

        ShortcutContactInfo info = (ShortcutContactInfo) v.getTag();
        Intent intent = new Intent(Intent.ACTION_SENDTO);

        Uri phone = Uri.fromParts("mmsto", info.phone, null);
        intent.setType("text/plain");
        intent.setData(phone);

        startActivity(intent);
    }
    private static final String[] PROJECTION =
            {
            /*
             * The detail data row ID. To make a ListView work,
             * this column is required.
             */
                    ContactsContract.Data._ID,
                    // The primary display name
                    //ContactsContract.Data.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    // The contact's _ID, to construct a content URI
                    ContactsContract.Data.CONTACT_ID,
                    // The contact's LOOKUP_KEY, to construct a content URI
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };

    private static final String[] CONTACT_PROJECTION =
            {
            /*
             * The detail data row ID. To make a ListView work,
             * this column is required.
             */
                    ContactsContract.Contacts._ID,
                    // The primary display name
                    //ContactsContract.Data.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    // The contact's LOOKUP_KEY, to construct a content URI
                    ContactsContract.Contacts.LOOKUP_KEY,
            };

    class ShortcutContactInfo {
        Uri contact_uri;
        String lookUpkey;
        Bitmap photo;
        String displayName;
        String phone;
    }

    private static final String SELECTION = HAS_PHONE_NUMBER + " = '1' ";
    private void retrieveAllContactsWithPhone() {
//        Cursor cursorID = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
//                new String[]{ContactsContract.Contacts._ID},
//                null, null, null);

//        String[] mProjection = new String[]{ContactsContract.Data.CONTACT_ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        ContentProviderClient client = null;
        try {
            Uri uri = ContactsContract.Contacts.CONTENT_URI;//ContactsContract.CommonDataKinds.Phone.CONTENT_URI;//I;//
            client = getContentResolver().acquireUnstableContentProviderClient(uri);
            Cursor cursor = client.query(uri, null, SELECTION, null, null, null);
            int n = 0;
            dumpCursor(cursor);
            while(cursor.moveToNext()) {

                int index_contact_id = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                int index_display_name = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
//                int index_id = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                int index_lookup_key = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                int index_phone = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                long contact_id = cursor.getLong(index_contact_id);

//                Bitmap photo = retrieveContactPhoto(contact_id);
                String displayName = cursor.getString(index_display_name);
//                long id = cursor.getLong(index_id);
                String lookUpkey = cursor.getString(index_lookup_key);

                ShortcutContactInfo info = new ShortcutContactInfo();
//                info.photo = photo;
                info.displayName = displayName;
                info.lookUpkey = lookUpkey;
                info.contact_uri =  ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact_id);
//                info.phone = cursor.getString(index_phone);
//                Logger.d("jasonlai", "n %s, photo? %s, display %s, id %s, lookUpkey %s, phone %s", n, photo != null ? photo.getWidth() + "x" + photo.getHeight() : null , displayName, id, lookUpkey, info.phone);
                addView(info);
                n++;

            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (client != null)
                client.close();
        }
    }

    void startPickContactActivity() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivityForResultSafely(intent, 1111);
    }

    void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
        } catch (SecurityException e) {
        }
    }

//////////////////////

//    private static final String TAG = MainActivity.class.getSimpleName();
//    private static final int REQUEST_CODE_PICK_CONTACTS = 1;
//    private Uri uriContact;
//    private String contactID;     // contacts unique ID


    /**
     * Called when the activity is first created.
     */
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//    }

//    public void onClickSelectContact(View btnSelectContact) {
//
//        // using native contacts selection
//        // Intent.ACTION_PICK = Pick an item from the data, returning what was selected.
//        startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), REQUEST_CODE_PICK_CONTACTS);
//    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == REQUEST_CODE_PICK_CONTACTS && resultCode == RESULT_OK) {
//            Logger.d(TAG, "Response: " + data.toString());
//            uriContact = data.getData();
//
//            retrieveContactName();
//            retrieveContactNumber();
//            retrieveContactPhoto();
//
//        }
//    }

    private Bitmap retrieveContactPhoto(long id) {

        Bitmap photo = null;

        try {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id), true);

            if (inputStream != null) {
//                Bitmap a = BitmapFactory.decodeStream(inputStream);
//                BitmapFactory.Options options = new BitmapFactory.Options();
                photo =  BitmapFactory.decodeStream(inputStream);//decodeSampledBitmapFromDescriptor(inputStream, 160, 160);
//                Logger.d("jasonlai", "A %s, photo %s", a, photo);
                inputStream.close();
                return photo;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap decodeSampledBitmapFromDescriptor(
            InputStream fileDescriptor, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
//            BitmapFactory.decodeStream(fileDescriptor);
        } catch (Exception e) {
            Logger.e("jasonlai", "decodeSampledBitmapFromDescriptor first time fail " + e);
            return null;
        }

        Logger.d("jasonlai", "decodeSampledBitmapFromDescriptor %s x %s", options.outWidth, options.outHeight);
        // Calculate inSampleSize
//        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
//        options.inJustDecodeBounds = false;

        // If we're running on Honeycomb or newer, try to use inBitmap
//        addInBitmapOptions(options, cache);
//        options.inMutable = true;

        Bitmap bitmap = null;
        options = new BitmapFactory.Options();
        try {
            bitmap = BitmapFactory.decodeStream(fileDescriptor);
        } catch (Exception e) {
            Logger.e("jasonlai", "decodeSampledBitmapFromDescriptor fail " + e);
        }

        return bitmap;

    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
//
//    private void retrieveContactNumber() {
//
//        String contactNumber = null;
//
//        // getting contacts ID
//        Cursor cursorID = getContentResolver().query(uriContact,
//                new String[]{ContactsContract.Contacts._ID},
//                null, null, null);
//
//        if (cursorID.moveToFirst()) {
//
//            contactID = cursorID.getString(cursorID.getColumnIndex(ContactsContract.Contacts._ID));
//        }
//
//        cursorID.close();
//
//        Logger.d(TAG, "Contact ID: " + contactID);
//
//        // Using the contact ID now we will get contact phone number
//        Cursor cursorPhone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
//
//                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
//                        ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
//                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
//
//                new String[]{contactID},
//                null);
//
//        if (cursorPhone.moveToFirst()) {
//            contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//        }
//
//        cursorPhone.close();
//
//        Logger.d(TAG, "Contact Phone Number: " + contactNumber);
//    }
//
//    private void retrieveContactName() {
//
//        String contactName = null;
//
//        // querying contact data store
//        Cursor cursor = getContentResolver().query(uriContact, null, null, null, null);
//
//        if (cursor.moveToFirst()) {
//
//            // DISPLAY_NAME = The display name for the contact.
//            // HAS_PHONE_NUMBER =   An indicator of whether this contact has at least one phone number.
//
//            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
//        }
//
//        cursor.close();
//
//        Logger.d(TAG, "Contact Name: " + contactName);
//
//    }
    /////////////////////
}
