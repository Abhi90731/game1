package com.example.mygame1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

public class Player {
    private float x, y;
    private float width, height;
    private final float screenHeight;
    private boolean isJumping = false;
    private boolean isSliding = false;
    private float velocityY = 0;

    private final float gravity = 2300f;
    private final float slideHeight = 50f;
    private final float normalHeight;

    private float scaleX = 1f;
    private float scaleY = 1f;

    private final float slideDuration = 0.7f;
    private float slideTimer = 0f;

    // Animation
    private static Bitmap spriteSheet; // cache globally
    private final int rows = 2;
    private final int columns = 3;
    private final int frameCount = rows * columns;
    private int currentFrame = 0;
    private float frameTicker = 0f;
    private final float framePeriod = 0.1f;
    private final int frameWidth;
    private final int frameHeight;

    private final Rect src = new Rect();
    private final Rect dst = new Rect();
    private final RectF bounds = new RectF();
    private int gameLevel;
    private float groundY; // Store the ground Y position based on level
    private boolean isFalling = false;

    // Add a feet offset to ensure bottom of sprite aligns with ground/platform
    private final float feetOffset = 0f; // May need adjustment based on sprite

    public Player(Context context, int screenWidth, int screenHeight, int level) {
        this.screenHeight = screenHeight;
        this.gameLevel = level;
        width = 200;
        height = 260;
        normalHeight = height;
        x = 200;

        // Set proper ground Y position based on level
        if (gameLevel >= 2) {
            // Adjust platform position to ensure the player's feet touch the platform
            groundY = screenHeight * 0.5f - height; // Remove the -5 offset
        } else {
            groundY = screenHeight - height - 100; // Ground level for level 1
        }

        // Initialize Y position to ground level
        y = groundY;

        if (spriteSheet == null) {
            spriteSheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.jello_sprite_sheet);
        }
        frameWidth = spriteSheet.getWidth() / columns;
        frameHeight = spriteSheet.getHeight() / rows;
    }

    public void update(float deltaTime) {
        if (isJumping) {
            velocityY += gravity * deltaTime;
            y += velocityY * deltaTime;
            scaleY = 1.2f;
            scaleX = 0.8f;

            // Check landing more precisely - we need to detect if we've passed through groundY
            if (velocityY > 0 && y + feetOffset >= groundY) {
                y = groundY; // Ensure exact landing at ground level
                isJumping = false;
                velocityY = 0;
                Log.d("PLAYER", "Landed precisely at groundY=" + groundY);
            }
        } else if (isSliding) {
            if (slideTimer == 0f) return;
            slideTimer -= deltaTime;
            if (slideTimer <= 0) {
                isSliding = false;
                Log.d("PLAYER", "Slide ended");
            }

            scaleX = 1.6f;
            scaleY = 0.2f;
        } else if (isFalling) {
            // Increase falling speed with more dramatic acceleration
            float fastFallFactor = 1.5f; // Reduced from 20f for more natural falling
            velocityY += gravity * deltaTime * fastFallFactor;
            y += velocityY * deltaTime;

            // More dramatic visual falling effect
            scaleY = Math.min(1.3f, 1.0f + (velocityY / 3000f)); // Stretch more as speed increases
            scaleX = Math.max(0.7f, 1.0f - (velocityY / 6000f)); // Compress width slightly

            // Log falling position for debugging
            if (velocityY > 0 && velocityY % 500 < 10) {
                Log.d("PLAYER", "Falling: y=" + y + ", velocityY=" + velocityY);
            }
        } else {
            // Smoothly reset scale to normal
            scaleX += (1f - scaleX) * 6 * deltaTime;
            scaleY += (1f - scaleY) * 6 * deltaTime;

            // Important: Ensure player is at groundY when not jumping/sliding/falling
            if (!isSliding && Math.abs(y - groundY) > 1f) {
                y = groundY;
                Log.d("PLAYER", "Snapped to groundY from y=" + y);
            }
        }

        // Ensure proper position if not sliding
        if (!isSliding && !isFalling && height != normalHeight) {
            height = normalHeight;
            y = groundY;
        }

        // Update animation
        frameTicker += deltaTime;
        if (frameTicker >= framePeriod) {
            currentFrame = (currentFrame + 1) % frameCount;
            frameTicker = 0f;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        int srcX = (currentFrame % columns) * frameWidth;
        int srcY = (currentFrame / columns) * frameHeight;
        src.set(srcX, srcY, srcX + frameWidth, srcY + frameHeight);

        float scaledWidth = width * scaleX;
        float scaledHeight = height * scaleY;

        float centerX = x + width / 2;
        float left = centerX - scaledWidth / 2;
        float right = centerX + scaledWidth / 2;

        // ✅ Adjust scaling anchor to bottom
        float bottom = y + height;
        float top = bottom - scaledHeight;

        dst.set((int) left, (int) top, (int) right, (int) bottom);
        canvas.drawBitmap(spriteSheet, src, dst, paint);
    }

    public void jump(float swipeDistance, float swipeSpeed) {
        Log.d("PLAYER", "jump() called | isJumping=" + isJumping + " isSliding=" + isSliding);
        if (!isJumping && !isSliding) {
            isJumping = true;
            swipeSpeed = Math.min(swipeSpeed, 3.0f);
            float jumpStrength = -500f - (swipeSpeed / 4.0f) * 800f;
            if(gameLevel >= 2){
                jumpStrength = -600f - (swipeSpeed / 4.0f) * 900f;
            }
            velocityY = jumpStrength;
            Log.d("PLAYER", "Jump started with velocityY=" + velocityY);
        }
    }

    public void slide() {
        Log.d("PLAYER", "slide() called | isJumping=" + isJumping + " isSliding=" + isSliding);
        if (!isJumping && !isSliding) {
            isSliding = true;
            slideTimer = slideDuration;

            // ✅ Do NOT change height or y — visual scaleY will handle it
            Log.d("PLAYER", "Slide started");
        }
    }

    public RectF getBounds() {
        float padding = 20f;
        float centerX = x + width / 2;
        float centerY = y + height / 2;
        float scaledWidth = width * scaleX - padding;
        float scaledHeight = height - padding;

        // Adjust bounds to better represent feet position
        float bottom = y + height;
        float top = bottom - height * scaleY;
        bounds.set(
                centerX - scaledWidth / 2,
                top + padding,
                centerX + scaledWidth / 2,
                bottom - padding
        );
        return bounds;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getX() {
        return x;
    }

    public int getWidth() {
        return (int) width;
    }

    public void setSliding(boolean sliding) {
        isSliding = sliding;
        if (!sliding) {
            height = normalHeight;
            y = groundY;
        }
    }

    public void setJumping(boolean jumping) {
        isJumping = jumping;
        if (!jumping) {
            velocityY = 0;
            y = groundY;
        }
    }

    public void resetScale() {
        scaleX = 1f;
        scaleY = 1f;
    }

    public void resetHeightAndY() {
        height = normalHeight;
        y = groundY;
        Log.d("PLAYER", "Reset position to groundY=" + groundY + " y=" + y);
    }

    public boolean isJumping() {
        return isJumping;
    }

    public boolean isSliding() {
        return isSliding;
    }

    // For platform level games, we need to update the ground Y position
    public void updateGroundY(float newGroundY) {
        this.groundY = newGroundY; // Use the parameter instead of recalculating
        // Always update Y position when not in mid-jump
        if (!isJumping && !isFalling) {
            y = groundY;
            Log.d("PLAYER", "Updated groundY to " + groundY + " and set y position");
        }
    }


    public float getY() {
        return y;
    }

    public float getGroundY() {
        return groundY;
    }

    public boolean isFalling() {
        return isFalling;
    }

    public void startFalling() {
        if (!isJumping && !isFalling) {
            Log.d("PLAYER", "startFalling called - initiating falling state");
            isFalling = true;
            velocityY = 300f; // Start with a small downward velocity
        }
    }

    public void stopFalling() {
        if (isFalling) {
            isFalling = false;
            velocityY = 0;
            y = groundY;
            Log.d("PLAYER", "Stopped falling, reset to groundY=" + groundY);
        }
    }

    public void resetAfterFall() {
        // Reset player after falling
        isFalling = false;  // Make sure this is set first
        isJumping = false;
        isSliding = false;
        height = normalHeight;
        velocityY = 0;
        resetScale();
        y = groundY;  // Ensure y is set to ground level
        Log.d("PLAYER", "Reset after fall to groundY=" + groundY);
    }
    public float getVelocityY() {
        return velocityY;
    }
}