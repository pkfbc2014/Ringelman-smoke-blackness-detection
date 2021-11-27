package com.rain.takephotodemo;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import com.dtflys.forest.config.ForestConfiguration;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mingle.widget.ShapeLoadingDialog;
import com.yalantis.ucrop.UCrop;
import id.zelory.compressor.Compressor;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final String TAG = "MainActivity";
    private static final int REQUEST_TAKE_PHOTO = 0; //拍照
    private static final int REQUEST_CROP = 1; //裁剪
    private static final int SCAN_OPEN_PHONE = 2; //相册
    private static final int REQUEST_PERMISSION = 100;
    private ImageView img; //预览图

    public Uri imgUri; //拍照时返回的uri（未裁剪的图片，即temp_photo1的Uri）
    public Uri mCutUri; //图片裁剪时返回的uri（裁剪之后的图片，即temp_photo2的Uri）
    public Uri GalleryUri; //从相册选取图片后的Uri（选取相册中的某个图片后生成）

    //private Uri photoUri;
    //private String newUri = "";
    private String op_sd = "";
    private ShapeLoadingDialog shapeLoadingDialog;
    private int offset = 0;
    public static class Msg {
        public int code;
        public String message;
        public Data data;
        public static class Data{
            public String main_img;
            public Smoke[] sub_img;
        }
    }
    private List<Smoke> SmokeList;

    private boolean hasPermission = false;
    private File imgFile; //拍照保存的图片文件
    int Mode = 0;//0手动裁剪模式，1自动识别模式
    String ringelmanPath = "";
    String PathTest = "";
    Boolean IfTakePhoto = true;//判断是拍照还是从相册读取，true:拍照，false:相册读取
    Boolean IfIdentifySuccess = true;
    Button btn_ManualCropMode;
    Button btn_AutoIdentifyMode;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_ManualCropMode).setOnClickListener(this);
        findViewById(R.id.btn_AutoIdentifyMode).setOnClickListener(this);
        findViewById(R.id.btn_takephoto).setOnClickListener(this);
        findViewById(R.id.btn_open_photo_album).setOnClickListener(this);
        img = findViewById(R.id.iv);
        btn_ManualCropMode = findViewById(R.id.btn_ManualCropMode);
        btn_AutoIdentifyMode = findViewById(R.id.btn_AutoIdentifyMode);
        checkPermissions();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, 0, 0, "将此软件分享给朋友");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case 0:
                Intent intent=new Intent(Intent.ACTION_SEND);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Share");
                intent.putExtra(Intent.EXTRA_TEXT, "我正在使用一款超好用的App，现在分享给你！它的名字叫做：林格曼黑度检测，是用来检测烟雾的黑度并给出环保建议的，快快下载，和我一起做环保小卫士吧！下载链接：https://pan.baidu.com/s/1Z6Rrldmbo2yk-aSkruD4yw ，提取码：6666");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(intent, getTitle()));
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btn_ManualCropMode:
                Mode = 0;
                btn_ManualCropMode.setTextColor(0xFF000000);//black
                btn_ManualCropMode.setTextSize(20);//big
                btn_AutoIdentifyMode.setTextColor(0xFF7C7C7C);//gray
                btn_AutoIdentifyMode.setTextSize(15);//small
                break;
            case R.id.btn_AutoIdentifyMode:
                Mode = 1;
                btn_ManualCropMode.setTextColor(0xFF7C7C7C);//gray
                btn_ManualCropMode.setTextSize(15);//small
                btn_AutoIdentifyMode.setTextColor(0xFF000000);//black
                btn_AutoIdentifyMode.setTextSize(20);//big
                break;
            case R.id.btn_takephoto:
                checkPermissions();
                if (hasPermission)
                {
                    takePhoto();
                }
                break;
            case R.id.btn_open_photo_album:
                checkPermissions();
                if (hasPermission)
                {
                    openGallery();
                }
                break;
            default:
                break;
        }

    }
    private void takePhoto() //拍照
    {
        IfTakePhoto = true;
        // 要保存的文件名
        String fileName = "temp_photo1";
        // 创建一个文件夹
        String path = Environment.getExternalStorageDirectory() + "/take_photo";//getExternalFilesDir(null).getPath() + "/" + fileName + ".jpg";//Environment.getExternalStorageDirectory() + "/take_photo";
        //PathTest = path;
        File file = new File(path);
        if (!file.exists())
        {
            //file.getParentFile().mkdirs();
            file.mkdirs();
        }
        // 要保存的图片文件
        imgFile = new File(file, fileName + ".jpeg");
        // 将file转换成uri
        // 注意7.0及以上与之前获取的uri不一样了，返回的是provider路径
        imgUri = getUriForFile(this, imgFile);
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");//(MediaStore.ACTION_IMAGE_CAPTURE);
        // 添加Uri读取权限
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        // 或者
//        grantUriPermission("com.rain.takephotodemo", imgUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // 添加图片保存位置
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
        intent.putExtra("return-data", false);
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }
    private void openGallery()
    {
        IfTakePhoto = false;
        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType("image/*");
        startActivityForResult(intent, SCAN_OPEN_PHONE);
    }

    private void checkPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            // 检查是否有存储和拍照权限
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            {
                hasPermission = true;
            }
            else
            {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                hasPermission = true;
            }
            else
            {
                Toast.makeText(this, "权限授予失败！", Toast.LENGTH_SHORT).show();
                hasPermission = false;
            }
        }
    }

     /*void compress(String path) {
        try {
            File new_file = new Compressor(MainActivity.this).compressToFile(new File(path));
            FileOutputStream fos = new FileOutputStream(new File(path));
            Bitmap bitmap = BitmapFactory.decodeFile(new_file.getPath());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
        }
    }*/

    private void cropPhotoOrAutoIdentity(Uri uri, boolean fromCapture) //图片裁剪或者自动识别
    {

        switch(Mode){
            case 0://手动剪裁
                Intent intent = new Intent("com.android.camera.action.CROP"); //打开系统自带的裁剪图片的intent
                intent.setDataAndType(uri, "image/*");
                intent.putExtra("crop", "true");

                // 注意一定要添加该项权限，否则会提示无法裁剪
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                intent.putExtra("scale", true);

                // 设置裁剪区域的宽高比例
                //intent.putExtra("aspectX", 1);
                //intent.putExtra("aspectY", 1);

                // 设置裁剪区域的宽度和高度
                intent.putExtra("outputX", 200);
                intent.putExtra("outputY", 200);

                // 取消人脸识别
                intent.putExtra("noFaceDetection", true);
                // 图片输出格式
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

                // 若为false则表示不返回数据
                intent.putExtra("return-data", false);

                //不论是拍照还是从相册选取，裁剪之后的图片都保存在temp_photo2中
                String fileName = "temp_photo2";
                File mCutFile = new File(Environment.getExternalStorageDirectory() + "/take_photo/", fileName + ".jpeg");
                if (!mCutFile.getParentFile().exists())
                {
                    mCutFile.getParentFile().mkdirs();
                }
                mCutUri = Uri.fromFile(mCutFile);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, mCutUri);

                //Toast.makeText(this, "剪裁图片", Toast.LENGTH_SHORT).show();

                // 以广播方式刷新系统相册，以便能够在相册中找到刚刚所拍摄和裁剪的照片
                Intent intentBc = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intentBc.setData(uri);
                this.sendBroadcast(intentBc);
                ringelmanPath = Environment.getExternalStorageDirectory() + "/take_photo/" + "temp_photo2.jpeg";
                startActivityForResult(intent, REQUEST_CROP); //设置裁剪参数显示图片至ImageView
                break;
            case  1://自动识别
                String path = "";
                String opd = "";
                //Log.wtf(TAG, "ringelmanPath:" + ringelmanPath);
                if(IfTakePhoto == true){
                    //path = PathTest;
                    path = imgFile.getPath();
                }else{
                    path = handleImageOnKitKat(GalleryUri);//GalleryUri.getPath();
                }

                opd = (Uri.parse("file://" + path)).toString().substring(7);
                //只需要将代码插入以下两处，其他代码无需更改！！！！！！！
                //1.请在下面添加传给云端的代码，传递给云端调用函数的的路径字符串型变量为：path
                //2.请在下面添加云端传回的代码，请将从云端传回的路径赋值给变量 ringelmanPath； 例：ringelmanPath = XXX; （XXX是从云端传回的在安卓设备上的绝对路径;

                ForestConfiguration forest = ForestConfiguration.configuration();
                MyClient myClient = forest.createInstance(MyClient.class);
                String upPath = opd;

                Thread thread = new Thread (
                        new Runnable() {
                            @Override
                            public void run() {
                                String result = myClient.upload(upPath, progress -> {
                                    System.out.println("total bytes: " + progress.getTotalBytes());   // 文件大小
                                    System.out.println("current bytes: " + progress.getCurrentBytes());   // 已上传字节数
                                    System.out.println("progress: " + Math.round(progress.getRate() * 100) + "%");  // 已上传百分比
                                    if (progress.isDone()) {   // 是否上传完成
                                        System.out.println("--------   Upload Completed!   --------");
                                    }
                                });

                                SmokeList = new ArrayList<>();
                                Gson gson = new Gson();
                                Msg msg = new Gson().fromJson(result, Msg.class);
                                //对象中拿到集合
                                Msg.Data data = msg.data;

                                File file;
                                Smoke tmp;
                                op_sd = "temp_photo2";//Method.getTimeStr();

                                if(data.sub_img.length == 0){
                                    IfIdentifySuccess = false;
                                    Mode = 0;
                                    //如果检测到是原图则不下载，而是直接进入手动裁剪模式
                                    /*file = myClient.downloadFile(
                                            Environment.getExternalStorageDirectory() + "/take_photo",//getExternalFilesDir(null).getPath(),
                                            op_sd + ".jpg",
                                            progress -> {
                                                System.out.println("total bytes: " + progress.getTotalBytes());   // 文件大小
                                                System.out.println("current bytes: " + progress.getCurrentBytes());   // 已下载字节数
                                                System.out.println("progress: " + Math.round(progress.getRate() * 100) + "%");  // 已下载百分比
                                                if (progress.isDone()) {   // 是否下载完成
                                                    System.out.println("--------   Main Img Download Completed!   --------");
                                                }
                                            }, data.main_img);*/


                                }else{
                                    IfIdentifySuccess = true;
                                    String downloadPath = data.sub_img[0].url;
                                    Log.v("tts", downloadPath);
                                    //op_sd = Method.getTimeStr();
                                    file = myClient.downloadFile(
                                            Environment.getExternalStorageDirectory() + "/take_photo",//getExternalFilesDir(null).getPath(),
                                            op_sd + ".jpg",
                                            progress -> {
                                                System.out.println("total bytes: " + progress.getTotalBytes());   // 文件大小
                                                System.out.println("current bytes: " + progress.getCurrentBytes());   // 已下载字节数
                                                System.out.println("progress: " + Math.round(progress.getRate() * 100) + "%");  // 已下载百分比
                                                if (progress.isDone()) {   // 是否下载完成
                                                    System.out.println("--------   Sub Img Download Completed!   --------");
                                                }
                                            },
                                            downloadPath);
                                }

                                tmp = new Smoke();
                                tmp.url = Environment.getExternalStorageDirectory() + "/take_photo"/*getExternalFilesDir(null).getPath()*/ + "/" + op_sd + ".jpg";
                                ringelmanPath = Environment.getExternalStorageDirectory() + "/take_photo"/*getExternalFilesDir(null).getPath()*/ + "/" + op_sd + ".jpg";
                                tmp.level = "-1";
                                SmokeList.add(tmp);
                                offset++;
                            }
                        });

                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                }

                if(IfIdentifySuccess == true){
                    //将图片输出到imageView
                    FileInputStream fis = null;
                    try
                    {
                        fis = new FileInputStream(ringelmanPath);
                        Bitmap bitmap = BitmapFactory.decodeStream(fis);
                        img.setImageBitmap(bitmap);
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    //使用林格曼算法函数
                    level_of_ringelman();
                }else{
                    Toast.makeText(MainActivity.this, "未识别到烟雾，进入手动裁剪模式", Toast.LENGTH_SHORT).show();
                    if(IfTakePhoto == true){
                        cropPhotoOrAutoIdentity(imgUri, true);
                    }else{
                        cropPhotoOrAutoIdentity(GalleryUri, false);
                    }
                    Mode = 1;
                }

                break;
            default:
                break;
        }

    }
    //以下两个函数是为了将android的Uri转换成Path，非常有用，误删
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }

            cursor.close();
        }
        return path;
    }
    private String handleImageOnKitKat(Uri uri) {
        String imagePath = null;

        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                //Log.d(TAG, uri.toString());
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                //Log.d(TAG, uri.toString());
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //Log.d(TAG, "content: " + uri.toString());
            imagePath = getImagePath(uri, null);
        }
        return imagePath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            switch (requestCode)
            {
                // 拍照并进行裁剪
                case REQUEST_TAKE_PHOTO:
                    //Log.e(TAG, "onActivityResult: imgUri:REQUEST_TAKE_PHOTO:" + imgUri.toString());
                    cropPhotoOrAutoIdentity(imgUri, true);
                    break;
                // 裁剪后设置图片
                case REQUEST_CROP:
                    //img.setImageURI(mCutUri);
                    FileInputStream fis = null;
                    try
                    {
                        fis = new FileInputStream(ringelmanPath);
                        Bitmap bitmap = BitmapFactory.decodeStream(fis);
                        img.setImageBitmap(bitmap);
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    //Log.e(TAG, "onActivityResult: imgUri:REQUEST_CROP:" + mCutUri.toString());
                    level_of_ringelman();
                    break;
                // 打开图库获取图片并进行裁剪
                case SCAN_OPEN_PHONE:
                    //Log.e(TAG, "onActivityResult: SCAN_OPEN_PHONE:" + data.getData().toString());
                    GalleryUri = data.getData();
                    cropPhotoOrAutoIdentity(data.getData(), false);
                    break;
                default:
                    break;
            }

        }
    }

    // 从file中获取uri
    // 7.0及以上使用的uri是contentProvider content://com.rain.takephotodemo.FileProvider/images/photo_20180824173621.jpg
    // 6.0使用的uri为file:///storage/emulated/0/take_photo/photo_20180824171132.jpg
    private static Uri getUriForFile(Context context, File file)
    {
        if (context == null || file == null)
        {
            throw new NullPointerException();
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24)
        {
            uri = FileProvider.getUriForFile(context.getApplicationContext(), "com.rain.takephotodemo.FileProvider", file);
        }
        else
        {
            uri = Uri.fromFile(file);
        }
        return uri;
    }
    public void level_of_ringelman()
    {
        TextView ringelman = (TextView) findViewById(R.id.ringelman); //林格曼黑度值
        TextView lv = (TextView) findViewById(R.id.lv); //烟雾级别
        TextView Smoke_concentration = (TextView) findViewById(R.id.Smoke_concentration); //烟雾浓度
        TextView evaluate = (TextView) findViewById(R.id.evaluate); //烟雾评价
        TextView Environmental_tips = (TextView) findViewById(R.id.Environmental_tips); //环保贴士

        int grayArray[] = null;
        grayArray = new int[256]; //灰度值数组
        for(int i = 0 ; i < 255 ; i++) //初始化数组
            grayArray[i] = 0;
        int L = 255;
        int T; //烟雾和背景分割的阈值的下标
        int st = 0, nd = 0; //图像灰度值的下界和上界的下标
        double min = 0.0; //左边灰度值波峰
        double max = 0.0; //右边灰度值波峰
        int level; //林格曼黑度等级
        double ringelman_num; //林格曼黑度值
        try
        {
            //以文件流的方式读取图片
            FileInputStream fis = new FileInputStream(ringelmanPath);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);

            int width = bitmap.getWidth(); //图片的宽度
            int height = bitmap.getHeight(); //图片的高度
            int pixel;
            for(int i = 0; i < width; i++)
            {
                for(int j = 0; j < height; j++)
                {
                    pixel = bitmap.getPixel(i, j); //pixel为当前像素点
                    int r = (pixel & 0xff0000) >> 16; //red
                    int g = (pixel & 0xff00) >> 8; //green
                    int b = (pixel & 0xff); //blue
                    int avg = (int)(0.3 * r + 0.59 * g + 0.11 * b); //彩色图转换为灰度图
                    grayArray[avg] = grayArray[avg] + 1;
                }
            }

            for (int i = 0; i <= L; i++)
            {
                if (grayArray[i] != 0)
                {
                    st = i;
                    break;
                }
            }
            for (int i = L; i >= 0; i--)
            {
                if (grayArray[i] != 0)
                {
                    nd = i;
                    break;
                }
            }
            T = (int)((st + nd) / 2); //取中

            if (st == nd) //极端纯色
            {
                ringelman_num = (double) st;
                if (ringelman_num > 205 && ringelman_num < 256)
                    level = 0;
                else if (ringelman_num > 154 && ringelman_num <= 205)
                    level = 1;
                else if (ringelman_num > 102 && ringelman_num <= 154)
                    level = 2;
                else if (ringelman_num > 51 && ringelman_num <= 102)
                    level = 3;
                else if(ringelman_num > 1 && ringelman_num <= 51)
                    level = 4;
                else
                    level = 5;
            }
            else //不是极端纯色，迭代法
            {
                int T1 = 0;
                double ip1, ip2, ip3, ip4;
                for (int t = 0; t < 100; t++) //迭代100次
                {
                    ip1 = ip2 = ip3 = ip4 = 0; //每次都要初始化
                    for (int i = 0; i < T; i++)
                    {
                        ip1 = ip1 + grayArray[i] * i;
                        ip2 = ip2 + grayArray[i];
                    }
                    if (ip2 == 0)
                        break;
                    min = ip1 / ip2;

                    for (int i = T; i <= L; i++)
                    {
                        ip3 = ip3 + grayArray[i] * i;
                        ip4 = ip4 + grayArray[i];
                    }
                    if (ip4 == 0)
                        break;
                    max = ip3 / ip4;

                    T1 = (int)((min + max) / 2);
                    if (T1 == T)
                        break;
                    else
                        T = T1;
                }

                if (Math.abs(max - min) <= 60) //两个波峰相差不超过60灰度值，近似认定为纯色烟雾
                {
                    ringelman_num = (max + min) / 2; //直接用max与min的平均值代替黑度值
                    if (ringelman_num > 205 && ringelman_num < 256)
                        level = 0;
                    else if (ringelman_num > 154 && ringelman_num <= 205)
                        level = 1;
                    else if (ringelman_num > 102 && ringelman_num <= 154)
                        level = 2;
                    else if (ringelman_num > 51 && ringelman_num <= 102)
                        level = 3;
                    else if(ringelman_num > 1 && ringelman_num <= 51)
                        level = 4;
                    else
                        level = 5;
                }
                else
                {
                    double dis1 = min; //计算min距离最黑的距离
                    double dis2 = 255.0 - max; //计算max距离最白的距离
                    if (dis1 > dis2) //max是烟雾，min是背景，且烟雾是白烟
                    {
                        ringelman_num = ((255 - max) / min) * 256; //反转取值
                    }
                    else //min是烟雾，max是背景，且烟雾是黑烟
                    {
                        ringelman_num = (min / max) * 256; //正取值
                    }
                    if (ringelman_num > 205 && ringelman_num <= 256)
                        level = 0;
                    else if (ringelman_num > 154 && ringelman_num <= 205)
                        level = 1;
                    else if (ringelman_num > 102 && ringelman_num <= 154)
                        level = 2;
                    else if (ringelman_num > 51 && ringelman_num <= 102)
                        level = 3;
                    else if(ringelman_num > 1 && ringelman_num <= 51)
                        level = 4;
                    else
                        level = 5;
                }
            }

            DecimalFormat df = new DecimalFormat("0.00"); //保留两位小数
            ringelman_num = Math.abs(255.0 - ringelman_num); //反转林格曼黑度值，使得数目越大烟雾越黑，且防止出现负数

            if (level == 0)
            {
                ringelman.setText("" + df.format(ringelman_num));
                lv.setText("" + level);
                Smoke_concentration.setText("" + "清淡");
                evaluate.setText("" + "该烟雾的林格曼黑度值较小，浓度较为清淡，对环境的影响不是很大");
                Environmental_tips.setText("" + "该烟雾若无浓烈气味，一般不会对人体和环境产生影响，皮肤病患者建议随时清洁");
            }
            else if (level == 1)
            {
                ringelman.setText("" + df.format(ringelman_num));
                lv.setText("" + level);
                Smoke_concentration.setText("" + "中等");
                evaluate.setText("" + "该烟雾的林格曼黑度值、浓度都为中等，排放这样的烟雾，会对环境造成危害");
                Environmental_tips.setText("" + "若吸入该烟雾，可以用生理盐水清洁鼻腔，保持鼻腔清洁，清除吸入的灰尘");
            }
            else if (level == 2)
            {
                ringelman.setText("" + df.format(ringelman_num));
                lv.setText("" + level);
                Smoke_concentration.setText("" + "较浓");
                evaluate.setText("" + "该烟雾会对环境造成较为严重危害，吸入该烟雾，不久便会出现不良症状");
                Environmental_tips.setText("" + "远离该烟雾，若居住在烟雾源附近，应尽量室内活动，必要时使用空气净化器");
            }
            else if (level == 3)
            {
                ringelman.setText("" + df.format(ringelman_num));
                lv.setText("" + level);
                Smoke_concentration.setText("" + "浓烈");
                evaluate.setText("" + "该烟雾黑度已经超标，其中含有浓烈毒素，会对人和环境造成不可估量的危害");
                Environmental_tips.setText("" + "请与该烟雾保持至少3公里的距离，戴好口罩，防止吸入其中的有害物质");
            }
            else if (level == 4)
            {
                ringelman.setText("" + df.format(ringelman_num));
                lv.setText("" + level);
                Smoke_concentration.setText("" + "重度浓烈");
                evaluate.setText("" + "该烟雾属于空气污染问题，会使得患有呼吸系统疾病的患者的症状明显加剧");
                Environmental_tips.setText("" + "该企业已经违反《大气污染防治法》，请保留好相关证据，并立即向相关部门举报");
            }
            else
            {
                ringelman.setText("" + df.format(ringelman_num));
                lv.setText("" + level);
                Smoke_concentration.setText("" + "极其浓烈");
                evaluate.setText("" + "警告！该烟雾已达到污染极限，会对环境造成极大危害，属于极重污染问题！");
                Environmental_tips.setText("" + "请立即向有关部门举报，并及时撤离！请用湿毛巾捂住口鼻，以防自身受到伤害！");
            }





        }
        catch (IOException e) //输出异常信息
        {
            System.out.println(e);
        }
    }
}

