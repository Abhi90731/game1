package com.example.mygame1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private Thread thread;
    private boolean isPlaying = true;
    private SurfaceHolder holder;
    private Paint paint;
    private Player player;
    private Background background;
    private List<Hurdle> hurdles;
    private List<Platform> platforms;
    private PlatformManager platformManager;
    private int screenWidth, screenHeight;
    private Activity context;
    private float startX, startY;

    private float hurdleTimer = 0;
    private float currentHurdleInterval;

    private long swipeStartTime;
    private long swipeEndTime;
    private int lives = 3;
    private ChasedCharacter chasedCharacter;
    private float playerSpeed = 13f;
    private float speedIncrement = 0.00005f;
    private float maxSpeed = 700f;
    private MediaPlayer bgMusic, lifeLostSound, jumpSound, slideSound, gameOverSound, restartSound, winSound;
    private float baseTempo = 1.0f;
    private float maxTempo = 2.0f;
    private float maxDistance;
    private float difficultyProgress = 0f;
    private boolean isBlinking = false;
    private float blinkDuration = 0.2f;
    private float blinkTimer = 0f;
    private boolean initialGracePeriod = true;
    private float gracePeriodTimer = 2.0f;
    private float safeZoneWidth = 200f;
    private boolean isPlayerOnGround = true; //track player grounded state

    private enum GameState {
        START_MENU,
        PLAYING,
        LEVEL_COMPLETE
    }

    private GameState gameState = GameState.START_MENU;

    private int currentLevel = 1;  // Default to level 1
    private static final String TAG = "GameView"; // Add a TAG for logging

    public GameView(Activity context, int level) {
        super(context);
        this.context = context;
        this.currentLevel = level; // Use the level passed from MainActivity
        holder = getHolder();
        paint = new Paint();
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        lives = 3;

        background = new Background(context, screenWidth, screenHeight, currentLevel);
        player = new Player(context, screenWidth, screenHeight, currentLevel);
        hurdles = new ArrayList<>();
        platforms = new ArrayList<>();

        if (this.currentLevel >= 2) { // Use this.currentLevel
            playerSpeed = 20f;
            speedIncrement = 0.0001f;
            try {
                platformManager = new PlatformManager(context, screenWidth, screenHeight);
                platformManager.setPlatformSpeedFactor(11.0f); // Make platforms move 5x faster
                platforms = platformManager.getVisiblePlatforms();

                player.setX(100f);
                player.resetHeightAndY();

                float ghostX = platformManager.getFinalPlatformX() + 50;
                chasedCharacter = new ChasedCharacter(context, screenWidth, screenHeight, currentLevel);
                chasedCharacter.setX(ghostX);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing platformManager", e);
                platformManager = null;
                player.setX(0f);
                chasedCharacter = new ChasedCharacter(context, screenWidth, screenHeight, currentLevel);
                chasedCharacter.setX(screenWidth * 2);
            }
        } else {
            // Level 1-specific initialization
            platformManager = null;
            player.setX(0);
            // Explicitly ensure player is at ground level for level 1
            float groundLevel = screenHeight - 100 - player.getBounds().height();
            player.updateGroundY(groundLevel);
            player.resetHeightAndY();

            chasedCharacter = new ChasedCharacter(context, screenWidth, screenHeight, currentLevel);
            chasedCharacter.setX(screenWidth / 2f);
        }

        maxDistance = screenWidth * 3;

        bgMusic = MediaPlayer.create(context, currentLevel == 2 ? R.raw.background_music_2 : R.raw.game_background);
        bgMusic.setLooping(true);
        bgMusic.setVolume(0.6f, 0.6f);
        bgMusic.start();

        lifeLostSound = MediaPlayer.create(context, R.raw.life_lost);
        jumpSound = MediaPlayer.create(context, R.raw.jump);
        slideSound = MediaPlayer.create(context, R.raw.jump);
        gameOverSound = MediaPlayer.create(context, R.raw.game_over);
        restartSound = MediaPlayer.create(context, R.raw.game_start);
        winSound = MediaPlayer.create(context, R.raw.win);

        holder.addCallback(this);
    }

    @Override
    public void run() {
        long previousTime = System.nanoTime();
        final double maxDeltaTime = 0.033;
        final int targetFPS = 60;
        final long targetTimePerFrame = 1_000_000_000L / targetFPS;

        while (isPlaying) {
            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - previousTime) / 1_000_000_000f;
            previousTime = currentTime;

            if (deltaTime > maxDeltaTime) deltaTime = (float) maxDeltaTime;
            if (!holder.getSurface().isValid()) continue;

            update(deltaTime);
            draw();

            long frameTime = System.nanoTime() - currentTime;
            long sleepTime = (targetTimePerFrame - frameTime) / 1_000_000;
            if (sleepTime > 0) SystemClock.sleep(sleepTime);
        }
    }

    private void update(float deltaTime) {
        if (initialGracePeriod) {
            gracePeriodTimer -= deltaTime;
            if (gracePeriodTimer <= 0) {
                initialGracePeriod = false;
            }
        }
        if (gameState != GameState.PLAYING) return;

        background.update(deltaTime);
        player.update(deltaTime);
        chasedCharacter.update(deltaTime);

        if (currentLevel >= 2 && platformManager != null) {
            platformManager.update(deltaTime, playerSpeed);
            platforms = platformManager.getVisiblePlatforms();
        } else {
            platforms = new ArrayList<>();
        }

        float distance = Math.max(0f, chasedCharacter.getX() - player.getX());
        float progress = 1f - (distance / maxDistance);
        progress = Math.max(0f, Math.min(1f, progress));

        if (difficultyProgress < progress) {
            difficultyProgress += 0.05f * deltaTime;
            if (difficultyProgress > progress) difficultyProgress = progress;
        }

        float hurdleIntervalMin = 2.5f - 1.8f * difficultyProgress;
        float hurdleIntervalMax = 4.0f - 2.5f * difficultyProgress;
        hurdleIntervalMin = Math.max(0.3f, hurdleIntervalMin);
        hurdleIntervalMax = Math.max(hurdleIntervalMin + 0.2f, hurdleIntervalMax);
        currentHurdleInterval = getRandomFloat(hurdleIntervalMin, hurdleIntervalMax);

        hurdleTimer += deltaTime;
        if (hurdleTimer >= currentHurdleInterval) {
            hurdleTimer = 0f;

            if (currentLevel >= 2) {
                // Platform-based hurdles for level 2+
                Platform targetPlatform = findSuitablePlatformForHurdle();
                if (targetPlatform != null) {
                    Random random = new Random();
                    boolean isLow = true;
                    if (difficultyProgress >= 0.2f) isLow = random.nextBoolean();
                    hurdles.add(new Hurdle(context, targetPlatform, isLow));
                }
            } else {
                // Ground-based hurdles for level 1
                Random random = new Random();
                boolean isLow = true;
                if (difficultyProgress >= 0.2f) isLow = random.nextBoolean();
                // Use the new constructor for level 1 hurdles
                hurdles.add(new Hurdle(context, screenWidth, screenHeight, isLow));
            }
        }

        Iterator<Hurdle> iterator = hurdles.iterator();
        while (iterator.hasNext()) {
            Hurdle hurdle = iterator.next();
            // Add this speed adjustment for level 1
            float hurdleSpeed = playerSpeed;
            if (currentLevel == 1) {
                hurdleSpeed = playerSpeed * 20.0f; // Increase hurdle speed for level 1
            }
            hurdle.update(deltaTime, hurdleSpeed);

            if (hurdle.getX() + hurdle.getWidth() < 0) {
                iterator.remove();
            }
            if (hurdle.collidesWith(player)) {
                iterator.remove();
                lives--;
                isBlinking = true;
                blinkTimer = blinkDuration;
                if (lifeLostSound != null) lifeLostSound.start();
                if (bgMusic != null) bgMusic.pause();
                if (restartSound != null) restartSound.start();
                if (bgMusic != null) {
                    bgMusic.seekTo(0);
                    bgMusic.start();
                }
                difficultyProgress = 0f;

                if (lives <= 0) {
                    if (bgMusic != null) bgMusic.pause();
                    if (gameOverSound != null) gameOverSound.start();
                    isPlaying = false;
                    context.runOnUiThread(() -> new AlertDialog.Builder(context)
                            .setTitle("Game Over")
                            .setMessage("You lost all lives! Restart or exit?")
                            .setCancelable(false)
                            .setPositiveButton("Restart", (dialog, which) -> resetGame())
                            .setNegativeButton("Exit", (dialog, which) -> context.finish())
                            .show());
                    return;
                } else {
                    resetAfterFall();
                    return;
                }
            }
        }

        // === Fall Detection Logic ===
        checkPlatformCollisions();
        checkPlayerState();

        playerSpeed += speedIncrement * deltaTime;
        if (playerSpeed > maxSpeed) playerSpeed = maxSpeed;
        player.setX(player.getX() + playerSpeed * deltaTime);

        float newTempo = baseTempo + (maxTempo - baseTempo) * difficultyProgress;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && bgMusic != null && bgMusic.isPlaying()) {
            PlaybackParams params = bgMusic.getPlaybackParams();
            params.setSpeed(newTempo);
            bgMusic.setPlaybackParams(params);
        }

        if (player.getX() + player.getWidth() >= chasedCharacter.getX()) {
            if (bgMusic != null) bgMusic.pause();
            if (winSound != null) winSound.start();
            isPlaying = false;
            gameState = GameState.LEVEL_COMPLETE;
            context.runOnUiThread(() -> {
                new AlertDialog.Builder(context)
                        .setTitle("Level Complete!")
                        .setMessage("Ready for the next level?")
                        .setCancelable(false)
                        .setPositiveButton("Next", (dialog, which) -> {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra("level", currentLevel + 1);
                            context.startActivity(intent);
                            context.finish();
                        })
                        .setNegativeButton("Exit", (dialog, which) -> context.finish())
                        .show();
            });
        }

        if (isBlinking) {
            blinkTimer -= deltaTime;
            if (blinkTimer <= 0) {
                isBlinking = false;
                // Ensure player is not considered falling anymore after blinking ends
                if (player.isFalling()) {
                    player.stopFalling();
                }
            }
        }
    }

    private Platform findSuitablePlatformForHurdle() {
        if (platforms == null || platforms.isEmpty()) return null;
        List<Platform> candidates = new ArrayList<>();

        for (Platform p : platforms) {
            // Only consider platforms that are visible and ahead of the player
            if (p.getX() > player.getX() + 100 &&      // Ensure platform is ahead of player
                    p.getX() < screenWidth * 0.9f &&       // Ensure platform is not too far right
                    p.getWidth() >= 150) {                 // Ensure platform is wide enough

                // Check if this platform already has a hurdle
                boolean hasHurdle = false;
                for (Hurdle h : hurdles) {
                    if (h.getParentPlatform() == p) {
                        hasHurdle = true;
                        break;
                    }
                }

                // Check if any nearby platforms have hurdles (to avoid hurdles close together)
                if (!hasHurdle) {
                    boolean nearbyHurdle = false;
                    float minDistance = 300f; // Minimum distance between hurdles

                    for (Platform otherP : platforms) {
                        // Skip if it's the same platform
                        if (otherP == p) continue;

                        // Check if the other platform has a hurdle and is close
                        if (Math.abs(otherP.getX() - p.getX()) < minDistance) {
                            for (Hurdle h : hurdles) {
                                if (h.getParentPlatform() == otherP) {
                                    nearbyHurdle = true;
                                    break;
                                }
                            }
                        }

                        if (nearbyHurdle) break;
                    }

                    // Add to candidates if no hurdles nearby
                    if (!nearbyHurdle) {
                        candidates.add(p);
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null;
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private void draw() {
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            background.draw(canvas, paint);
            if (gameState == GameState.START_MENU) {
                drawStartMenu(canvas);
            } else if (gameState == GameState.LEVEL_COMPLETE) {
                drawLevelComplete(canvas);
            } else {
                if (currentLevel >= 2 && platforms != null) {
                    for (Platform platform : platforms) {
                        if (platform != null) {
                            platform.draw(canvas);
                        }
                    }
                }
                chasedCharacter.draw(canvas, paint);
                player.draw(canvas, paint);
                for (Hurdle hurdle : hurdles) {
                    hurdle.draw(canvas, paint);
                }
                drawHearts(canvas, paint);
                if (isBlinking) {
                    paint.setColor(Color.argb(150, 255, 0, 0));
                    canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
                }
            }
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawLevelComplete(Canvas canvas) {
        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setTextSize(80f);
        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);
        canvas.drawText("Level 2 Complete!", screenWidth / 2f - 300, screenHeight / 2f, paint);
    }

    private void drawStartMenu(Canvas canvas) {
        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setTextSize(80f);
        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);
        float centerX = screenWidth / 2f;
        float baseY = screenHeight / 2f - 100;
        canvas.drawText("Play Game", centerX - 200, baseY, paint);
        canvas.drawText("Credits", centerX - 200, baseY + 120, paint);
        canvas.drawText("Exit", centerX - 200, baseY + 240, paint);
    }

    private void drawHearts(Canvas canvas, Paint paint) {
        paint.setColor(Color.RED);
        paint.setTextSize(60f);
        paint.setFakeBoldText(true);
        float heartSpacing = 80f;
        float totalWidth = lives * heartSpacing;
        float startX = screenWidth - totalWidth - 40;
        for (int i = 0; i < lives; i++) {
            canvas.drawText("❤️", startX + i * heartSpacing, 200f, paint);
        }
    }

    private float getRandomFloat(float min, float max) {
        return min + new Random().nextFloat() * (max - min);
    }

    private void resetAfterFall() {
        if (player.getY() <= player.getGroundY() + 5) {
            Log.d("GameView", "Prevented unnecessary reset after valid landing");
            return;
        }
        // First reset player positions
        player.setX(0f);
        player.setJumping(false);
        player.setSliding(false);
        player.stopFalling();  // Explicitly stop the falling state

        // For level 2, ensure we have the right ground height from platformManager
        if (currentLevel >= 2 && platformManager != null) {
            float platformY = platformManager.getPlatformY() - player.getBounds().height();
            player.updateGroundY(platformY);
        }

        // Now reset height and y position
        player.resetHeightAndY();
        player.resetScale();
        isPlayerOnGround = true;

        // Add a grace period after reset to prevent immediate falling again
        initialGracePeriod = true;
        gracePeriodTimer = 1.0f;  // 1 second grace period after reset

        Log.d("GameView", "Reset player after fall, isPlayerOnGround=" + isPlayerOnGround);
    }

    private void resetGame() {
        background = new Background(context, screenWidth, screenHeight, currentLevel);
        player = new Player(context, screenWidth, screenHeight, currentLevel);
        hurdles = new ArrayList<>();

        lives = 3;
        initialGracePeriod = true;
        gracePeriodTimer = 2.0f;
        isPlayerOnGround = true;

        if (currentLevel >= 2) {
            try {
                platformManager = new PlatformManager(context, screenWidth, screenHeight);
                platformManager.setPlatformSpeedFactor(11.0f);
                platforms = platformManager.getVisiblePlatforms();

                player.setX(100f);
                player.resetHeightAndY();

                float ghostX = platformManager.getFinalPlatformX() + 200;
                chasedCharacter = new ChasedCharacter(context, screenWidth, screenHeight, currentLevel);
                chasedCharacter.setX(ghostX);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing platforms", e);
                platformManager = null;
                platforms = new ArrayList<>();

                player.setX(0f);
                chasedCharacter = new ChasedCharacter(context, screenWidth, screenHeight, currentLevel);
                chasedCharacter.setX(screenWidth * 2);
            }
        } else {
            platformManager = null;
            platforms = new ArrayList<>();

            player.setX(0f);
            player.resetHeightAndY();
            chasedCharacter = new ChasedCharacter(context, screenWidth, screenHeight, currentLevel);
            chasedCharacter.setX(screenWidth * 2);
        }

        isPlaying = true;
        hurdleTimer = 0;
        playerSpeed = 13f;
        gameState = GameState.PLAYING;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (thread == null || !thread.isAlive()) {
            isPlaying = true;
            thread = new Thread(this);
            thread.start();
        }
        if (bgMusic != null && !bgMusic.isPlaying()) {
            bgMusic.start();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        isPlaying = false;
        if (bgMusic != null && bgMusic.isPlaying()) bgMusic.pause();
        try {
            if (thread != null) thread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread pause error", e);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (gameState == GameState.START_MENU) {
                float x = event.getX();
                float y = event.getY();
                float centerX = screenWidth / 2f;
                float baseY = screenHeight / 2f - 100;
                if (y > baseY - 60 && y < baseY + 20) {
                    gameState = GameState.PLAYING;
                    restartSound.start();
                    player.setSliding(false);
                    player.setJumping(false);
                    player.resetHeightAndY();
                    player.resetScale();
                    return true;
                } else if (y > baseY + 60 && y < baseY + 140) {
                    context.runOnUiThread(() -> new AlertDialog.Builder(context)
                            .setTitle("Credits")
                            .setMessage("Game by Abhishek @abhishekbordoloi ❤️")
                            .setPositiveButton("Back", null)
                            .show());
                } else if (y > baseY + 180 && y < baseY + 260) {
                    context.finish();
                }
            } else {
                startX = event.getX();
                startY = event.getY();
                swipeStartTime = System.currentTimeMillis();
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP && gameState == GameState.PLAYING) {
            float endX = event.getX();
            float endY = event.getY();
            swipeEndTime = System.currentTimeMillis();
            float deltaX = endX - startX;
            float deltaY = endY - startY;
            long swipeDuration = swipeEndTime - swipeStartTime;
            float swipeDistance = Math.abs(deltaY);
            float swipeSpeed = swipeDistance / Math.max(swipeDuration, 1);
            if (swipeDistance < 100 || swipeDuration > 1000) return true;
            if (Math.abs(deltaY) > Math.abs(deltaX)) {
                if (deltaY < 0) {
                    if (jumpSound != null) jumpSound.start();
                    player.jump(swipeDistance, swipeSpeed);
                } else {
                    if (slideSound != null) slideSound.start();
                    player.slide();
                }
            }
        }
        return true;
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
        if (bgMusic != null && !bgMusic.isPlaying()) {
            bgMusic.start();
        }
    }

    public void pause() {
        isPlaying = false;
        if (bgMusic != null && bgMusic.isPlaying()) {
            bgMusic.pause();
        }

        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread pause error", e);
        }
    }

    private void checkPlayerState() {
        // If player is falling and has fallen too far
        if (player.isFalling()) {
            // For level 2, check against a maximum fall distance rather than screen height
            float maxFallDistance = currentLevel >= 2 ? screenHeight * 0.8f : screenHeight * 0.95f;

            // Check if player has fallen beyond the max fall distance
            if (player.getY() > maxFallDistance) {
                // If not already blinking (to prevent multiple life losses during the same fall)
                if (!isBlinking) {
                    lives--;
                    isBlinking = true;
                    blinkTimer = blinkDuration;
                    Log.d("GameView", "Player fell too far - lost life");

                    // Play sounds
                    if (lifeLostSound != null) lifeLostSound.start();
                    if (bgMusic != null) bgMusic.pause();
                    if (restartSound != null) restartSound.start();
                    if (bgMusic != null) {
                        bgMusic.seekTo(0);
                        bgMusic.start();
                    }

                    // Reset difficulty
                    difficultyProgress = 0f;

                    // Reset player position and explicitly stop falling state
                    resetAfterFall();

                    // Check lives
                    if (lives <= 0) {
                        if (bgMusic != null) bgMusic.pause();
                        if (gameOverSound != null) gameOverSound.start();
                        isPlaying = false;
                        context.runOnUiThread(() -> new AlertDialog.Builder(context)
                                .setTitle("Game Over")
                                .setMessage("You lost all lives! Restart or exit?")
                                .setCancelable(false)
                                .setPositiveButton("Restart", (dialog, which) -> resetGame())
                                .setNegativeButton("Exit", (dialog, which) -> context.finish())
                                .show());
                    }
                }
            }
        }
    }

    private void checkPlatformCollisions() {
        boolean onPlatform = false;
        float platformY;

        // Default platform Y position based on level
        if (currentLevel >= 2 && platformManager != null) {
            platformY = platformManager.getPlatformY() - player.getBounds().height();
        } else {
            platformY = screenHeight - 100 - player.getBounds().height(); // Ground level for level 1
        }

        if (platforms != null && !platforms.isEmpty() && currentLevel >= 2) {
            // Check if player is currently on a platform
            for (Platform platform : platforms) {
                if (platform != null) {
                    // Get player bounds
                    float playerFeetY = player.getY() + player.getBounds().height();
                    float playerCenterX = player.getX() + player.getWidth() / 2;

                    // More precise platform collision: check if player's center is over platform
                    boolean playerOverPlatform = playerCenterX >= platform.getX() &&
                            playerCenterX <= platform.getX() + platform.getWidth();

                    // Check if player is at the right height to be on this platform
                    // Allow some vertical tolerance for landing
                    boolean correctHeight = Math.abs(playerFeetY - platform.getY()) < 20;

                    // If player is jumping/falling, only check platforms below
                    if (player.isJumping() || player.isFalling()) {
                        // If moving downward (falling part of jump or actual falling)
                        if (player.getVelocityY() > 0) {
                            // Check if we're about to land on this platform
                            if (playerOverPlatform && playerFeetY <= platform.getY() &&
                                    playerFeetY + player.getVelocityY() * 0.016f >= platform.getY() - 20) {
                                // Land the player
                                player.updateGroundY(platform.getY() - player.getBounds().height());
                                player.stopFalling();
                                player.setJumping(false);
                                player.resetHeightAndY();
                                onPlatform = true;
                                Log.d("GameView", "Player landed on platform at y=" + platform.getY());
                                break;
                            }
                        }
                    } else if (playerOverPlatform && correctHeight) {
                        // Player is directly on this platform
                        onPlatform = true;
                        Log.d("GameView", "Player is on platform at x=" + platform.getX());
                        break;
                    }
                }
            }

            // If player is not on a platform and not jumping
            if (!onPlatform && !player.isJumping() && !player.isFalling()) {
                boolean inSafeZone = player.getX() < safeZoneWidth;
                // Don't fall if in grace period or safe zone
                if (!initialGracePeriod && !inSafeZone) {
                    // Add a small deliberate delay before falling (feels more natural)
                    Log.d("GameView", "Player not on platform - initiating fall");
                    player.startFalling();
                }
            }
        } else {
            // Level 1 or no platforms - ensure player is on ground
            player.updateGroundY(platformY);
            isPlayerOnGround = true;
        }
    }
}
