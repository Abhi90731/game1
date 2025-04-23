package com.example.mygame1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class Platform {
    private float x, y, width, height;
    private Bitmap image;

    public Platform(Context context, float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        try {
            // Replace with actual drawable resource
            this.image = BitmapFactory.decodeResource(context.getResources(), R.drawable.platform);
            // Create fallback colored bitmap if image not found
            if (this.image == null) {
                this.image = Bitmap.createBitmap(100, 20, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(this.image);
                Paint paint = new Paint();
                paint.setColor(Color.GREEN);
                canvas.drawRect(0, 0, 100, 20, paint);
            }
        } catch (Exception e) {
            Log.e("Platform", "Error loading platform image", e);
            // Create fallback bitmap
            this.image = Bitmap.createBitmap(100, 20, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(this.image);
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            canvas.drawRect(0, 0, 100, 20, paint);
        }
    }

    public void update(float deltaTime, float speed) {
        x -= speed * deltaTime;
    }

    public void draw(Canvas canvas) {
        RectF dst = new RectF(x, y, x + width, y + height);
        canvas.drawBitmap(image, null, dst, null);
    }

    public boolean isOutOfScreen() {
        return x + width < -20;
    }

    public float getX() {
        return x;
    }

    public float getWidth() {
        return width;
    }

    public float getY() {
        return y;
    }

    public float getHeight() {
        return height;
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }
}
