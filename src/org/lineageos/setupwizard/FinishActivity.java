/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-FileCopyrightText: 2023-2024 The EtherealOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static android.os.Binder.getCallingUserHandle;
import static android.os.UserHandle.USER_CURRENT;

import static org.lineageos.setupwizard.Manifest.permission.FINISH_SETUP;
import static org.lineageos.setupwizard.SetupWizardApp.LOGV;
import static org.lineageos.setupwizard.SetupWizardApp.NAVIGATION_OPTION_KEY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

import org.lineageos.setupwizard.util.SetupWizardUtils;

public class FinishActivity extends BaseSetupWizardActivity {

    public static final String TAG = FinishActivity.class.getSimpleName();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private static volatile boolean sIsFinishing;

    private View mRootView;
    private Resources.Theme mEdgeToEdgeWallpaperBackgroundTheme;

    private LottieAnimationView lottieAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.translucent_enter,
                R.anim.translucent_exit);

        if (LOGV) {
            logActivityState("onCreate savedInstanceState=" + savedInstanceState);
        }

        setNextText(R.string.start);

        // Edge-to-edge for full background coverage
        final Window window = getWindow();
        window.setDecorFitsSystemWindows(false);
        window.setNavigationBarContrastEnforced(false);

        mRootView = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(mRootView, (view, windowInsets) -> {
            final View linearLayout = findViewById(R.id.linear_layout);
            final Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            final MarginLayoutParams params = (MarginLayoutParams) linearLayout.getLayoutParams();
            params.leftMargin = insets.left;
            params.topMargin = insets.top;
            params.rightMargin = insets.right;
            params.bottomMargin = insets.bottom;
            linearLayout.setLayoutParams(params);
            return WindowInsetsCompat.CONSUMED;
        });

        // Lottie background animation setup
        lottieAnimationView = findViewById(R.id.lottieBackground);
        lottieAnimationView.setRepeatCount(LottieDrawable.INFINITE);
        lottieAnimationView.playAnimation();
        
        // Set up brand animation
        LottieAnimationView animationView = findViewById(R.id.brand_logo_end);
	animationView.setAnimation(R.raw.logo);
	animationView.playAnimation();

        if (sIsFinishing) {
            startFinishSequence();
        }
    }

    private void disableActivityTransitions() {
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
    }

    @Override
    protected void applyForwardTransition() {
        if (!sIsFinishing) {
            super.applyForwardTransition();
        }
    }

    @Override
    protected void applyBackwardTransition() {
        if (!sIsFinishing) {
            super.applyBackwardTransition();
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.finish_activity;
    }

    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = super.getTheme();
        if (sIsFinishing) {
            if (mEdgeToEdgeWallpaperBackgroundTheme == null) {
                theme.applyStyle(R.style.EdgeToEdgeWallpaperBackground, true);
                mEdgeToEdgeWallpaperBackgroundTheme = theme;
            }
            return mEdgeToEdgeWallpaperBackgroundTheme;
        }
        return theme;
    }

    @Override
    public void onNavigateNext() {
        if (!sIsFinishing) {
            sIsFinishing = true;
            startActivity(getIntent());
            finish();
            disableActivityTransitions();
        }
        hideNextButton();
    }

    private void startFinishSequence() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        hideNextButton();

        if (mRootView.isAttachedToWindow()) {
            mHandler.post(this::animateOut);
        } else {
            mRootView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    mHandler.post(() -> animateOut());
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    // Do nothing
                }
            });
        }
    }

    private void animateOut() {
        final int cx = (mRootView.getLeft() + mRootView.getRight()) / 2;
        final int cy = (mRootView.getTop() + mRootView.getBottom()) / 2;
        final float fullRadius = (float) Math.hypot(cx, cy);

        Animator anim = ViewAnimationUtils.createCircularReveal(mRootView, cx, cy, fullRadius, 0f);
        anim.setDuration(900);

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mRootView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mRootView.setVisibility(View.INVISIBLE);

                // ✨ Also fade out the Lottie animation
                lottieAnimationView.animate()
                        .alpha(0f)
                        .setDuration(900)
                        .withEndAction(() -> lottieAnimationView.setVisibility(View.INVISIBLE))
                        .start();

                mHandler.post(() -> {
                    if (LOGV) {
                        Log.v(TAG, "Animation ended");
                    }
                    SetupWizardUtils.finishSetupWizard(FinishActivity.this);
                });
            }
        });

        anim.start();
    }
}

