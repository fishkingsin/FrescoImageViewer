package com.stfalcon.frescoimageviewer.drawee;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

/**
 * _   _ _______   ________ _       _____   __
 * | \ | |_   _\ \ / /| ___ \ |     / _ \ \ / /
 * |  \| | | |  \ V / | |_/ / |    / /_\ \ V /
 * | . ` | | |  /   \ |  __/| |    |  _  |\ /
 * | |\  |_| |_/ /^\ \| |   | |____| | | || |
 * \_| \_/\___/\/   \/\_|   \_____/\_| |_/\_/
 *
 * Created by jameskong on 25/7/2017.
 */

public final class ArcUtils {
    private static final double FULL_CIRCLE_RADIANS = toRadians(360d);

    private ArcUtils() { }

    /**
     * Draws a circular arc on the given {@code Canvas}.
     *
     * @param canvas       The canvas to draw into.
     * @param circleCenter The center of the circle on which to draw the arc.
     * @param circleRadius The radius of the circle on which to draw the arc.
     * @param startAngle   Starting angle (in degrees) where the arc begins.
     * @param sweepAngle   Sweep angle (in degrees) measured clockwise.
     * @param paint        The paint to use then drawing the arc.
     *
     * @see #drawArc(Canvas, PointF, float, float, float, Paint, int, boolean)
     */
    public static void drawArc(@NonNull Canvas canvas, PointF circleCenter, float circleRadius,
                               float startAngle, float sweepAngle, @NonNull Paint paint)
    {
        drawArc(canvas, circleCenter, circleRadius, startAngle, sweepAngle, paint, 8, false);
    }

    /**
     * Draws a circular arc on the given {@code Canvas}.
     *
     * @param canvas             The canvas to draw into.
     * @param circleCenter       The center of the circle on which to draw the arc.
     * @param circleRadius       The radius of the circle on which to draw the arc.
     * @param startAngle         Starting angle (in degrees) where the arc begins.
     * @param sweepAngle         Sweep angle (in degrees) measured clockwise.
     * @param paint              The paint to use then drawing the arc.
     * @param arcsPointsOnCircle See {@link #createBezierArcDegrees(PointF, float, float, float, int, boolean, Path)}.
     * @param arcsOverlayPoints  See {@link #createBezierArcDegrees(PointF, float, float, float, int, boolean, Path)}.
     *
     * @see #drawArc(Canvas, PointF, float, float, float, Paint)
     */
    public static void drawArc(@NonNull Canvas canvas, PointF circleCenter, float circleRadius,
                               float startAngle, float sweepAngle, @NonNull Paint paint,
                               int arcsPointsOnCircle, boolean arcsOverlayPoints)
    {
        if (sweepAngle == 0f)
        {
            final PointF p = pointFromAngleDegrees(circleCenter, circleRadius, startAngle);
            canvas.drawPoint(p.x, p.y, paint);
        }
        else
        {
            canvas.drawPath(createBezierArcDegrees(
                    circleCenter, circleRadius, startAngle, sweepAngle,
                    arcsPointsOnCircle, arcsOverlayPoints, null), paint);
        }
    }


    public static double normalizeRadians(double radians)
    {
        radians %= FULL_CIRCLE_RADIANS;
        if (radians < 0d) { radians += FULL_CIRCLE_RADIANS; }
        if (radians == FULL_CIRCLE_RADIANS) { radians = 0d; }
        return radians;
    }


    /**
     * Returns the point of a given angle (in radians) on a circle.
     *
     * @param center       The center of the circle.
     * @param radius       The radius of the circle.
     * @param angleRadians The angle (in radians).
     *
     * @return The point of the given angle on the specified circle.
     *
     * @see #pointFromAngleDegrees(PointF, float, float)
     */
    @NonNull
    public static PointF pointFromAngleRadians(@NonNull PointF center, float radius, double angleRadians)
    {
        return new PointF((float)(center.x + radius * cos(angleRadians)),
                (float)(center.y + radius * sin(angleRadians)));
    }

    /**
     * Returns the point of a given angle (in degrees) on a circle.
     *
     * @param center       The center of the circle.
     * @param radius       The radius of the circle.
     * @param angleDegrees The angle (in degrees).
     *
     * @return The point of the given angle on the specified circle.
     *
     * @see #pointFromAngleRadians(PointF, float, double)
     */
    @NonNull
    public static PointF pointFromAngleDegrees(@NonNull PointF center, float radius, float angleDegrees)
    {
        return pointFromAngleRadians(center, radius, toRadians(angleDegrees));
    }


    public static void addBezierArcToPath(@NonNull Path path, @NonNull PointF center,
                                          @NonNull PointF start, @NonNull PointF end, boolean moveToStart)
    {
        if (moveToStart) { path.moveTo(start.x, start.y); }
        if (start.equals(end)) { return; }

        final double ax = start.x - center.x;
        final double ay = start.y - center.y;
        final double bx = end.x - center.x;
        final double by = end.y - center.y;
        final double q1 = ax * ax + ay * ay;
        final double q2 = q1 + ax * bx + ay * by;
        final double k2 = 4d / 3d * (sqrt(2d * q1 * q2) - q2) / (ax * by - ay * bx);
        final float x2 = (float)(center.x + ax - k2 * ay);
        final float y2 = (float)(center.y + ay + k2 * ax);
        final float x3 = (float)(center.x + bx + k2 * by);
        final float y3 = (float)(center.y + by - k2 * bx);

        path.cubicTo(x2, y2, x3, y3, end.x, end.y);
    }


    @NonNull
    public static Path createBezierArcRadians(@NonNull PointF center, float radius, double startAngleRadians,
                                              double sweepAngleRadians, int pointsOnCircle, boolean overlapPoints,
                                              @Nullable Path addToPath)
    {
        final Path path = addToPath != null ? addToPath : new Path();
        if (sweepAngleRadians == 0d) { return path; }

        if (pointsOnCircle >= 1)
        {
            final double threshold = FULL_CIRCLE_RADIANS / pointsOnCircle;
            if (abs(sweepAngleRadians) > threshold)
            {
                double angle = normalizeRadians(startAngleRadians);
                PointF end, start = pointFromAngleRadians(center, radius, angle);
                path.moveTo(start.x, start.y);
                if (overlapPoints)
                {
                    final boolean cw = sweepAngleRadians > 0; // clockwise?
                    final double angleEnd = angle + sweepAngleRadians;
                    while (true)
                    {
                        double next = (cw ? ceil(angle / threshold) : floor(angle / threshold)) * threshold;
                        if (angle == next) { next += threshold * (cw ? 1d : -1d); }
                        final boolean isEnd = cw ? angleEnd <= next : angleEnd >= next;
                        end = pointFromAngleRadians(center, radius, isEnd ? angleEnd : next);
                        addBezierArcToPath(path, center, start, end, false);
                        if (isEnd) { break; }
                        angle = next;
                        start = end;
                    }
                }
                else
                {
                    final int n = abs((int)ceil(sweepAngleRadians / threshold));
                    final double sweep = sweepAngleRadians / n;
                    for (int i = 0;
                         i < n;
                         i++, start = end)
                    {
                        angle += sweep;
                        end = pointFromAngleRadians(center, radius, angle);
                        addBezierArcToPath(path, center, start, end, false);
                    }
                }
                return path;
            }
        }

        final PointF start = pointFromAngleRadians(center, radius, startAngleRadians);
        final PointF end = pointFromAngleRadians(center, radius, startAngleRadians + sweepAngleRadians);
        addBezierArcToPath(path, center, start, end, true);
        return path;
    }


    @NonNull
    public static Path createBezierArcDegrees(@NonNull PointF center, float radius, float startAngleDegrees,
                                              float sweepAngleDegrees, int pointsOnCircle, boolean overlapPoints,
                                              @Nullable Path addToPath)
    {
        return createBezierArcRadians(center, radius, toRadians(startAngleDegrees), toRadians(sweepAngleDegrees),
                pointsOnCircle, overlapPoints, addToPath);
    }
}