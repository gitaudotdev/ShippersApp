package com.codekid.shippersapp.Common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Location;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;

import com.codekid.shippersapp.Model.Request;
import com.codekid.shippersapp.Model.Shipper;
import com.codekid.shippersapp.Model.ShippingInfo;
import com.codekid.shippersapp.Remote.IGeoCoordinates;
import com.codekid.shippersapp.Remote.RetroFitClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Common {
    public static final String SHIPPER_TABLE ="Shippers";
    public static final String SHIPPING_TABLE = "OrdersToBeShipped";
    public static final String SHIPPER_INFO_TABLE = "OrdersInShipping";

    public static Request currentRequest;
    public static String currentKey;

    public static Shipper currentShipper;
    public static final int REQUEST_CODE = 8888;

    public static final String Base_Url = "https://maps.googleapis.com/";

    public static IGeoCoordinates getGeoCodeService(){
        return RetroFitClient.getClient(Base_Url).create(IGeoCoordinates.class);
    }

    public static Bitmap scaleBitmap (Bitmap bitmap , int newWidth, int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth,newHeight,Bitmap.Config.ARGB_8888);

        float scaleX = newWidth/(float)bitmap.getWidth();
        float scaleY = newHeight/(float)bitmap.getHeight();
        float pivotX=0,pivotY=0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX,scaleY,pivotX,pivotY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap,0,0,new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    public static String convertCodeToStatus(String code) {
        if(code.equals("0"))
            return "Placed";
        else if (code.equals("1"))
            return "On my Way";
        else if (code.equals("2"))
            return "Shipping";
        else
            return "Shipped";
    }

    public static String getDate(long time){
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(time);
        StringBuilder date = new StringBuilder(DateFormat.format("dd:MM:yyyy HH:mm",calendar).toString());
        return date.toString();
    }

    public static void createShippingOrder(String key, String phone, Location mLastLocation) {

        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setOrderId(key);
        shippingInfo.setShipperPhone(phone);
        shippingInfo.setLat(mLastLocation.getLatitude());
        shippingInfo.setLng(mLastLocation.getLongitude());

        //Create new item on ShipperInformation table
        FirebaseDatabase.getInstance()
                .getReference(SHIPPER_INFO_TABLE)
                .child(key)
                .setValue(shippingInfo)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("CodeKidError",e.getMessage());
                    }
                });

    }

    public static void updateShippingInfo(String currentKey, Location mLastLocation) {
        Map<String,Object> update_location = new HashMap<>();
        update_location.put("lat",mLastLocation.getLatitude());
        update_location.put("lng",mLastLocation.getLongitude());

        FirebaseDatabase.getInstance()
                .getReference(SHIPPER_INFO_TABLE)
                .child(currentKey)
                .updateChildren(update_location)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("codeKidError",e.getMessage());
                    }
                });
    }
}
