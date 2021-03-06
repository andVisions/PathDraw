package com.martin.pdmaster;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;

import com.caverock.androidsvg.PreserveAspectRatio;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is base on PathView.class in PathView ,
 * If you want know more , The link of PathView: https://github.com/geftimov/android-pathview.
 *
 * Licence of pathview :
 *
 * Copyright 2016 Georgi Eftimov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 * 作者：MartinBZDQSM on 2016/8/28 0028.
 * 博客：http://www.jianshu.com/users/78f0e5f4a403/latest_articles
 * github：https://github.com/MartinBZDQSM
 * <p/>
 * 该类修改自 PathView的SvgUtils 链接：https://github.com/geftimov/android-pathview
 */
public class PathUtils {
    /**
     * It is for logging purposes.
     */
    private static final String LOG_TAG = "PathLayer";
    /**
     * 一张svg可能会有多条path路径
     */
    private final List<SvgPath> mPaths = new ArrayList<>();

    public final List<Path> mDrawer = new ArrayList<>();

    /**
     * The init svg.
     */
    private SVG mSvg;

    /**
     * Init the SVGUtils with a paint1 for coloring.
     */
    public PathUtils() {
    }

    /**
     * Loading the svg from the resources.
     *
     * @param context     Context object to get the resources.
     * @param svgResource int resource id of the svg.
     */
    public void load(Context context, int svgResource) {
        if (mSvg != null)
            return;
        try {
            mSvg = SVG.getFromResource(context, svgResource);
            mSvg.setDocumentPreserveAspectRatio(PreserveAspectRatio.UNSCALED);
        } catch (SVGParseException e) {
            Log.e(LOG_TAG, "Could not load specified SVG resource", e);
        }
    }

    /**
     * 渲染svg到canvas上，把path回调回来
     *
     * @param width  - the width to scale down the view to,
     * @param height - the height to scale down the view to,
     * @return All the paths from the svg.
     */
    public List<SvgPath> getPathsForViewport(final int width, final int height) {
        Canvas canvas = new Canvas() {
            private final Matrix mMatrix = new Matrix();

            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public void drawPath(Path path, Paint paint) {
                Path dst = new Path();
                getMatrix(mMatrix);
                path.transform(mMatrix, dst);
                mPaths.add(new SvgPath(dst));

                Path dst2 = new Path();
                getMatrix(mMatrix);
                path.transform(mMatrix, dst2);
                mDrawer.add(dst2);
            }
        };

        rescaleCanvas(width, height, canvas);

        return mPaths;
    }

    /**
     * Draw the svg to the canvas.
     *
     * @param canvas The canvas to be drawn.
     * @param width  The width of the canvas.
     * @param height The height of the canvas.
     */
    public void drawSvgAfter(final Canvas canvas, final int width, final int height) {
        rescaleCanvas(width, height, canvas);
    }

    /**
     * 按实际比例进行缩放
     *
     * @param width       The width of the canvas.
     * @param height      The height of the canvas.
     * @param canvas      The canvas to be drawn.
     */
    private void rescaleCanvas(int width, int height, Canvas canvas) {
        if (mSvg == null)
            return;
        final RectF viewBox = mSvg.getDocumentViewBox();

        final float scale = Math.min(width
                        / (viewBox.width()),
                height / (viewBox.height()));

        canvas.translate((width - viewBox.width() * scale) / 2.0f,
                (height - viewBox.height() * scale) / 2.0f);
        canvas.scale(scale, scale);

        mSvg.renderToCanvas(canvas);
    }


    /**
     * Path with bounds for scalling , length and mypaint.
     */
    public static class SvgPath {

        /**
         * Region of the path.
         */
        private static final Region REGION = new Region();
        /**
         * This is done for clipping the bounds of the path.
         */
        private static final Region MAX_CLIP =
                new Region(Integer.MIN_VALUE, Integer.MIN_VALUE,
                        Integer.MAX_VALUE, Integer.MAX_VALUE);
        /**
         * The path itself.
         */
        final Path path;

        /**
         * The length of the path.
         */
        float length;
        /**
         * Listener to notify that an animation step has happened.
         */
        AnimationStepListener animationStepListener;
        /**
         * The bounds of the path.
         */
        final Rect bounds;
        /**
         * The measure of the path, we can use it later to get segment of it.
         */
        final PathMeasure measure;

        boolean isMeasure = false;

        float[] point = new float[2];

        /**
         * Constructor to add the path and the mypaint.
         *
         * @param path The path that comes from the rendered svg.
         */
        SvgPath(Path path) {
            this.path = path;
            measure = new PathMeasure(path, false);
            this.length = measure.getLength();
            REGION.setPath(path, MAX_CLIP);
            bounds = REGION.getBounds();
        }

        /**
         * Sets the animation step listener.
         *
         * @param animationStepListener AnimationStepListener.
         */
        public void setAnimationStepListener(AnimationStepListener animationStepListener) {
            this.animationStepListener = animationStepListener;
        }

        /**
         * Sets the length of the path.
         *
         * @param length The length to be set.
         */
        public void setLength(float length) {
            path.reset();
            measure.getSegment(0.0f, length, path, true);
            measure.getPosTan(length, point, null);
            path.rLineTo(0.0f, 0.0f);
            if (animationStepListener != null) {
                animationStepListener.onAnimationStep();
            }
        }

        /**
         * @return The length of the path.
         */
        public float getLength() {
            return length;
        }
    }

    public interface AnimationStepListener {

        /**
         * Called when an animation step happens.
         */
        void onAnimationStep();
    }

    public List<SvgPath> getPaths() {
        return mPaths;
    }
}
