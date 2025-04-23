package com.example.mygame1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.util.Log;

public class Hurdle {
    private float x, y;
    private Bitmap image;
    private Platform parentPlatform; // Reference to the platform this hurdle is on
    private final boolean isLow;
    private final float width, height;
    private float relativePositionOnPlatform; // Store relative position on platform (0.0 to 1.0)

    private static Bitmap lowHurdleBitmap;
    private static Bitmap highHurdleBitmap;
    private final RectF bounds = new RectF();

    // Original constructor for platform-based hurdles (level 2+)
    public Hurdle(Context context, Platform platform, boolean isLow) {
        this.isLow = isLow;
        this.parentPlatform = platform;

        // Define width and height
        width = 100f;  // Smaller hurdles
        height = width; // Square shape

        initBitmaps(context);

        // Assign image and vertical position based on platform
        if (isLow) {
            image = lowHurdleBitmap;
            y = platform.getY() - height;
        } else {
            image = highHurdleBitmap;
            y = platform.getY() - height - 100; // Higher position for high hurdles
        }

        // Position hurdle on the platform
        // Calculate and store the relative position on platform (between 0.0 and 1.0)
        float platformWidth = platform.getWidth();

        // If platform is wide enough, have some variety
        if (platformWidth > 200) {
            // Random position between 0.3 and 0.7 of platform width
            relativePositionOnPlatform = 0.3f + (float)Math.random() * 0.4f;
        } else {
            relativePositionOnPlatform = 0.5f; // Default to middle
        }

        // Set initial x position relative to platform
        this.x = platform.getX() + platformWidth * relativePositionOnPlatform - width/2;
    }

    // New constructor for ground-based hurdles (level 1)
    public Hurdle(Context context, float screenWidth, float screenHeight, boolean isLow) {
        this.isLow = isLow;
        this.parentPlatform = null; // No platform for level 1

        // Define width and height
        width = 100f;
        height = width;

        initBitmaps(context);

        // Set initial position (start off-screen to the right)
        this.x = screenWidth + 50;

        // Set vertical position based on ground level
        float groundLevel = screenHeight - 100;
        if (isLow) {
            image = lowHurdleBitmap;
            y = groundLevel - height;
        } else {
            image = highHurdleBitmap;
            y = groundLevel - height - 100; // Higher position for high hurdles
        }
    }

    // Helper method to initialize bitmaps
    private void initBitmaps(Context context) {
        try {
            // Load and cache hurdle bitmaps only once
            if (lowHurdleBitmap == null || highHurdleBitmap == null) {
                Bitmap original = BitmapFactory.decodeResource(context.getResources(), R.drawable.spikes);
                if (original == null) {
                    original = createFallbackSpikeBitmap((int)width, (int)height);
                }
                Bitmap scaled = Bitmap.createScaledBitmap(original, (int) width, (int) height, true);

                // Low hurdle (bottom spike)
                lowHurdleBitmap = scaled;

                // High hurdle (flipped top spike)
                Matrix matrix = new Matrix();
                matrix.preScale(1, -1);
                highHurdleBitmap = Bitmap.createBitmap(scaled, 0, 0, (int) width, (int) height, matrix, true);
            }
        } catch (Exception e) {
            Log.e("Hurdle", "Error loading hurdle images", e);
            // Create fallback images
            if (lowHurdleBitmap == null) {
                lowHurdleBitmap = createFallbackSpikeBitmap((int)width, (int)height);
            }
            if (highHurdleBitmap == null) {
                Matrix matrix = new Matrix();
                matrix.preScale(1, -1);
                highHurdleBitmap = Bitmap.createBitmap(lowHurdleBitmap, 0, 0,
                        (int) width, (int) height, matrix, true);
            }
        }
    }

    private Bitmap createFallbackSpikeBitmap(int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setColor(Color.RED);

        // Draw triangle spikes
        Path path = new Path();
        path.moveTo(0, height);
        path.lineTo(width/2, 0);
        path.lineTo(width, height);
        path.close();

        canvas.drawPath(path, paint);
        return bmp;
    }

    public void update(float deltaTime, float speed) {
        if (parentPlatform != null) {
            // Update position based on platform for level 2+
            float platformWidth = parentPlatform.getWidth();
            x = parentPlatform.getX() + platformWidth * relativePositionOnPlatform - width/2;

            // Update y position in case platform moves vertically
            if (isLow) {
                y = parentPlatform.getY() - height;
            } else {
                y = parentPlatform.getY() - height - 100;
            }
        } else {
            // Level 1 hurdle: just move left at player speed
            x -= speed * deltaTime;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        canvas.drawBitmap(image, x, y, paint);
    }

    public boolean isOffScreen() {
        return x + width < 0;
    }

    public boolean collidesWith(Player player) {
        return RectF.intersects(getBounds(), player.getBounds());
    }

    public RectF getBounds() {
        float padding = 20f; // Smaller collision box
        bounds.set(
                x + padding,
                y + padding,
                x + width - padding,
                y + height - padding
        );
        return bounds;
    }

    public float getX() {
        return x;
    }

    public float getWidth() {
        return width;
    }

    public Platform getParentPlatform() {
        return parentPlatform;
    }
}