package com.musayusuf.theme;

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.io.ByteArrayOutputStream;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.core.content.FileProvider;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

@CapacitorPlugin(name = "MusaWallpaper")
public class WallpaperPlugin extends Plugin {

    private byte[] imageBytes(PluginCall call) {
        String data = call.getString("image");
        return data == null ? null : Base64.decode(data, Base64.DEFAULT);
    }

    @PluginMethod
    public void setWallpaper(PluginCall call) {
        try {
            byte[] b = imageBytes(call);
            if (b == null) { call.reject("No image"); return; }
            Bitmap bm = BitmapFactory.decodeByteArray(b, 0, b.length);
            String which = call.getString("which", "both");
            int flag = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
            if ("home".equals(which)) flag = WallpaperManager.FLAG_SYSTEM;
            else if ("lock".equals(which)) flag = WallpaperManager.FLAG_LOCK;
            WallpaperManager.getInstance(getContext()).setBitmap(bm, null, true, flag);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void saveImage(PluginCall call) {
        try {
            byte[] b = imageBytes(call);
            if (b == null) { call.reject("No image"); return; }
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, "musa-yusuf-" + System.currentTimeMillis() + ".jpg");
            v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT >= 29) {
                v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MusaYusufTheme");
            }
            ContentResolver r = getContext().getContentResolver();
            Uri u = r.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            OutputStream o = r.openOutputStream(u);
            o.write(b);
            o.close();
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void shareImage(PluginCall call) {
        try {
            byte[] b = imageBytes(call);
            if (b == null) { call.reject("No image"); return; }
            File f = new File(getContext().getCacheDir(), "share.jpg");
            FileOutputStream o = new FileOutputStream(f);
            o.write(b);
            o.close();
            Uri u = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("image/jpeg");
            i.putExtra(Intent.EXTRA_STREAM, u);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent c = Intent.createChooser(i, "Share");
            c.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(c);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void getApps(PluginCall call) {
        try {
            PackageManager pm = getContext().getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
            JSArray arr = new JSArray();
            String self = getContext().getPackageName();
            for (ResolveInfo ri : list) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(self)) continue;
                JSObject o = new JSObject();
                o.put("label", String.valueOf(ri.loadLabel(pm)));
                o.put("pkg", pkg);
                Drawable d = ri.loadIcon(pm);
                Bitmap bm = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bm);
                d.setBounds(0, 0, 96, 96);
                d.draw(c);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.PNG, 100, bo);
                o.put("icon", Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP));
                arr.put(o);
            }
            JSObject res = new JSObject();
            res.put("apps", arr);
            call.resolve(res);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void launchApp(PluginCall call) {
        try {
            String pkg = call.getString("pkg");
            Intent i = getContext().getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) { call.reject("Not found"); return; }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void isHome(PluginCall call) {
        JSObject r = new JSObject();
        Intent i = getActivity().getIntent();
        r.put("home", i != null && i.hasCategory(Intent.CATEGORY_HOME));
        call.resolve(r);
    }
}
