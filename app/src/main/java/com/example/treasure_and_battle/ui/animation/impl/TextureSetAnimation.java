package com.example.treasure_and_battle.ui.animation.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.treasure_and_battle.ui.animation.Animation;
import com.example.treasure_and_battle.ui.animation.model.AnimationTargetType;
import com.example.treasure_and_battle.ui.animation.model.TextureSetAnimationTemplate;

import java.io.IOException;
import java.io.InputStream;

/**
 * 贴图组动画 - 按时间顺序播放一组贴图
 */
public class TextureSetAnimation extends Animation {

    private final TextureSetAnimationTemplate textureTemplate;
    private final Bitmap[] textures;           // 预加载的贴图数组
    private final ImageView imageView;         // 显示贴图的ImageView
    private final Context context;

    public TextureSetAnimation(TextureSetAnimationTemplate template, int createFrame,
                           View targetView, ViewGroup animationContainer,
                           Context context) {
        super(template, createFrame, targetView, animationContainer);
        this.textureTemplate = template;
        this.context = context;
        this.textures = loadTextures();
        this.imageView = createImageView();
        setupInitialPosition();
    }

    @Override
    public boolean update() {
        if (!isStarted) {
            if (!shouldStart()) {
                currentFrame++;
                return false;
            }
            isStarted = true;
            // 添加到容器
            animationContainer.addView(imageView);
        }

        if (shouldEnd()) {
            isFinished = true;
            return true;
        }

        // 计算当前应该显示哪张贴图
        int progressFrame = currentFrame - textureTemplate.beginAt;
        int textureIndex = (progressFrame * textureTemplate.num) / textureTemplate.duration;
        textureIndex = Math.min(textureIndex, textureTemplate.num - 1);

        // 更新贴图
        if (textures[textureIndex] != null) {
            imageView.setImageBitmap(textures[textureIndex]);
        }

        currentFrame++;
        return false;
    }

    @Override
    public void cleanup() {
        if (imageView != null && imageView.getParent() != null) {
            animationContainer.removeView(imageView);
        }
        // 回收贴图资源
        for (Bitmap bitmap : textures) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private Bitmap[] loadTextures() {
        Bitmap[] bitmaps = new Bitmap[textureTemplate.num];
        for (int i = 0; i < textureTemplate.num; i++) {
            String path = textureTemplate.textureSet[i];
            try (InputStream is = context.getAssets().open(path)) {
                bitmaps[i] = BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                android.util.Log.w("TextureSetAnimation", "Failed to load texture: " + path);
                bitmaps[i] = null;
            }
        }
        return bitmaps;
    }

    private ImageView createImageView() {
        ImageView iv = new ImageView(context);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // 计算尺寸和位置
        ViewGroup.LayoutParams params = calculateLayoutParams();
        iv.setLayoutParams(params);

        return iv;
    }

    private ViewGroup.LayoutParams calculateLayoutParams() {
        // 基于目标实体View的尺寸计算动画大小
        int targetWidth = targetView.getWidth();
        int targetHeight = targetView.getHeight();

        // 应用scale参数：scale=1.0 表示和实体一样大
        float scale = textureTemplate.scale;
        int width = (int) (targetWidth * scale);
        int height = (int) (targetHeight * scale);

        // 保持贴图原始比例
        if (textures[0] != null) {
            float bitmapRatio = (float) textures[0].getWidth() / textures[0].getHeight();
            float targetRatio = (float) width / height;

            if (bitmapRatio > targetRatio) {
                // 以宽度为准
                height = (int) (width / bitmapRatio);
            } else {
                // 以高度为准
                width = (int) (height * bitmapRatio);
            }
        }

        android.util.Log.d("TextureSetAnimation", "动画尺寸: " + width + "x" + height +
                          " (实体: " + targetWidth + "x" + targetHeight + ", scale: " + scale + ")");

        return new ViewGroup.LayoutParams(width, height);
    }

    private void setupInitialPosition() {
        if (targetView != null) {
            // 基于targetView定位
            positionRelativeToTarget();
        } else {
            // 全局场景定位
            positionGlobally();
        }
    }

    private void positionRelativeToTarget() {
        // 创建FrameLayout.LayoutParams以支持gravity设置
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        switch (textureTemplate.position.toLowerCase()) {
            case "center":
                // 在目标容器中心
                params.gravity = Gravity.CENTER;
                break;
            case "top":
                // 在目标容器顶部，水平居中
                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                break;
            case "bottom":
                // 在目标容器底部，水平居中
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                break;
            default:
                // 默认居中
                params.gravity = Gravity.CENTER;
                break;
        }

        imageView.setLayoutParams(params);
    }

    private void positionGlobally() {
        // 全局场景定位 - 默认居中
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        imageView.setLayoutParams(params);
    }
}