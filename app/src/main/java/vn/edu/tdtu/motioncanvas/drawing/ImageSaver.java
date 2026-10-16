package vn.edu.tdtu.motioncanvas.drawing;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;

/** Lưu Bitmap thành PNG vào thư viện ảnh qua MediaStore. Gọi trên luồng nền. */
final class ImageSaver {
    private static final String ALBUM = "MotionCanvas";

    private ImageSaver() {
    }

    /**
     * Ghi `bitmap` thành file PNG trong album Pictures/MotionCanvas.
     *
     * @return Uri của ảnh vừa lưu trong MediaStore
     * @throws IOException nếu không tạo hoặc không ghi được file (file dở dang đã bị xóa)
     */
    static Uri save(Context context, Bitmap bitmap) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "drawing_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM);
            values.put(MediaStore.Images.Media.IS_PENDING, 1); // ẩn file khỏi Gallery khi đang ghi dở
        }

        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Không tạo được file ảnh");

        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw new IOException("Không ghi được ảnh");
            }
        } catch (IOException e) {
            resolver.delete(uri, null, null); // không để lại file rỗng
            throw e;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues done = new ContentValues();
            done.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(uri, done, null, null);
        }
        return uri;
    }
}
