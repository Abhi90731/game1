package com.example.mygame1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class ChasedCharacter {

    private final int screenWidth;
    private Bitmap image;
    private float x, y;
    private float speed;
    private boolean stopAtEdge = false;

    public ChasedCharacter(Context context, int screenWidth, int screenHeight, int level) {
        this.screenWidth = screenWidth;
        try {
            image = BitmapFactory.decodeResource(context.getResources(), R.drawable.ghost);
            if (image == null) {
                // Create a simple ghost shape if image not found
                image = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(image);
                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                canvas.drawOval(10, 10, 90, 90, paint);
            } else {
                image = Bitmap.createScaledBitmap(image, 120, 120, false);
            }
        } catch (Exception e) {
            Log.e("ChasedCharacter", "Error loading ghost image", e);
            image = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(image);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            canvas.drawOval(10, 10, 90, 90, paint);
        }
        x = screenWidth - 300; // starts ahead of player
        // Set y position based on level
        if (level >= 2) {
            y = screenHeight * 0.5f - 120; // Higher position for level 2+
        } else {
            y = screenHeight - 200; // Ground level for level 1
        }
        speed = 1000f; // constant speed
    }

    public void update(float deltaTime) {
        if (!stopAtEdge) {
            x += speed * deltaTime;

            // Limit to 80% of screen width
            float maxX = screenWidth * 0.9f;
            if (x + getWidth() >= maxX) {
                x = maxX - getWidth(); // adjust so the whole sprite fits
                stopAtEdge = true;
            }
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        canvas.drawBitmap(image, x, y, paint);
    }

    public float getX() {
        return x;
    }

    public float getWidth() {
        return image.getWidth();
    }

    public float getY() {
        return y;
    }

    public void setX(float x) {
        this.x = x;
    }
}
