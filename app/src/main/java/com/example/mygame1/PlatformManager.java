// In PlatformManager.java, ensure the platformY value is consistently calculated:

package com.example.mygame1;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class PlatformManager {
    private List<Platform> platforms;
    private Context context;
    private float screenWidth, screenHeight;
    private float platformY;
    private Random random;
    private boolean startedRandomizing = false;
    private float initialSafeLength = 600f;
    private float finalSafeLength = 200f;
    private float platformSpeedFactor = 11.0f; // Added speed factor
    private final float PLATFORM_HEIGHT = 40f;  // Consistent platform height

    public PlatformManager(Context context, float screenWidth, float screenHeight) {
        this.context = context;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        // Make sure this is exactly the same calculation used elsewhere
        this.platformY = screenHeight * 0.5f;
        this.platforms = new ArrayList<>();
        this.random = new Random();

        try {
            platforms.add(new Platform(context, 0, platformY, initialSafeLength, PLATFORM_HEIGHT));

            float currentX = initialSafeLength;
            Log.e("Platform manager","initial platform length:"+initialSafeLength);

            float firstGap = randomRange(150, 152);
            platforms.add(new Platform(context, currentX + firstGap, platformY, 250, PLATFORM_HEIGHT));
            currentX += 200 + firstGap;

            for (int i = 0; i < 19; i++) {
                float width = randomRange(400, 500);
                float gap = randomRange(150, 152);

                platforms.add(new Platform(context, currentX + gap, platformY, width, PLATFORM_HEIGHT));
                currentX += width + gap;
            }

            platforms.add(new Platform(context, currentX + 100, platformY, finalSafeLength, PLATFORM_HEIGHT));

        } catch (Exception e) {
            Log.e("PlatformManager", "Error creating initial platforms", e);
            platforms.add(new Platform(context, 0, platformY, initialSafeLength, PLATFORM_HEIGHT));
        }
    }

    public void update(float deltaTime, float speed) {
        Iterator<Platform> iter = platforms.iterator();
        while (iter.hasNext()) {
            Platform p = iter.next();
            p.update(deltaTime, speed * platformSpeedFactor); // Apply speed factor here
            if (p.isOutOfScreen()) {
                iter.remove();
            }
        }

        float lastX = platforms.isEmpty() ? 0 : platforms.get(platforms.size() - 1).getX() + platforms.get(platforms.size() - 1).getWidth();

        while (lastX < screenWidth * 1.5) {
            float width = randomRange(400, 500);
            float gap = randomRange(150, 152);

            // Ensure all new platforms are created at exactly the same height
            Platform p = new Platform(context, lastX + gap, platformY, width, PLATFORM_HEIGHT);
            platforms.add(p);
            lastX = p.getX() + p.getWidth();
        }
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public List<Platform> getVisiblePlatforms() {
        return platforms;
    }

    public float getFinalPlatformX() {
        if (platforms.isEmpty()) return screenWidth * 2;
        Platform lastPlatform = platforms.get(platforms.size() - 1);
        return lastPlatform.getX();
    }

    // New method to set the platform speed factor
    public void setPlatformSpeedFactor(float factor) {
        this.platformSpeedFactor = factor;
    }

    // Add a getter for platformY to ensure consistency
    public float getPlatformY() {
        return platformY;
    }
}