package com.example.mygame1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.util.Log;

public class Background {
    private Bitmap image;
    private float x1, x2;
    private float scrollSpeed;
    private int screenWidth, screenHeight;

    public Background(Context context, int screenWidth, int screenHeight, int level) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        try {
            // Choose background based on level
            int resId = (level == 2) ? R.drawable.background2 : R.drawable.background;
            Bitmap original = BitmapFactory.decodeResource(context.getResources(), resId);

            // Create fallback if resource not found
            if (original == null) {
                Log.e("Background", "Failed to load background resource: " + resId);
                original = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(original);
                Paint paint = new Paint();
                paint.setColor(level == 2 ? Color.DKGRAY : Color.LTGRAY);
                canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
            }

            image = Bitmap.createScaledBitmap(original, screenWidth, screenHeight, true);
        } catch (Exception e) {
            Log.e("Background", "Error creating background", e);
            // Create emergency fallback background
            image = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(image);
            Paint paint = new Paint();
            paint.setColor(level == 2 ? Color.DKGRAY : Color.LTGRAY);
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        }

        x1 = 0;
        x2 = screenWidth;
        scrollSpeed = screenWidth * 0.2f; // Adjust as needed
    }

    public void update(float deltaTime) {
        x1 -= scrollSpeed * deltaTime;
        x2 -= scrollSpeed * deltaTime;

        // Wrap logic for infinite scrolling
        if (x1 + screenWidth <= 0) {
            x1 = x2 + screenWidth;
        }
        if (x2 + screenWidth <= 0) {
            x2 = x1 + screenWidth;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        canvas.drawBitmap(image, x1, 0, paint);
        canvas.drawBitmap(image, x2, 0, paint);
    }
}
